/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) Dennis Schumann
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import net.researchgate.release.tasks.CheckCommitNeeded
import net.researchgate.release.tasks.CheckSnapshotDependencies
import net.researchgate.release.tasks.CheckUpdateNeeded
import net.researchgate.release.tasks.CheckoutAndMergeToReleaseBranch
import net.researchgate.release.tasks.CheckoutMergeFromReleaseBranch
import net.researchgate.release.tasks.CommitNewVersion
import net.researchgate.release.tasks.ConfirmReleaseVersion
import net.researchgate.release.tasks.CreateReleaseTag
import net.researchgate.release.tasks.InitScmAdapter
import net.researchgate.release.tasks.PreTagCommit
import net.researchgate.release.tasks.PrepareVersions
import net.researchgate.release.tasks.UnSnapshotVersion
import net.researchgate.release.tasks.UpdateVersion
import org.gradle.StartParameter
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskState
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion

import javax.inject.Inject

import static org.gradle.api.logging.configuration.ShowStacktrace.ALWAYS
import static org.gradle.api.logging.configuration.ShowStacktrace.ALWAYS_FULL

abstract class ReleasePlugin extends PluginHelper implements Plugin<Project> {
    static final String RELEASE_GROUP = 'Release'

    private BaseScmAdapter scmAdapter

    void apply(Project project) {
        if (!project.plugins.hasPlugin(BasePlugin.class)) {
            project.plugins.apply(BasePlugin.class)
        }
        this.project = project
        extension = project.extensions.create('release', ReleaseExtension, project, attributes)

        String preCommitText = findProperty('release.preCommitText', null, 'preCommitText')
        if (preCommitText) {
            extension.preCommitText.convention(preCommitText)
        }

        // name tasks with an absolute path so subprojects can be released independently
        String p = project.path
        p = !p.endsWith(Project.PATH_SEPARATOR) ? p + Project.PATH_SEPARATOR : p

        project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP) {
            def projectDirectory = layout.projectDirectory.asFile
            def startParameter = gradle.startParameter
            doLast {
                launchGradle(
                        projectDirectory,
                        startParameter,
                        "${p}createScmAdapter",
                        "${p}initScmAdapter",
                        "${p}checkCommitNeeded",
                        "${p}checkUpdateNeeded",
                        "${p}checkoutMergeToReleaseBranch",
                        "${p}unSnapshotVersion",
                        "${p}confirmReleaseVersion",
                        "${p}checkSnapshotDependencies",
                        "${p}runBuildTasks",
                        "${p}preTagCommit",
                        "${p}createReleaseTag",
                        "${p}checkoutMergeFromReleaseBranch",
                        "${p}updateVersion",
                        "${p}commitNewVersion"
                )
            }
        }

        project.task('beforeReleaseBuild', group: RELEASE_GROUP,
                description: 'Runs immediately before the build when doing a release') {}
        project.task('afterReleaseBuild', group: RELEASE_GROUP,
                description: 'Runs immediately after the build when doing a release') {}
        project.task('createScmAdapter', group: RELEASE_GROUP,
                description: 'Finds the correct SCM plugin') doLast this.&createScmAdapter

        project.tasks.create('initScmAdapter', InitScmAdapter)
        project.tasks.create('checkCommitNeeded', CheckCommitNeeded.class)
        project.tasks.create('checkUpdateNeeded', CheckUpdateNeeded.class)
        project.tasks.create('prepareVersions', PrepareVersions.class)
        project.tasks.create('checkoutMergeToReleaseBranch', CheckoutAndMergeToReleaseBranch.class) {
            onlyIf {
                extension.pushReleaseVersionBranch.isPresent()
            }
        }
        project.tasks.create('unSnapshotVersion', UnSnapshotVersion.class)
        project.tasks.create('confirmReleaseVersion', ConfirmReleaseVersion.class)
        project.tasks.create('checkSnapshotDependencies', CheckSnapshotDependencies.class)
        project.tasks.create('runBuildTasks') {
            group = RELEASE_GROUP
            description = 'Runs the build process in a separate gradle run.'

            def projectDirectory = layout.projectDirectory.asFile
            def startParameter = gradle.startParameter
            doLast {
                startParameter = startParameter.newInstance()
                startParameter.projectProperties.put('release.releasing', "true")
                launchGradle(
                        projectDirectory,
                        startParameter,
                        "${p}beforeReleaseBuild" as String,
                        *extension.buildTasks.get(),
                        "${p}afterReleaseBuild" as String
                )
            }
       }
       project.tasks.create('preTagCommit', PreTagCommit.class)
        project.tasks.create('createReleaseTag', CreateReleaseTag.class)
        project.tasks.create('checkoutMergeFromReleaseBranch', CheckoutMergeFromReleaseBranch) {
            onlyIf {
                extension.pushReleaseVersionBranch.isPresent()
            }
        }
        project.tasks.create('updateVersion', UpdateVersion.class)
        project.tasks.create('commitNewVersion', CommitNewVersion.class)

