package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class PreTagCommit extends BaseReleaseTask {

    @Inject
    PreTagCommit() {
        super()
        description = 'Commits any changes made by the Release plugin - eg. If the unSnapshotVersion task was executed'
    }

    @TaskAction
    def performTask() {
        if (extension.skipRelease(getProject())) {
            return
        }
        BaseScmAdapter scmAdapter = ((ReleaseExtension) getRootProject().extensions.getByName("release")).scmAdapter

        if (projectAttributes.usesSnapshot || projectAttributes.versionModified || projectAttributes.propertiesFileCreated) {
            // should only be committed if the project was using a snapshot version.
            def message = extension.preTagCommitMessage + " '${tagName()}'."

            if (extension.preCommitText) {
                message = "${extension.preCommitText} ${message}"
            }

            if (projectAttributes.propertiesFileCreated) {
                scmAdapter.add(findPropertiesFile());
            }
            scmAdapter.commit(message)
        }
    }

}
