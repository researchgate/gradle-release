package net.researchgate.release.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class PrepareVersions extends BaseReleaseTask {

    PrepareVersions() {
        super()
        description = 'Locates project version files or prompts for creation'
    }

    @TaskAction
    def performTask() {
        Project rootProject = getRootProject()
        def versionPropertyFile = extension.versionPropertyFile
        File rootVersionFile = rootProject.file(versionPropertyFile)
        if (rootVersionFile.file) {
            if (isMultiVersionProject()) {
                throw new GradleException("[$rootVersionFile.canonicalPath] was found but the 'useMultipleVersionFiles' config is set to true")
            }
            return
        }

        if (isMultiVersionProject()) {
            def subProjectsWithoutVersions = rootProject.subprojects.findAll {
                !it.project.file(versionPropertyFile).file
            }
            if (subProjectsWithoutVersions) {
                checkSnapshotUse()
            }
            subProjectsWithoutVersions.each {
                File subProjectVersionFile = it.project.file(versionPropertyFile)
                createPropertiesFile(it.project, subProjectVersionFile)
            }
        } else {
            checkSnapshotUse()
            createPropertiesFile(getProject(), rootVersionFile)
        }
    }

    void checkSnapshotUse() {
        if (!useAutomaticVersion() && promptYesOrNo('Do you want to use SNAPSHOT versions in between releases')) {
            extension.attributes.usesSnapshot = true
        }
    }

    void createPropertiesFile(Project project, File propertiesFile) {
        log.debug("Creating version file '" + propertiesFile.canonicalPath + "' for project '" + project.name + "'")
        if (!isVersionDefined()) {
            project.version = getReleaseVersion('1.0.0')
        }

        if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
            writeVersion(propertiesFile, 'version', project.version)
            extension.getOrCreateProjectAttributes(getProject().name).propertiesFileCreated = true
        } else {
            log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
            throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
        }
    }
}
