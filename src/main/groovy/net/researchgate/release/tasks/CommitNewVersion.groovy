package net.researchgate.release.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CommitNewVersion extends BaseReleaseTask {

    CommitNewVersion() {
        super()
        description = 'Commits the version update to your SCM'
    }

    @TaskAction
    def commitNewVersion() {
        String message = extension.newVersionCommitMessage.get() + " '${tagName()}'."
        if (extension.preCommitText.get()) {
            message = "${extension.preCommitText.get()} - ${message}"
        }
        getScmAdapter().commit(message)
    }
}
