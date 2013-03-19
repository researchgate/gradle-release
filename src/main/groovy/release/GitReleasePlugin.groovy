package release

import org.ajoberstar.gradle.git.api.TrackingStatus
import org.ajoberstar.gradle.git.tasks.*
import org.eclipse.jgit.lib.Constants
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends BaseScmPlugin<GitReleasePluginConvention> {

    private static final String LINE = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'

    @Override
    void apply(Project project) {
        super.apply(project)
    }

    @Override
    void init() {
        if (convention().requireBranch) {
            def branch = gitCurrentBranch()
            if (!(branch == convention().requireBranch)) {
                throw new GradleException("Current Git branch is \"$branch\" and not \"${ convention().requireBranch }\".")
            }
        }
    }

    @Override
    GitReleasePluginConvention buildConventionInstance() { releaseConvention().git }

    @Override
    void checkCommitNeeded() {
        GitStatus task = configureGitTask(GitStatus)
        task.execute()

        if (!task.untracked.isEmpty()) {
            warnOrThrow(releaseConvention().failOnCommitNeeded,
                    ['You have unversioned files:', LINE, task.untracked.files*.name, LINE].flatten().join("\n"))
        } else {
            def modifiedFiles = []
            if (!task.added.isEmpty()) {
                modifiedFiles << task.added.files*.name
            }
            if (!task.changed.isEmpty()) {
                modifiedFiles << task.changed.files*.name
            }
            if (!task.modified.isEmpty()) {
                modifiedFiles << task.modified.files*.name
            }
            if (!modifiedFiles.isEmpty()) {
                warnOrThrow(releaseConvention().failOnCommitNeeded,
                        ['You have uncommitted files:', LINE, modifiedFiles, LINE].flatten().join("\n"))
            }
        }
    }

    @Override
    void checkUpdateNeeded() {
        configureGitTask(GitFetch).execute()

        TrackingStatus st = configureGitTask(GitBranchTrackingStatus).with {
            localBranch = gitCurrentBranch()
            execute()
            trackingStatus
        }

        if (st?.aheadCount > 0) {
            warnOrThrow(releaseConvention().failOnPublishNeeded, "You have ${st.aheadCount} local change(s) to push.")
        }
        if (st?.behindCount > 0) {
            warnOrThrow(releaseConvention().failOnUpdateNeeded, "You have ${st.behindCount} remote change(s) to pull.")
        }
    }

    @Override
    void createReleaseTag(String msg = "") {
        def tagNm = tagName()
        configureGitTask(GitTag).with {
            message = msg ?: "Created by Release Plugin: ${tagNm}"
            tagName = tagNm
            execute()
        }
        configureGitTask(GitPush).with {
            pushTags = true
            execute()
        }
    }

    @Override
    void commit(String msg) {
        configureGitTask(GitAdd).with {
            include(releaseConvention().versionPropertyFile)
            execute()
        }
        configureGitTask(GitCommit).with {
            include(releaseConvention().versionPropertyFile)
            message = msg
            execute()
        }
        configureGitTask(GitPush).execute()
    }

    @Override
    void revert() {
        configureGitTask(GitCheckout).with {
            startPoint = Constants.HEAD
            include(findPropertiesFile().name)
            execute()
        }
    }

    private String gitCurrentBranch() {
        return configureGitTask(GitBranchList).with {
            execute()
            workingBranch.name
        }
    }

    private <T> T configureGitTask(Class<T> clz) {
        return project.tasks.replace("release${clz.simpleName}", clz)
    }
}
