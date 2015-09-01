/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskState

import java.util.regex.Matcher

class ReleasePlugin extends PluginHelper implements Plugin<Project> {

    static final String RELEASE_GROUP = 'Release'

    private BaseScmAdapter scmAdapter

    void apply(Project project) {
        this.project = project
        extension = project.extensions.create('release', ReleaseExtension, project)

        def preCommitText = findProperty('release.preCommitText') ?: findProperty('preCommitText')
        if (preCommitText) {
            extension.preCommitText = preCommitText
        }

        project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP, type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()

            tasks = [
                    'createScmAdapter',
                    //  0. (This Plugin) Initializes the corresponding SCM plugin (Git/Bazaar/Svn/Mercurial).
                    'initScmAdapter',
                    //  1. (SCM Plugin) Check to see if source needs to be checked in.
                    'checkCommitNeeded',
                    //  2. (SCM Plugin) Check to see if source is out of date
                    'checkUpdateNeeded',
                    //  3. (This Plugin) Update Snapshot version if used
                    //     Needs to be done before checking for snapshot versions since the project might depend on other
                    //     Modules within the same project.
                    'unSnapshotVersion',
                    //  4. (This Plugin) Confirm this release version
                    'confirmReleaseVersion',
                    //  5. (This Plugin) Check for SNAPSHOT dependencies if required.
                    'checkSnapshotDependencies',
                    //  6. (This Plugin) Build && run Unit tests
                    'runBuildTasks',
                    //  7. (This Plugin) Commit Snapshot update (if done)
                    'preTagCommit',
                    //  8. (SCM Plugin) Create tag of release.
                    'createReleaseTag',
                    //  9. (This Plugin) Update version to next version.
                    'updateVersion',
                    // 10. (This Plugin) Commit version update.
                    'commitNewVersion'
            ]
        }

        project.task('createScmAdapter', group: RELEASE_GROUP,
            description: 'Finds the correct SCM plugin') << this.&createScmAdapter
        project.task('initScmAdapter', group: RELEASE_GROUP,
            description: 'Initializes the SCM plugin') << this.&initScmAdapter
        project.task('checkCommitNeeded', group: RELEASE_GROUP,
            description: 'Checks to see if there are any added, modified, removed, or un-versioned files.') << this.&checkCommitNeeded
        project.task('checkUpdateNeeded', group: RELEASE_GROUP,
            description: 'Checks to see if there are any incoming or outgoing changes that haven\'t been applied locally.') << this.&checkUpdateNeeded
        project.task('checkSnapshotDependencies', group: RELEASE_GROUP,
            description: 'Checks to see if your project has any SNAPSHOT dependencies.') << this.&checkSnapshotDependencies
        project.task('unSnapshotVersion', group: RELEASE_GROUP,
            description: 'Removes "-SNAPSHOT" from your project\'s current version.') << this.&unSnapshotVersion
        project.task('confirmReleaseVersion', group: RELEASE_GROUP,
            description: 'Prompts user for this release version. Allows for alpha or pre releases.') << this.&confirmReleaseVersion
        project.task('preTagCommit', group: RELEASE_GROUP,
            description: 'Commits any changes made by the Release plugin - eg. If the unSnapshotVersion tas was executed') << this.&preTagCommit
        project.task('updateVersion', group: RELEASE_GROUP,
            description: 'Prompts user for the next version. Does it\'s best to supply a smart default.') << this.&updateVersion
        project.task('commitNewVersion', group: RELEASE_GROUP,
            description: 'Commits the version update to your SCM') << this.&commitNewVersion
        project.task('createReleaseTag', group: RELEASE_GROUP,
            description: 'Creates a tag in SCM for the current (un-snapshotted) version.') << this.&commitTag
        project.task('runBuildTasks', group: RELEASE_GROUP, description: 'Runs the build process in a separate gradle run.', type: GradleBuild) {
            startParameter = project.getGradle().startParameter.newInstance()

            project.afterEvaluate {
                tasks = [
                    'beforeReleaseBuild',
                     extension.buildTasks,
                     'afterReleaseBuild'
                ].flatten()
            }
        }

