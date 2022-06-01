package net.researchgate.release.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher

class UpdateVersion extends BaseReleaseTask {

    UpdateVersion() {
        super()
        description = 'Prompts user for the next version. Does it\'s best to supply a smart default.'
    }

    @TaskAction
    def updateVersion() {
        setSnapshotVersion()
    }

    void setSnapshotVersion() {
        def version = project.version.toString()
        Map<String, Closure> patterns = extension.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, project)
                Map<String, Object> projectAttributes = extension.attributes
                if (projectAttributes.usesSnapshot) {
                    nextVersion += extension.snapshotSuffix.get()
                }

                nextVersion = getNextVersion(nextVersion)
                scmAdapter.updateVersionProperty(nextVersion)

                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }

    String getNextVersion(String candidateVersion) {
        String key = "release.newVersion"
        String nextVersion = findProperty(key, null, 'newVersion')

        if (useAutomaticVersion()) {
            return nextVersion ?: candidateVersion
        }

        return readLine("Enter the next version (current one released as [${project.version}]):", nextVersion ?: candidateVersion)
    }

}
