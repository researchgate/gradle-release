package release

import org.ajoberstar.gradle.git.api.TrackingStatus
import org.ajoberstar.gradle.git.tasks.GitBranchList
import org.ajoberstar.gradle.git.tasks.GitBranchTrackingStatus
import org.ajoberstar.gradle.git.tasks.GitFetch
import org.ajoberstar.gradle.git.tasks.GitStatus
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

    @Override
    void apply(Project project) {
        super.apply(project)
        this.gitBranchListTask = project.tasks.add(name: 'releaseGitBranchList', type: GitBranchList) {
            type = GitBranchList.BranchType.LOCAL
        }
        this.gitStatusTask = project.tasks.add(name: 'releaseGitStatus', type: GitStatus)
        this.gitFetchTask = project.tasks.add(name: 'releaseGitFetch', type: GitFetch)
        this.gitBranchTrackingStatus = project.tasks.add(name: 'releaseGitBranchTrackingStatus', type: GitBranchTrackingStatus)
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
    void createReleaseTag(String message = "") {
        def tagName = tagName()
        gitExec(['tag', '-a', tagName, '-m', message ?: "Created by Release Plugin: ${tagName}"], "Duplicate tag [$tagName]", 'already exists')
        gitExec(['push', 'origin', tagName], '', '! [rejected]', 'error: ', 'fatal: ')
    }


    @Override
    void commit(String message) {
        gitExec(['commit', '-a', '-m', message], '')
        def pushCmd = ['push', 'origin']
        if (convention().pushToCurrentBranch) {
            pushCmd << gitCurrentBranch()
        }
        gitExec(pushCmd, '', '! [rejected]', 'error: ', 'fatal: ')
    }

    @Override
    void revert() {
        gitExec(['reset', '--hard', 'HEAD', findPropertiesFile().name], "Error reverting changes made by the release plugin.")
    }

    private String gitCurrentBranch() {
        gitBranchListTask.execute()
        return gitBranchListTask.workingBranch.name
    }

    String gitExec(Collection<String> params, String errorMessage, String... errorPattern) {
        def gitDir = project.rootProject.file(".git").canonicalPath.replaceAll("\\\\", "/")
        def workTree = project.rootProject.projectDir.canonicalPath.replaceAll("\\\\", "/")
        def cmdLine = ['git', "--git-dir=${gitDir}", "--work-tree=${workTree}"].plus(params)
        return exec(cmdLine, errorMessage, errorPattern)
    }
}
