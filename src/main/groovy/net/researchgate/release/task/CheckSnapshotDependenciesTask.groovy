package net.researchgate.release.task

import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction

class CheckSnapshotDependenciesTask extends BaseScmTask {

    @TaskAction
    void checkSnapshotDependencies() {
        def matcher = { Dependency d -> d.version?.contains('SNAPSHOT') }
        def collector = { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

        def message = ""

        project.allprojects.each { project ->
            def snapshotDependencies = [] as Set
            project.configurations.each { cfg ->
                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
            }
            if (snapshotDependencies.size() > 0) {
                message += "\n\t${project.name}: ${snapshotDependencies}"
            }
        }

        if (message) {
            message = "Snapshot dependencies detected: ${message}"
            pluginHelper.warnOrThrow(extension.failOnSnapshotDependencies, message)
        }
    }
}
