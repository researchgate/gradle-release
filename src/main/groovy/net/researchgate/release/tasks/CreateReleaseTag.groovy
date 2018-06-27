package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CreateReleaseTag extends BaseReleaseTask {

    CreateReleaseTag() {
        super()
        description = 'Checks to see if your project has any SNAPSHOT dependencies.'
    }

    @TaskAction
    def performTask() {
        if (extension.skipRelease(getProject())) {
            return
        }
        def message = extension.tagCommitMessage + " '${tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        getScmAdapter().createReleaseTag(message, tagName())
    }

}
