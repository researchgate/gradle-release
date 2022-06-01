package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class ConfirmReleaseVersion extends BaseReleaseTask {

    @Inject
    ConfirmReleaseVersion() {
        super()
        description = 'Prompts user for this release version. Allows for alpha or pre releases.'
    }

    @TaskAction
    def confirmReleaseVersion() {
        Map<String, Object> projectAttributes = extension.attributes
        if (projectAttributes.propertiesFileCreated) {
            return
        }
        scmAdapter.updateVersionProperty(releaseVersion())
    }
}
