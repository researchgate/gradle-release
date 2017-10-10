package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class InitScmAdapterTask extends BaseScmTask {

    @TaskAction
    void initScmAdapter() {
        getScmAdapter()
    }
}
