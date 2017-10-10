package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class CommitNewVersionTask extends BaseScmTask {

    @TaskAction
    void commitNewVersion() {
        def message = extension.newVersionCommitMessage + " '${pluginHelper.tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        getScmAdapter().commit(message)
    }
}
