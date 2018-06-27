package net.researchgate.release.tasks

import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class CheckSnapshotDependencies extends BaseReleaseTask {

    @Inject
    CheckSnapshotDependencies() {
        super()
        description = 'Checks to see if your project has any SNAPSHOT dependencies.'
    }

    @TaskAction
    def performTask() {
        def matcher = { Dependency d -> d.version?.contains('SNAPSHOT') && !extension.ignoredSnapshotDependencies.contains("${d.group ?: ''}:${d.name}".toString()) }
        def collector = { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

        def message = ""

        project.allprojects.each { project ->
            def snapshotDependencies = [] as Set
            project.configurations.each { cfg ->
                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
            }
            project.buildscript.configurations.each { cfg ->
                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
            }
            if (snapshotDependencies.size() > 0) {
                message += "\n\t${project.name}: ${snapshotDependencies}"
            }
        }

        if (message) {
            message = "Snapshot dependencies detected: ${message}"
            warnOrThrow(extension.failOnSnapshotDependencies, message)
        }
    }

}