        project.tasks.initScmAdapter.dependsOn(project.tasks.createScmAdapter)
        project.tasks.checkCommitNeeded.dependsOn(project.tasks.initScmAdapter)
        project.tasks.checkUpdateNeeded.dependsOn(project.tasks.initScmAdapter)
        project.tasks.checkUpdateNeeded.mustRunAfter(project.tasks.checkCommitNeeded)
        project.tasks.checkoutMergeToReleaseBranch.dependsOn(project.tasks.initScmAdapter)
        project.tasks.checkoutMergeToReleaseBranch.mustRunAfter(project.tasks.checkUpdateNeeded)
        project.tasks.unSnapshotVersion.mustRunAfter(project.tasks.checkoutMergeToReleaseBranch)
        project.tasks.confirmReleaseVersion.mustRunAfter(project.tasks.unSnapshotVersion)
        project.tasks.checkSnapshotDependencies.mustRunAfter(project.tasks.confirmReleaseVersion)
        project.tasks.runBuildTasks.mustRunAfter(project.tasks.checkSnapshotDependencies)
        project.tasks.preTagCommit.dependsOn(project.tasks.initScmAdapter)
        project.tasks.preTagCommit.mustRunAfter(project.tasks.runBuildTasks)
        project.tasks.createReleaseTag.dependsOn(project.tasks.initScmAdapter)
        project.tasks.createReleaseTag.mustRunAfter(project.tasks.preTagCommit)
        project.tasks.preTagCommit.dependsOn(project.tasks.initScmAdapter)
        project.tasks.checkoutMergeFromReleaseBranch.mustRunAfter(project.tasks.createReleaseTag)
        project.tasks.updateVersion.mustRunAfter(project.tasks.checkoutMergeFromReleaseBranch)
        project.tasks.commitNewVersion.dependsOn(project.tasks.initScmAdapter)
        project.tasks.commitNewVersion.mustRunAfter(project.tasks.updateVersion)

        project.afterEvaluate {
            def buildTasks = extension.buildTasks.get()
            if (!buildTasks.empty) {
                project.tasks.findByPath(buildTasks.first()).mustRunAfter(project.tasks.beforeReleaseBuild)
                project.tasks.afterReleaseBuild.mustRunAfter(project.tasks.findByPath(buildTasks.last()))
            }
        }

