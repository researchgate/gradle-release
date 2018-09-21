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
        if (extension.isUseMultipleVersionFiles()) {
            String message = ""
            project.getSubprojects().each { Project subProject ->
                if (extension.skipRelease(subProject)) {
                    return
                }
                if (message.isEmpty()) {
                    if (extension.preCommitText) {
                        message = "${extension.preCommitText} "
                    }
                    message += extension.newVersionCommitMessage + " '${tagName(subProject)}'"
                } else {
                     message += " '${tagName(subProject)}'"
                }
            }
            getScmAdapter().commit(message + '.')
        } else {
            String message = extension.newVersionCommitMessage + " '${tagName(project)}'."
            if (extension.preCommitText) {
                message = "${extension.preCommitText} ${message}"
            }
            getScmAdapter().commit(message)
        }
    }
}
