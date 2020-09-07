/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import net.researchgate.release.tasks.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskState

import java.util.regex.Matcher

class ReleasePlugin extends PluginHelper implements Plugin<Project> {

    static final String RELEASE_GROUP = 'Release'

    private BaseScmAdapter scmAdapter

    void apply(Project project) {
        this.project = project
        extension = project.extensions.create('release', ReleaseExtension, project, attributes)

        String preCommitText = findProperty('release.preCommitText', null, 'preCommitText')
        if (preCommitText) {
            extension.preCommitText = preCommitText
        }

        // name tasks with an absolute path so subprojects can be released independently
        String rootPath = getPath(project)

        String p = project.path
        p = !p.endsWith(Project.PATH_SEPARATOR) ? p + Project.PATH_SEPARATOR : p

        project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP, type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newBuild()
            startParameter.projectProperties.putAll(project.getGradle().startParameter.projectProperties)
            startParameter.projectProperties.put('release.releasing', "true")
            startParameter.projectDir = project.projectDir
            startParameter.settingsFile = project.getGradle().startParameter.settingsFile
            startParameter.gradleUserHomeDir = project.getGradle().startParameter.gradleUserHomeDir
            buildName = project.name

            /**
             *  We use a separate 'runBuildTasks' GradleBuild process since we only have access to the extension
             *  properties after the project has been evaluated to decide which tasks to also include in the build.
             */
            tasks = [
                    "${rootPath}createScmAdapter" as String,
                    "${rootPath}initScmAdapter" as String,
                    "${rootPath}checkReleaseNeeded" as String,
                    "${rootPath}checkCommitNeeded" as String,
                    "${rootPath}checkUpdateNeeded" as String,
                    "${rootPath}prepareVersions" as String,
                    "${rootPath}unSnapshotVersion" as String,
                    "${rootPath}confirmReleaseVersion" as String,
                    "${rootPath}checkSnapshotDependencies" as String,
                    "${rootPath}runBuildTasks" as String,
                    "${rootPath}preTagCommit" as String,
                    "${rootPath}createReleaseTag" as String,
                    "${rootPath}updateVersion" as String,
                    "${rootPath}commitNewVersion" as String
            ]
        }

        // The SCM adapter is created in the plugin context since its needed to revert changes on task failure

        project.tasks.create('initScmAdapter', InitScmAdapter)
        project.task('createScmAdapter', group: RELEASE_GROUP,
                description: 'Finds the correct SCM plugin') doLast this.&createScmAdapter
        project.tasks.create('checkReleaseNeeded', CheckReleaseNeeded)
        project.tasks.create('checkCommitNeeded', CheckCommitNeeded)
        project.tasks.create('checkUpdateNeeded', CheckUpdateNeeded)
        project.tasks.create('prepareVersions', PrepareVersions)
        project.tasks.create('unSnapshotVersion', UnSnapshotVersion)
        project.tasks.create('confirmReleaseVersion', ConfirmReleaseVersion)
        project.tasks.create('checkSnapshotDependencies', CheckSnapshotDependencies)
        project.task('beforeReleaseBuild', group: RELEASE_GROUP,
                description: 'Runs immediately before the build when doing a release') {}
        project.task('runBuildTasks', group: RELEASE_GROUP,
                description: 'Runs the build process in a separate gradle run.', type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()
            startParameter.projectProperties.putAll(project.getGradle().startParameter.projectProperties)
            startParameter.projectProperties.put('release.releasing', "true")
            startParameter.projectDir = project.projectDir
            startParameter.settingsFile = project.getGradle().startParameter.settingsFile
            startParameter.gradleUserHomeDir = project.getGradle().startParameter.gradleUserHomeDir
            buildName = project.name

            project.afterEvaluate {
                tasks = [
                        "${p}beforeReleaseBuild" as String,
                        extension.buildTasks.collect { it },
                        "${p}afterReleaseBuild" as String
                ].flatten()
            }
        }
        project.task('afterReleaseBuild', group: RELEASE_GROUP,
                description: 'Runs immediately after the build when doing a release') {}
        project.tasks.create('preTagCommit', PreTagCommit)
        project.tasks.create('createReleaseTag', CreateReleaseTag)
        project.tasks.create('updateVersion', UpdateVersion)
        project.tasks.create('commitNewVersion', CommitNewVersion)

        Boolean supportsMustRunAfter = project.tasks.initScmAdapter.respondsTo('mustRunAfter')

        if (supportsMustRunAfter) {
            project.tasks.initScmAdapter.mustRunAfter(project.tasks.createScmAdapter)
            project.tasks.checkReleaseNeeded.mustRunAfter(project.tasks.initScmAdapter)
            project.tasks.checkCommitNeeded.mustRunAfter(project.tasks.checkReleaseNeeded)
            project.tasks.checkUpdateNeeded.mustRunAfter(project.tasks.checkCommitNeeded)
            project.tasks.unSnapshotVersion.mustRunAfter(project.tasks.checkUpdateNeeded)
            project.tasks.confirmReleaseVersion.mustRunAfter(project.tasks.unSnapshotVersion)
            project.tasks.checkSnapshotDependencies.mustRunAfter(project.tasks.confirmReleaseVersion)
            project.tasks.runBuildTasks.mustRunAfter(project.tasks.checkSnapshotDependencies)
            project.tasks.preTagCommit.mustRunAfter(project.tasks.runBuildTasks)
            project.tasks.createReleaseTag.mustRunAfter(project.tasks.preTagCommit)
            project.tasks.updateVersion.mustRunAfter(project.tasks.createReleaseTag)
            project.tasks.commitNewVersion.mustRunAfter(project.tasks.updateVersion)
        }

        if (supportsMustRunAfter) {
            project.afterEvaluate {
                def buildTasks = extension.buildTasks
                if (!buildTasks.empty) {
                    buildTasks.each {
                        project.tasks.getByPath(it).mustRunAfter(project.tasks.beforeReleaseBuild)
                        project.tasks.afterReleaseBuild.mustRunAfter(project.tasks.getByPath(it))
                    }
                }
            }
        }

        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (state.failure && task.name == "release") {
                try {
                    createScmAdapter()
                } catch (Exception ignored) {}
                if (scmAdapter && extension.revertOnFail) {
                    if (project.file(extension.versionPropertyFile)?.exists()) {
                        log.error('Release process failed, reverting back any changes made by Release Plugin to ' + project.name)
                        scmAdapter.revert(project.file(extension.versionPropertyFile))
                    }
                } else {
                    log.error('Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.')
                }
            }
        }
    }

    String getPath(Project project) {
        return !project.path.endsWith(Project.PATH_SEPARATOR) ? project.path + Project.PATH_SEPARATOR : project.path
    }

    void createScmAdapter() {
        scmAdapter = findScmAdapter()
        extension.scmAdapter = scmAdapter
    }

    /**
     * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
     * @param directory the directory to start from
     */
    protected BaseScmAdapter findScmAdapter() {
        BaseScmAdapter adapter
        File projectPath = project.projectDir.canonicalFile

        extension.scmAdapters.find {
            assert BaseScmAdapter.isAssignableFrom(it)

            BaseScmAdapter instance = it.getConstructor(Project.class, Map.class).newInstance(project, attributes)
            if (instance.isSupported(projectPath)) {
                adapter = instance
                return true
            }

            return false
        }

        if (adapter == null) {
            throw new GradleException(
                "No supported Adapter could be found. Are [${ projectPath }] or its parents are valid scm directories?")
        }

        adapter
    }
}
