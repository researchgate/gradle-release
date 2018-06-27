package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CommitNewVersion extends BaseReleaseTask {

    CommitNewVersion() {
        super()
        description = 'Commits the version update to your SCM'
    }

    @TaskAction
    def performTask() {
        if (extension.skipRelease(getProject())) {
            return
        }
        def message = extension.newVersionCommitMessage + " '${tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        getScmAdapter().commit(message)
    }

}
