package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction


class CommitTagTask extends BaseScmTask {

    @TaskAction
    void commitTag() {
        def message = extension.tagCommitMessage + " '${pluginHelper.tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        getScmAdapter().createReleaseTag(message)
    }
}
