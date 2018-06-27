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
    def performTask() {
        if (extension.skipRelease(getProject())) {
            return
        }
        if (projectAttributes.propertiesFileCreated) {
            return
        }
        updateVersionProperty(getReleaseVersion())
    }

}
