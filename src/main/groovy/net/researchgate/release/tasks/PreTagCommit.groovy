package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class PreTagCommit extends BaseReleaseTask {

    @Inject
    PreTagCommit() {
        super()
        description = 'Commits any changes made by the Release plugin - eg. If the unSnapshotVersion task was executed'
    }

    @TaskAction
    void preTagCommit() {
        BaseScmAdapter scmAdapter = ((ReleaseExtension) getRootProject().extensions.getByName("release")).scmAdapter
        if (extension.isUseMultipleVersionFiles()) {
            String message = ""
            if (extension.preCommitText) {
                message = "${extension.preCommitText} "
            }
            message += extension.preTagCommitMessage
            project.subprojects.each { Project subProject ->
                if (extension.skipRelease(subProject)) {
                    return
                }
                Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(subProject.name)
                if (projectAttributes.usesSnapshot || projectAttributes.versionModified || projectAttributes.propertiesFileCreated) {
                    // should only be committed if the project was using a snapshot version.
                    message += " '${tagName(subProject)}'"

                    if (projectAttributes.propertiesFileCreated) {
                        scmAdapter.add(findPropertiesFile(subProject))
                    }
                }
            }
            scmAdapter.commit(message + '.')
        } else {
            Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(project.name)
            if (projectAttributes.usesSnapshot || projectAttributes.versionModified || projectAttributes.propertiesFileCreated) {
                // should only be committed if the project was using a snapshot version.
                String message = extension.preTagCommitMessage + " '${tagName(project)}'."
                if (extension.preCommitText) {
                    message = "${extension.preCommitText} ${message}"
                }
                if (projectAttributes.propertiesFileCreated) {
                    scmAdapter.add(findPropertiesFile(project))
                }
                scmAdapter.commit(message)
            }
        }
    }

}
