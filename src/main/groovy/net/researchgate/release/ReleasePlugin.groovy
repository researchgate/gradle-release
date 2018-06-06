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

import net.researchgate.release.task.BaseScmTask
import net.researchgate.release.task.CheckCommitNeededTask
import net.researchgate.release.task.CheckSnapshotDependenciesTask
import net.researchgate.release.task.CheckUpdateNeededTask
import net.researchgate.release.task.CommitNewVersionTask
import net.researchgate.release.task.CommitTagTask
import net.researchgate.release.task.ConfirmReleaseVersionTask
import net.researchgate.release.task.CreateScmAdapterTask
import net.researchgate.release.task.InitScmAdapterTask
import net.researchgate.release.task.PreTagCommitTask
import net.researchgate.release.task.RunBuildTasksTask
import net.researchgate.release.task.UnSnapshotVersionTask
import net.researchgate.release.task.UpdateVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskState

class ReleasePlugin implements Plugin<Project> {

    static final String RELEASE_GROUP = 'Release'

    private Project project

    private ReleaseExtension extension

    private PluginHelper pluginHelper

    void apply(Project project) {
        this.project = project
        extension = project.extensions.create('release', ReleaseExtension, project, [:])
        pluginHelper = new PluginHelper(extension, project)

        String preCommitText = pluginHelper.findProperty('release.preCommitText', null, 'preCommitText')
        if (preCommitText) {
            extension.preCommitText = preCommitText
        }

        // name tasks with an absolute path so subprojects can be released independently
        String p = project.path
        p = !p.endsWith(Project.PATH_SEPARATOR) ? p + Project.PATH_SEPARATOR : p

        project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP, type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()

            tasks = [
                "${p}initScmAdapter" as String,
                "${p}checkCommitNeeded" as String,
                "${p}checkUpdateNeeded" as String,
                "${p}unSnapshotVersion" as String,
                "${p}confirmReleaseVersion" as String,
                "${p}checkSnapshotDependencies" as String,
                "${p}runBuildTasks" as String,
                "${p}preTagCommit" as String,
                "${p}createReleaseTag" as String,
                "${p}updateVersion" as String,
                "${p}commitNewVersion" as String
            ]
        }

        project.tasks.create('createScmAdapter', CreateScmAdapterTask.class) {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            delegate.group RELEASE_GROUP
            delegate.description 'Finds the correct SCM plugin'
        }

        InitScmAdapterTask initScmAdapterTask = project.tasks.create('initScmAdapter', InitScmAdapterTask.class)
        initScmAdapterTask.with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            delegate.group RELEASE_GROUP
            delegate.description 'Initializes the SCM plugin'
        }

        project.tasks.create('checkCommitNeeded', CheckCommitNeededTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Checks to see if there are any added, modified, removed, or un-versioned files.'
            dependsOn project.tasks.initScmAdapter
        }
        project.tasks.create('checkUpdateNeeded', CheckUpdateNeededTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Checks to see if there are any incoming or outgoing changes that haven\'t been applied locally.'
            dependsOn project.tasks.checkCommitNeeded
        }
        project.tasks.create('unSnapshotVersion', UnSnapshotVersionTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Removes "-SNAPSHOT" from your project\'s current version.'
            dependsOn project.tasks.checkUpdateNeeded
        }
        project.tasks.create('confirmReleaseVersion', ConfirmReleaseVersionTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Prompts user for this release version. Allows for alpha or pre releases.'
            dependsOn project.tasks.unSnapshotVersion
        }
        project.tasks.create('checkSnapshotDependencies', CheckSnapshotDependenciesTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Checks to see if your project has any SNAPSHOT dependencies.'
            dependsOn project.tasks.confirmReleaseVersion
        }

        project.tasks.create('runBuildTasks', RunBuildTasksTask.class ) {
            group RELEASE_GROUP
            description 'Runs the build process in a separate gradle run.'
            startParameter = project.getGradle().startParameter.newInstance()
            dependsOn project.tasks.checkSnapshotDependencies

            project.afterEvaluate {
                tasks = [
                    "${p}beforeReleaseBuild" as String,
                    extension.buildTasks.collect { p + it },
                    "${p}afterReleaseBuild" as String
                ].flatten()
            }
        }
        project.tasks.create('preTagCommit', PreTagCommitTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Commits any changes made by the Release plugin - eg. If the unSnapshotVersion task was executed'
            dependsOn project.tasks.runBuildTasks
        }
        project.tasks.create('createReleaseTag', CommitTagTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Creates a tag in SCM for the current (un-snapshotted) version.'
            dependsOn project.tasks.preTagCommit
        }
        project.tasks.create('updateVersion', UpdateVersionTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Prompts user for the next version. Does it\'s best to supply a smart default.'
            dependsOn project.tasks.createReleaseTag
        }
        project.tasks.create('commitNewVersion', CommitNewVersionTask.class).with {
            delegate.extension = this.extension
            delegate.pluginHelper = this.pluginHelper
            group RELEASE_GROUP
            description 'Commits the version update to your SCM'
            dependsOn project.tasks.updateVersion
        }

        Boolean supportsMustRunAfter = initScmAdapterTask.respondsTo('mustRunAfter')

        if (supportsMustRunAfter) {
            project.tasks.initScmAdapter.mustRunAfter(project.tasks.createScmAdapter)
            project.tasks.checkCommitNeeded.mustRunAfter(project.tasks.initScmAdapter)
            project.tasks.checkCommitNeeded.mustRunAfter(project.tasks.initScmAdapter)
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

        project.tasks.create(name: 'beforeReleaseBuild', group: RELEASE_GROUP,
            description: 'Runs immediately before the build when doing a release') {}
        project.tasks.create(name:'afterReleaseBuild', group: RELEASE_GROUP,
            description: 'Runs immediately after the build when doing a release') {}

        if (supportsMustRunAfter) {
            project.afterEvaluate {
                def buildTasks = extension.buildTasks
                if (!buildTasks.empty) {
                    project.tasks[buildTasks.first()].mustRunAfter(project.tasks.beforeReleaseBuild)
                    project.tasks.afterReleaseBuild.mustRunAfter(project.tasks[buildTasks.last()])
                }
            }
        }

        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (state.failure && task.name == "release") {
                if (pluginHelper.scmAdapter && extension.revertOnFail && project.file(extension.versionPropertyFile)?.exists()) {
                    project.logger.error('Release process failed, reverting back any changes made by Release Plugin.')
                    pluginHelper.scmAdapter.revert()
                } else {
                    project.logger.error('Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.')
                }
            }
        }

        project.afterEvaluate {
            project.tasks.withType(BaseScmTask.class) {
                delegate.extension = this.extension
                delegate.pluginHelper = this.pluginHelper
            }
        }
    }

}
