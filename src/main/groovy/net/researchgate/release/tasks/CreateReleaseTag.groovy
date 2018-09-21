package net.researchgate.release.tasks

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateReleaseTag extends BaseReleaseTask {

    CreateReleaseTag() {
        super()
        description = 'Checks to see if your project has any SNAPSHOT dependencies.'
    }

    @TaskAction
    def createReleaseTag() {
        if (extension.isUseMultipleVersionFiles()) {
            project.subprojects.each { Project subProject ->
                if (extension.skipRelease(subProject)) {
                    return
                }
                createReleaseTagFor(subProject)
            }

        } else {
            createReleaseTagFor(getProject())
        }
    }

    void createReleaseTagFor(Project project) {
        def message = extension.tagCommitMessage + " '${tagName(project)}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        getScmAdapter().createReleaseTag(message, tagName(project))
    }

}
