package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CheckCommitNeeded extends BaseReleaseTask {

    CheckCommitNeeded() {
        super()
        description = 'Checks to see if there are any added, modified, removed, or un-versioned files.'
    }

    @TaskAction
    def performTask() {
        getScmAdapter().checkCommitNeeded()
    }
}
