package net.researchgate.release.tasks

import groovy.text.SimpleTemplateEngine
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class UnSnapshotVersion extends BaseReleaseTask {

    UnSnapshotVersion() {
        super()
        description = 'Removes "-SNAPSHOT" from your project\'s current version.'
    }

    @TaskAction
    void unSnapshotVersion() {
        checkPropertiesFile(project)
        def version = project.version.toString()

        if (version.contains(extension.snapshotSuffix.get())) {
            Map<String, Object> projectAttributes = extension.attributes
            projectAttributes.usesSnapshot = true
            version -= extension.snapshotSuffix.get()
            scmAdapter.updateVersionProperty(version)
        }
    }

    void checkPropertiesFile(Project project) {
        File propertiesFile = findPropertiesFile(project)

        if (!propertiesFile.canRead() || !propertiesFile.canWrite()) {
            throw new GradleException("Unable to update version property. Please check file permissions.")
        }

        Properties properties = new Properties()
        propertiesFile.withReader { properties.load(it) }

        assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
        assert extension.versionPatterns.keySet().any { (properties.version =~ it).find() },
                "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
                        extension.versionPatterns.keySet()

        // set the project version from the properties file if it was not otherwise specified
        if (!isVersionDefined()) {
            project.version = properties.version
        }
    }
}
