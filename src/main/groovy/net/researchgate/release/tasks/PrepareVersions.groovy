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
        String versionPropertyFilepath = extension.versionPropertyFile
        File rootVersionFile = rootProject.file(versionPropertyFilepath)
        if (rootVersionFile.file) {
            if (isMultiVersionProject()) {
                throw new GradleException("[$rootVersionFile.canonicalPath] was found but the 'useMultipleVersionFiles' config is set to true")
            }
            return
        }

        if (isMultiVersionProject()) {
            Set<Project> subProjectsWithoutVersions = project.subprojects.findAll { Project subProject ->
                !subProject.file(versionPropertyFilepath).file
            }
            subProjectsWithoutVersions.each { Project subProject ->
                checkSnapshotUse(subProject.name)
                File subProjectVersionFile = subProject.file(versionPropertyFilepath)
                createPropertiesFile(subProject, subProjectVersionFile)
            }
        } else {
            checkSnapshotUse(project.name)
            createPropertiesFile(project, rootVersionFile)
        }
    }

    void checkSnapshotUse(String projectName) {
        if (!useAutomaticVersion() && promptYesOrNo("For project '${projectName}' do you want to use SNAPSHOT versions in between releases")) {
            extension.getOrCreateProjectAttributes(projectName).usesSnapshot = true
        }
    }

    void createPropertiesFile(Project project, File propertiesFile) {
        log.info("Creating version file '" + propertiesFile.canonicalPath + "' for project '" + project.name + "'")
        if (!isVersionDefined()) {
            project.version = getReleaseVersion(project, '1.0.0')
        }

        if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
            writeVersion(propertiesFile, 'version', project.version)
            extension.getOrCreateProjectAttributes(project.name).propertiesFileCreated = true
        } else {
            log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
            throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
        }
    }
}