        project.task('beforeReleaseBuild', group: RELEASE_GROUP, description: 'Runs immediately before the build when doing a release') {}
        project.task('afterReleaseBuild', group: RELEASE_GROUP, description: 'Runs immediately after the build when doing a release') {}

        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (state.failure && task.name == "release") {
                try {
                    createScmAdapter()
                } catch (Exception e) {}
                if (scmAdapter && extension.revertOnFail && project.file(extension.versionPropertyFile)?.exists()) {
                    log.error('Release process failed, reverting back any changes made by Release Plugin.')
                    scmAdapter.revert()
                } else {
                    log.error('Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.')
                }
            }
        }
    }

    void createScmAdapter() {
        scmAdapter = findScmAdapter()
    }

    void initScmAdapter() {
        checkPropertiesFile()
        scmAdapter.init()
    }

    void checkCommitNeeded() {
        scmAdapter.checkCommitNeeded()
    }

    void checkUpdateNeeded() {
        scmAdapter.checkUpdateNeeded()
    }

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
            warnOrThrow(extension.failOnSnapshotDependencies, message)
        }
    }

    void commitTag() {
        def message = extension.tagCommitMessage + " '${tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        scmAdapter.createReleaseTag(message)
    }

    void confirmReleaseVersion() {
        def version = getReleaseVersion()
        updateVersionProperty(version)
    }

    String getReleaseVersion(String candidateVersion = "${project.version}") {
        String releaseVersion = project.properties['releaseVersion']

        if (useAutomaticVersion()) {
			releaseVersion = scmAdapter.assignReleaseVersionAutomatically(candidateVersion)
            return releaseVersion ?: candidateVersion
        }

        return readLine("This release version:", releaseVersion ?: candidateVersion)
    }

    void unSnapshotVersion() {
        def version = project.version.toString()

        if (version.contains('-SNAPSHOT')) {
            project.ext.set('usesSnapshot', true)
            project.ext.set('snapshotVersion', version)
            version -= '-SNAPSHOT'
            updateVersionProperty(version)
        } else {
            project.ext.set('usesSnapshot', false)
        }
    }

    void preTagCommit() {
        if (project.properties['usesSnapshot'] || project.properties['versionModified']) {
            // should only be committed if the project was using a snapshot version.
            def message = extension.preTagCommitMessage + " '${tagName()}'."

            if (extension.preCommitText) {
                message = "${extension.preCommitText} ${message}"
            }
            scmAdapter.commit(message)
        }
    }

    void updateVersion() {
        def version = project.version.toString()
        Map<String, Closure> patterns = extension.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key
            //noinspection GroovyUnusedAssignment
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, project)
                if (project.properties['usesSnapshot']) {
                    nextVersion += '-SNAPSHOT'
                }

                nextVersion = getNextVersion(nextVersion)

                project.ext.set('release.oldVersion', project.version)
                project.ext.set('release.newVersion', nextVersion)
                updateVersionProperty(nextVersion)
                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }

    String getNextVersion(String candidateVersion) {
        String nextVersion = project.properties['newVersion']

        if (useAutomaticVersion()) {
            return nextVersion ?: candidateVersion
        }

        return readLine("Enter the next version (current one released as [${project.version}]):", nextVersion ?: candidateVersion)
    }

    def commitNewVersion() {
        def message = extension.newVersionCommitMessage + " '${tagName()}'."
        if (extension.preCommitText) {
            message = "${extension.preCommitText} ${message}"
        }
        scmAdapter.commit(message)
    }


    def checkPropertiesFile() {
        File propertiesFile = findPropertiesFile()

        if (!propertiesFile.canRead() || !propertiesFile.canWrite()) {
            throw new GradleException("Unable to update version property. Please check file permissions.")
        }

        Properties properties = new Properties()
        propertiesFile.withReader { properties.load(it) }

        assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
        assert extension.versionPatterns.keySet().any { (properties.version =~ it).find() },
            "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
                extension.versionPatterns.keySet()

        // set the project version from the properties file if it was not otherwise specified
        if (!isVersionDefined()) {
            project.version = properties.version
        }
    }

    /**
     * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
     * @param directory the directory to start from
     */
    protected BaseScmAdapter findScmAdapter() {
        BaseScmAdapter adapter
        File projectPath = project.rootProject.projectDir.canonicalFile

        extension.scmAdapters.find {
            assert BaseScmAdapter.isAssignableFrom(it)

            BaseScmAdapter instance = it.getConstructor(Project.class).newInstance(project)
            if (instance.isSupported(projectPath)) {
                adapter = instance
                return true
            }

            return false
        }

        if (adapter == null) {
            throw new GradleException(
                "No supported Adapter could be found. Are [${ projectPath }] or its parents are valid scm directories?")
        }

        adapter
    }
}
