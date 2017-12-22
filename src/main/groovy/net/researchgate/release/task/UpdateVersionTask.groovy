package net.researchgate.release.task

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher

class UpdateVersionTask extends BaseScmTask {

    @TaskAction
    void updateVersion() {
        def version = project.version.toString()
        Map<String, Closure> patterns = extension.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, project)
                if (pluginHelper.attributes.usesSnapshot || Boolean.TRUE.equals(extension.useSnapshotVersion)) {
                    nextVersion += '-SNAPSHOT'
                }

                nextVersion = pluginHelper.getNextVersion(nextVersion)
                pluginHelper.updateVersionProperty(nextVersion)

                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }
}
