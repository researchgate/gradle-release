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
    def performTask() {
        if (extension.skipRelease(getProject())) {
            return
        }
        Project project = getProject();
        def version = project.version.toString()
        Map<String, Closure> patterns = extension.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, project)
                if (projectAttributes.usesSnapshot) {
                    nextVersion += '-SNAPSHOT'
                }

                nextVersion = getNextVersion(nextVersion)
                updateVersionProperty(nextVersion)

                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }

    String getNextVersion(String candidateVersion) {
        String key = isMultiVersionProject() ? "release.${project.name}.newVersion" : "release.newVersion"
        String nextVersion = findProperty(key, null, 'newVersion')

        if (useAutomaticVersion()) {
            return nextVersion ?: candidateVersion
        }

        return readLine("Enter the next version for " + getProject().name + " (current one released as [${project.version}]):", nextVersion ?: candidateVersion)
    }

}
