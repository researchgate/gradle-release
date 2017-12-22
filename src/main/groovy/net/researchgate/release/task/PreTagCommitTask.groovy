package net.researchgate.release.task

import net.researchgate.release.BaseScmAdapter
import org.gradle.api.tasks.TaskAction


class PreTagCommitTask extends BaseScmTask {

    @TaskAction
    void preTagCommit() {
        BaseScmAdapter adapter = getScmAdapter()
        Map attributes = pluginHelper.attributes
        if (attributes.usesSnapshot || attributes.versionModified || attributes.propertiesFileCreated) {
            // should only be committed if the project was using a snapshot version.
            def message = extension.preTagCommitMessage + " '${pluginHelper.tagName()}'."

            if (extension.preCommitText) {
                message = "${extension.preCommitText} ${message}"
            }

            if (attributes.propertiesFileCreated) {
                adapter.add(pluginHelper.findPropertiesFile())
            }
            adapter.commit(message)
        }
    }
}
