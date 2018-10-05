package net.researchgate.release.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction

class CheckReleaseNeeded extends BaseReleaseTask {

    @TaskAction
    void checkReleaseNeeded() {
        if (extension.isUseMultipleVersionFiles()) {
            GradleBuild releaseTask = project.tasks.getByName('release') as GradleBuild
            boolean releaseRequired = false
            project.subprojects.each { Project subProject ->
                if (!extension.skipRelease(subProject)) {
                    releaseRequired = true
                } else {
                    String key = "release.${subProject.name}.skipRelease"
                    releaseTask.startParameter.projectProperties.put(key, "true")
                }
            }
            if (!releaseRequired) {
                throw new GradleException("Can't find a project that requires a new release. All are marked as skipped")
            }
        }
    }
}
