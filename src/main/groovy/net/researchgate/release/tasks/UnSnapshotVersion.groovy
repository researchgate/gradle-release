package net.researchgate.release.tasks

import groovy.text.SimpleTemplateEngine
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class UnSnapshotVersion extends BaseReleaseTask {

    UnSnapshotVersion() {
        super()
        description = 'Removes "-SNAPSHOT" from your project\'s current version.'
    }

    @TaskAction
    def performTask() {
        if (extension.skipRelease(getProject())) {
            // Replace the version with the latest released version
            String latestTag = scmAdapter.getLatestTag(getProject().name)
            if (latestTag == null) {
                throw new GradleException("The latest tag of '" + getProject().name + "' is not available")
            }

            if (!extension.tagTemplate) {
                throw new GradleException("Skipping a release requires the 'tagTemplate' property to be set")
            }

            // Determine the version number part of the tag using the tag template
            def engine = new SimpleTemplateEngine()
            def binding = [
                    "version": "",
                    "name"   : project.name
            ]
            String tagNamePart = engine.createTemplate(extension.tagTemplate).make(binding).toString()
            String version = latestTag.replaceAll(tagNamePart, "")

            log.debug("Using version " + version + " for " + getProject().name + " dependencies")
            project.version = version
            return
        }
        checkPropertiesFile()
        def version = project.version.toString()

        if (version.contains('-SNAPSHOT')) {
            projectAttributes.usesSnapshot = true
            version -= '-SNAPSHOT'
            updateVersionProperty(version)
        }
    }

    def checkPropertiesFile() {
        File propertiesFile = findPropertiesFile()

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
