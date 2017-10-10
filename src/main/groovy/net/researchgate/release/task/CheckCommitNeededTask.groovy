package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class CheckCommitNeededTask extends BaseScmTask {

    @TaskAction
    void checkCommitNeeded() {
        getScmAdapter().checkCommitNeeded()
    }
}
