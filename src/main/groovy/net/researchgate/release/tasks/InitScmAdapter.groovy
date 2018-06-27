package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class InitScmAdapter extends BaseReleaseTask {

    InitScmAdapter() {
        super()
        description = 'Initializes the SCM plugin'
    }

    @TaskAction
    def performTask() {
        getScmAdapter().init()
    }
}
