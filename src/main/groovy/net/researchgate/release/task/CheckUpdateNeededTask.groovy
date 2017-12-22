package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class CheckUpdateNeededTask extends BaseScmTask {

    @TaskAction
    void checkUpdateNeeded() {
        getScmAdapter().checkUpdateNeeded()
    }
}
