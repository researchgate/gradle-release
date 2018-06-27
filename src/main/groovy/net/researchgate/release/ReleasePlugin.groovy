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

        project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP, type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()

            /**
             *  We use a separate 'runBuildTasks' GradleBuild process since we only have access to the extension
             *  properties after the project has been evaluated to decide which tasks to also include in the build.
             */
            tasks = [
                    "${rootPath}runBuildTasks" as String,
            ].flatten()
        }

        project.tasks.create('prepareVersions', PrepareVersions)

        // The SCM adapter is created in the plugin context since its needed to revert changes on task failure
        project.task('createScmAdapter', group: RELEASE_GROUP,
            description: 'Finds the correct SCM plugin') doLast this.&createScmAdapter

        project.tasks.create('initScmAdapter', InitScmAdapter)
        project.tasks.create('checkCommitNeeded', CheckCommitNeeded)
        project.tasks.create('checkUpdateNeeded', CheckUpdateNeeded)

        conf(project)
        project.subprojects?.each {
            conf(it.project)
        }

        project.task('runBuildTasks', group: RELEASE_GROUP,
                description: 'Runs the build process in a separate gradle run.', type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()

            project.afterEvaluate {

                // Tasks should be added to this list in the order that they should be executed
                List taskList = new ArrayList();
                taskList.add("${rootPath}createScmAdapter" as String)
                taskList.add("${rootPath}initScmAdapter" as String)
                taskList.add("${rootPath}checkCommitNeeded" as String)
                taskList.add("${rootPath}checkUpdateNeeded" as String)
                taskList.add("${rootPath}prepareVersions" as String)

                if (extension.useMultipleVersionFiles) {

                    project.subprojects?.each {
                        String subPath = getPath(it.project)
                        taskList.add("${subPath}unSnapshotVersion" as String)
                        taskList.add("${subPath}confirmReleaseVersion" as String)
                    }

                    taskList.add("${rootPath}checkSnapshotDependencies" as String)
                    extension.buildTasks?.each {
                        taskList.add("${rootPath}" + it as String)
                    }

                    project.subprojects?.each {
                        String subPath = getPath(it.project)
                        taskList.add("${subPath}preTagCommit" as String)
                        taskList.add("${subPath}createReleaseTag" as String)
                        taskList.add("${subPath}updateVersion" as String)
                        taskList.add("${subPath}commitNewVersion" as String)
                    }
                } else {
                    taskList.add("${rootPath}unSnapshotVersion" as String)
                    taskList.add("${rootPath}confirmReleaseVersion" as String)
                    taskList.add("${rootPath}checkSnapshotDependencies" as String)

                    extension.buildTasks?.each {
                        taskList.add("${rootPath}" + it as String)
                    }

                    taskList.add("${rootPath}preTagCommit" as String)
                    taskList.add("${rootPath}createReleaseTag" as String)
                    taskList.add("${rootPath}updateVersion" as String)
                    taskList.add("${rootPath}commitNewVersion" as String)
                }

                Boolean supportsMustRunAfter = project.tasks.initScmAdapter.respondsTo('mustRunAfter')

                if (supportsMustRunAfter) {
                    Task previousTask = null
                    for (def task : taskList) {
                        Task currentTask = project.task(task)
                        if (previousTask != null) {
                            currentTask.mustRunAfter(previousTask)
                        }
                        previousTask = currentTask;
                    }
                }

                tasks = taskList
            }
        }
    }

    String getPath(Project project) {
        return !project.path.endsWith(Project.PATH_SEPARATOR) ? project.path + Project.PATH_SEPARATOR : project.path
    }

    void conf(Project project) {
        project.tasks.create('unSnapshotVersion', UnSnapshotVersion)
        project.tasks.create('confirmReleaseVersion', ConfirmReleaseVersion)
        project.tasks.create('checkSnapshotDependencies', CheckSnapshotDependencies)
        project.tasks.create('preTagCommit', PreTagCommit)
        project.tasks.create('createReleaseTag', CreateReleaseTag)
        project.tasks.create('updateVersion', UpdateVersion)
        project.tasks.create('commitNewVersion', CommitNewVersion)

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
