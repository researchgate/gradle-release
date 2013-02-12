package release

import org.ajoberstar.gradle.git.api.TrackingStatus
import org.ajoberstar.gradle.git.tasks.GitBranchList
import org.ajoberstar.gradle.git.tasks.GitBranchTrackingStatus
import org.ajoberstar.gradle.git.tasks.GitCheckout
import org.ajoberstar.gradle.git.tasks.GitCommit
import org.ajoberstar.gradle.git.tasks.GitFetch
import org.ajoberstar.gradle.git.tasks.GitPush
import org.ajoberstar.gradle.git.tasks.GitStatus
import org.ajoberstar.gradle.git.tasks.GitTag
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

    private GitBranchList gitBranchListTask
    private GitStatus gitStatusTask
    private GitFetch gitFetchTask
    private GitBranchTrackingStatus gitBranchTrackingStatus
    private GitCommit gitCommit
    private GitPush gitPush
    private GitTag gitTag
    private GitCheckout gitCheckout;

    @Override
    void apply(Project project) {
        super.apply(project)
        this.gitBranchListTask = project.tasks.add(name: 'releaseGitBranchList', type: GitBranchList)
        this.gitStatusTask = project.tasks.add(name: 'releaseGitStatus', type: GitStatus)
        this.gitFetchTask = project.tasks.add(name: 'releaseGitFetch', type: GitFetch)
        this.gitBranchTrackingStatus = project.tasks.add(name: 'releaseGitBranchTrackingStatus', type: GitBranchTrackingStatus)
        this.gitCommit = project.tasks.add(name: 'releaseGitCommit', type: GitCommit) {
            commitAll = true
        }
        this.gitPush = project.tasks.add(name: 'releaseGitPush', type: GitPush)
        this.gitTag = project.tasks.add(name: 'releaseGitTag', type: GitTag)
        this.gitCheckout = project.tasks.add(name: 'releaseGitCheckout', type: GitCheckout) {
            startPoint = Constants.HEAD
        }
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
        gitStatusTask.execute()
        if (!gitStatusTask.untracked.isEmpty()) {
            warnOrThrow(releaseConvention().failOnCommitNeeded,
                ['You have unversioned files:', LINE, gitStatusTask.untracked.files*.name, LINE].flatten().join("\n"))
        } else {
            def modifiedFiles = []
            if (!gitStatusTask.added.isEmpty()) {
                modifiedFiles << gitStatusTask.added.files*.name
            }
            if (!gitStatusTask.changed.isEmpty()) {
                modifiedFiles << gitStatusTask.changed.files*.name
            }
            if (!gitStatusTask.modified.isEmpty()) {
                modifiedFiles << gitStatusTask.modified.files*.name
            }
            if (!modifiedFiles.isEmpty()) {
                warnOrThrow(releaseConvention().failOnCommitNeeded,
                    ['You have uncommitted files:', LINE, modifiedFiles, LINE].flatten().join("\n"))
            }
        }
    }


    @Override
    void checkUpdateNeeded() {
        gitFetchTask.execute()
        gitBranchTrackingStatus.setLocalBranch(gitCurrentBranch())
        gitBranchTrackingStatus.execute()
        TrackingStatus st = gitBranchTrackingStatus.trackingStatus

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

        gitTag.with {
            message = msg ?: "Created by Release Plugin: ${tagNm}"
            tagName = tagNm
            execute()
        }

        gitPush.with {
            pushTags = true
            pushAll = false
            execute()
        }
    }


    @Override
    void commit(String msg) {
        gitCommit.with {
            message = msg
            execute()
        }
        gitPush.execute()
    }

    @Override
    void revert() {
        gitCheckout.include(findPropertiesFile().name)
        gitCheckout.execute()
    }

    private String gitCurrentBranch() {
        gitBranchListTask.execute()
        return gitBranchListTask.workingBranch.name
    }
}
