package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class UnSnapshotVersionTask extends BaseScmTask {

    @TaskAction
    void unSnapshotVersion() {
        pluginHelper.checkPropertiesFile()
        def version = project.version.toString()

        if (version.contains('-SNAPSHOT') || Boolean.TRUE.equals(extension.useSnapshotVersion)) {
            pluginHelper.attributes.usesSnapshot = true
            version -= '-SNAPSHOT'
            pluginHelper.updateVersionProperty(version)
        }
    }
}
