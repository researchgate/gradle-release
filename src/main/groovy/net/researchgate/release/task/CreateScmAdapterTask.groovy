package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class CreateScmAdapterTask extends BaseScmTask {

    @TaskAction
    void createScmAdapter() {
        getScmAdapter()
    }
}