        if (GradleVersion.current() < GradleVersion.version('6.1')) {
            project.gradle.taskGraph.afterTask { Task task, TaskState state ->
                if (state.failure && task.name == 'release') {
                    revert()
                }
            }
        } else {
            objects.newInstance(BuildEventsListenerRegistryProvider)
                  .buildEventsListenerRegistry
                  .onTaskCompletion(providers.provider {
                      { finishEvent ->
                          if ((finishEvent.result instanceof TaskFailureResult) && finishEvent.descriptor.taskPath.endsWith(':release')) {
                              revert()
                          }
                      } as OperationCompletionListener
                  })
        }
    }

    @Inject
    abstract protected ObjectFactory getObjects();

    @Inject
    abstract protected ProviderFactory getProviders();

    @Inject
    abstract protected ProjectLayout getLayout();

    @Inject
    abstract protected Gradle getGradle();

    private static void launchGradle(File projectDirectory, StartParameter startParameter, String... tasks) {
        GradleConnector
              .newConnector()
              .forProjectDirectory(projectDirectory)
              .connect()
              .withCloseable { projectConnection ->
                  def buildLauncher = projectConnection
                        .newBuild()
                        .forTasks(tasks)
                        .setStandardInput(System.in)
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                  if (GradleVersion.current() >= GradleVersion.version("7.4")) {
                      // work-around until https://github.com/researchgate/gradle-release/issues/346 is properly fixed,
                      // then the setting can be translated like for the others
                      buildLauncher.addArguments("--no-configuration-cache")
                  }
                  buildLauncher.addArguments("-Dorg.gradle.logging.level=${startParameter.logLevel}")
                  if (startParameter.showStacktrace == ALWAYS_FULL) {
                      buildLauncher.addArguments("--full-stacktrace")
                  } else if (startParameter.showStacktrace == ALWAYS) {
                      buildLauncher.addArguments("--stacktrace")
                  }
                  buildLauncher
                        .addArguments("--console=${startParameter.consoleOutput}")
                        .addArguments("--warning-mode=${startParameter.warningMode}")
                        .addArguments((startParameter.parallelProjectExecutionEnabled) ? "--parallel" : "--no-parallel")
                        .addArguments("--max-workers=${startParameter.maxWorkerCount}")
                  startParameter.excludedTaskNames.forEach {
                      buildLauncher.addArguments("-x", it)
                  }
                  startParameter.projectProperties.forEach { key, value ->
                      buildLauncher.addArguments("-P$key=$value")
                  }
                  startParameter.systemPropertiesArgs.forEach { key, value ->
                      buildLauncher.addArguments("-D$key=$value")
                  }
                  buildLauncher.addArguments("--gradle-user-home=${startParameter.gradleUserHomeDir}")
                  if ((GradleVersion.current() < GradleVersion.version("9.0")) && (startParameter.settingsFile != null)) {
                      buildLauncher.addArguments("--settings-file=${startParameter.settingsFile}")
                  }
                  if ((GradleVersion.current() < GradleVersion.version("9.0")) && (startParameter.buildFile != null)) {
                      buildLauncher.addArguments("--build-file=${startParameter.buildFile}")
                  }
                  startParameter.initScripts.forEach {
                      buildLauncher.addArguments("--init-script=$it")
                  }
                  if (startParameter.rerunTasks) {
                      buildLauncher.addArguments("--rerun-tasks")
                  }
                  if (startParameter.profile) {
                      buildLauncher.addArguments("--profile")
                  }
                  if (startParameter.continueOnFailure) {
                      buildLauncher.addArguments("--continue")
                  }
                  if (startParameter.offline) {
                      buildLauncher.addArguments("--offline")
                  }
                  if (startParameter.projectCacheDir != null) {
                      buildLauncher.addArguments("--project-cache-dir=${startParameter.projectCacheDir}")
                  }
                  if (startParameter.refreshDependencies) {
                      buildLauncher.addArguments("--refresh-dependencies")
                  }
                  if (startParameter.configureOnDemand) {
                      buildLauncher.addArguments("--configure-on-demand")
                  }
                  startParameter.includedBuilds.forEach {
                      buildLauncher.addArguments("--include-build=$it")
                  }
                  if (startParameter.buildScan) {
                      buildLauncher.addArguments("--scan")
                  }
                  if (startParameter.noBuildScan) {
                      buildLauncher.addArguments("--no-scan")
                  }
                  buildLauncher.run()
              }
    }

    protected revert() {
        try {
            createScmAdapter()
        } catch (Exception ignored) {}
        if (scmAdapter && extension.revertOnFail.get() && project.file(extension.versionPropertyFile)?.exists()) {
            log.error('Release process failed, reverting back any changes made by Release Plugin.')
            scmAdapter.revert()
        } else {
            log.error('Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.')
        }
    }

    void createScmAdapter() {
        scmAdapter = findScmAdapter()
        extension.scmAdapter = scmAdapter
    }

    void checkUpdateNeeded() {
        scmAdapter.checkUpdateNeeded()
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
