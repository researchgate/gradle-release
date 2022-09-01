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
        BaseScmAdapter scmAdapter = ((ReleaseExtension) project.extensions.getByName("release")).scmAdapter
        Map<String, Object> projectAttributes = extension.attributes
        if (projectAttributes.usesSnapshot || projectAttributes.versionModified || projectAttributes.propertiesFileCreated) {
            // should only be committed if the project was using a snapshot version.
            String message = extension.preTagCommitMessage.get() + " '${tagName()}'."
            if (extension.preCommitText) {
                message = "${extension.preCommitText.get()} ${message}"
            }
            if (projectAttributes.propertiesFileCreated) {
                scmAdapter.add(findPropertiesFile(project))
            }
            scmAdapter.commit(message)
        }
    }

}
