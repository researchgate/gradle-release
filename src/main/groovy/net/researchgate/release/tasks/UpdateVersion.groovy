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
        if (extension.isUseMultipleVersionFiles()) {
            project.subprojects { Project subProject ->
                if (!extension.skipRelease(subProject)) {
                    setSnapshotVersion(subProject)
                }
            }
        } else {
            setSnapshotVersion(getProject())
        }
    }

    void setSnapshotVersion(Project projectToSnapshot) {
        def version = projectToSnapshot.version.toString()
        Map<String, Closure> patterns = extension.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, projectToSnapshot)
                Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(projectToSnapshot.name)
                if (projectAttributes.usesSnapshot) {
                    nextVersion += '-SNAPSHOT'
                }

                nextVersion = getNextVersion(projectToSnapshot, nextVersion)
                updateVersionProperty(projectToSnapshot, nextVersion)

                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }

    String getNextVersion(Project projectToGetVersionFrom, String candidateVersion) {
        String key = isMultiVersionProject() ? "release.${projectToGetVersionFrom.name}.newVersion" : "release.newVersion"
        String nextVersion = findProperty(key, null, 'newVersion')

        if (useAutomaticVersion()) {
            return nextVersion ?: candidateVersion
        }

        return readLine("Enter the next version for " + projectToGetVersionFrom.name + " (current one released as [${projectToGetVersionFrom.version}]):", nextVersion ?: candidateVersion)
    }

}
