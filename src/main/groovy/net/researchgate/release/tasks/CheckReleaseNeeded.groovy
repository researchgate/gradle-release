package net.researchgate.release.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CheckReleaseNeeded extends BaseReleaseTask {

    @TaskAction
    void checkReleaseNeeded() {
        if (extension.isUseMultipleVersionFiles()) {
            boolean releaseRequired = project.subprojects.any { Project subProject ->
                return !extension.skipRelease(subProject)
            }
            if (!releaseRequired) {
                throw new GradleException("Can't find a project that requires a new release. All are marked as skipped")
            }
        }
    }
}
