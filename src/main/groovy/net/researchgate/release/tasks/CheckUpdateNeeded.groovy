package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CheckUpdateNeeded extends BaseReleaseTask {

    CheckUpdateNeeded() {
        super()
        description = 'Checks to see if there are any incoming or outgoing changes that haven\'t been applied locally.'
    }

    @TaskAction
    def performTask() {
        getScmAdapter().checkUpdateNeeded()
    }
}
