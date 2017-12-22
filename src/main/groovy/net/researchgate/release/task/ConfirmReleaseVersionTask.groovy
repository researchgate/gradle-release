package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class ConfirmReleaseVersionTask extends BaseScmTask {

    @TaskAction
    void confirmReleaseVersion() {
        if (pluginHelper.attributes.propertiesFileCreated) {
            return
        }
        pluginHelper.updateVersionProperty(pluginHelper.getReleaseVersion(project.version))
    }
}
