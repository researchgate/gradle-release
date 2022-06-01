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
    def prepareVersions() {
        Project rootProject = getRootProject()
        String versionPropertyFilepath = extension.versionPropertyFile.get()
        File rootVersionFile = rootProject.file(versionPropertyFilepath)
        if (rootVersionFile.file) {
            return
        }

        checkSnapshotUse(project.name)
        createPropertiesFile(project, rootVersionFile)
    }

    void checkSnapshotUse(String projectName) {
        if (!useAutomaticVersion() && promptYesOrNo("For project '${projectName}' do you want to use SNAPSHOT versions in between releases")) {
            extension.attributes.usesSnapshot = true
        }
    }

    void createPropertiesFile(Project project, File propertiesFile) {
        log.info("Creating version file '" + propertiesFile.canonicalPath + "' for project '" + project.name + "'")
        if (!isVersionDefined()) {
            project.version = releaseVersion('1.0.0')
        }

        if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
            writeVersion(propertiesFile, 'version', project.version)
            extension.attributes.propertiesFileCreated = true
        } else {
            log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
            throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
        }
    }
}
