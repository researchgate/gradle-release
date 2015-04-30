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

import org.apache.tools.ant.BuildException

import java.util.regex.Matcher

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskState

class ReleasePlugin extends PluginHelper implements Plugin<Project> {

	static final String RELEASE_GROUP = "Release"

	private BaseScmPlugin scmPlugin

    void apply(Project project) {
		this.project = project
        extension = project.extensions.create('release', ReleaseExtension)

		def preCommitText = findProperty("release.preCommitText") ?: findProperty("preCommitText")
        if (preCommitText) {
            extension.preCommitText = preCommitText
        }

		project.task('release', description: 'Verify project, release, and update version to next.', group: RELEASE_GROUP, type: GradleBuild) {
			startParameter = project.getGradle().startParameter.newInstance()

			tasks = [
					'findScmPlugin',
					//  0. (This Plugin) Initializes the corresponding SCM plugin (Git/Bazaar/Svn/Mercurial).
					'initScmPlugin',
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

		project.task('findScmPlugin', group: RELEASE_GROUP,
			description: 'Finds the correct SCM plugin') << this.&findScmPlugin
		project.task('initScmPlugin', group: RELEASE_GROUP,
			description: 'Initializes the SCM plugin') << this.&initScmPlugin
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

            tasks = [
                'beforeReleaseBuild',
                'build',
                'afterReleaseBuild'
            ]
        }

        project.task('beforeReleaseBuild', group: RELEASE_GROUP, description: 'Runs immediately before the build when doing a release') {}
        project.task('afterReleaseBuild', group: RELEASE_GROUP, description: 'Runs immediately after the build when doing a release') {}

		project.gradle.taskGraph.afterTask { Task task, TaskState state ->
			if (state.failure && task.name == "release") {
                try {
                    findScmPlugin();
                } catch (Exception e) {}
				if (scmPlugin && extension.revertOnFail && project.file(extension.versionPropertyFile)?.exists()) {
					log.error("Release process failed, reverting back any changes made by Release Plugin.")
					scmPlugin.revert()
				} else {
					log.error("Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.")
				}
			}
		}
	}

	void findScmPlugin() {
		scmPlugin = applyScmPlugin()
	}

	void initScmPlugin() {
        checkPropertiesFile()
		scmPlugin.init()
	}

	void checkCommitNeeded() {
		scmPlugin.checkCommitNeeded()
	}

	void checkUpdateNeeded() {
		scmPlugin.checkUpdateNeeded()
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
		scmPlugin.createReleaseTag(message)
	}

	void confirmReleaseVersion() {
		def version = getReleaseVersion()
		updateVersionProperty(version)
	}

    String getReleaseVersion(String candidateVersion = "${project.version}") {
        String releaseVersion = project.properties['releaseVersion']

        if (useAutomaticVersion()) {
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
			scmPlugin.commit(message)
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
		scmPlugin.commit(message)
	}


	def checkPropertiesFile() {
		File propertiesFile = findPropertiesFile()

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

		try {
			// test to make sure the version property is in the correct version=[version] format.
			project.ant.replace(file: propertiesFile, token: "version=${project.version}", value: "version=${project.version}", failOnNoReplacements: true, preserveLastModified: true)
		} catch (BuildException be) {
			throw new GradleException("Unable to update version property. Please check file permissions, and ensure property is in \"version=${project.version}\" format.", be)
		}
	}

    /**
     * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
     * @param directory the directory to start from
     */
    protected Class findScmType(File directory) {
        Class clazz = (Class) directory.list().with {
            delegate.grep('.svn') ? SvnReleasePlugin :
                delegate.grep('.bzr') ? BzrReleasePlugin :
                    delegate.grep('.git') ? GitReleasePlugin :
                        delegate.grep('.hg') ? HgReleasePlugin :
                            null
        }

        if (!clazz && directory.parentFile) {
            clazz = findScmType(directory.parentFile)
        }

        clazz
    }

	/**
	 * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
	 * @param project
	 */
	private BaseScmPlugin applyScmPlugin() {

		def projectPath = project.rootProject.projectDir.canonicalFile

		Class clazz = findScmType(projectPath)

		if (!clazz) {
			throw new GradleException(
					'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
							"[${ projectPath }] or its parent directories.")
		}

		assert BaseScmPlugin.isAssignableFrom(clazz)

        clazz.getConstructor(Project.class).newInstance(project);
	}
}
