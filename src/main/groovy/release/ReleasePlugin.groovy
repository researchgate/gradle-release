package release

import java.util.regex.Matcher
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.GradleBuild

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin extends PluginHelper implements Plugin<Project> {

	@SuppressWarnings('StatelessClass')
	private BaseScmPlugin scmPlugin

	void apply(Project project) {

		this.project = project

		setConvention('release', new ReleasePluginConvention())
		this.scmPlugin = applyScmPlugin()

		project.task('release', description: 'Verify project, release, and update version to next.', type: GradleBuild) {
			tasks = [
					//  0. (This Plugin) Initializes the corresponding SCM plugin (Git/Bazaar/Svn/Mercurial).
					'initScmPlugin',
					//  1. (SCM Plugin) Check to see if source needs to be checked in.
					'checkCommitNeeded',
					//  2. (SCM Plugin) Check to see if source is out of date
					'checkUpdateNeeded',
					//  3. (This Plugin) Check for SNAPSHOT dependencies if required.
					'checkSnapshotDependencies',
					//  4. (This Plugin) Build && run Unit tests
					'build',
					//  5. (This Plugin) Update Snapshot version if used
					'unSnapshotVersion',
					//  6. (This Plugin) Commit Snapshot update (if done)
					'preTagCommit',
					//  7. (SCM Plugin) Create tag of release.
					'createReleaseTag',
					//  8. (This Plugin) Update version to next version.
					'updateVersion',
					//  9. (This Plugin) Commit version update.
					'commitNewVersion'
			].flatten()
		}

		project.task('initScmPlugin',
				description: 'Initializes the SCM plugin (based on hidden directories in your project\'s directory)') << this.&initScmPlugin
		project.task('checkSnapshotDependencies',
				description: 'Checks to see if your project has any SNAPSHOT dependencies.') << this.&checkSnapshotDependencies
		project.task('unSnapshotVersion',
				description: 'Removes "-SNAPSHOT" from your project\'s current version.') << this.&unSnapshotVersion
		project.task('preTagCommit',
				description: 'Commits any changes made by the Release plugin - eg. If the unSnapshotVersion tas was executed') << this.&preTagCommit
		project.task('updateVersion',
				description: 'Prompts user for the next version. Does it\'s best to supply a smart default.') << this.&updateVersion
		project.task('commitNewVersion',
				description: 'Commits the version update to your SCM') << this.&commitNewVersion
	}


	void initScmPlugin() {

		checkPropertiesFile()
		scmPlugin.setConvention()
		scmPlugin.init()

		// Verifying all "release" steps are defined
		// TODO: Eric says - Not sure if this is actually necessary. The Base SCM Plugin requires the methods be
		//       implemented and adds the tasks accordingly. So unless someone doesn't extend BaseSCMPlugin, there's no
		//       real chance of the tasks not being there.
		//Set<String> allTasks = project.tasks*.name
		//assert ((GradleBuild) project.tasks['release']).tasks.every { allTasks.contains(it) }
	}


	void checkSnapshotDependencies() {

		def matcher = { Dependency d -> d.version?.contains('SNAPSHOT') }
		def collector = { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

		// get the snapshot dependencies on the root project
		def rootSnapshotDependencies = project.configurations.findByName('runtime')?.allDependencies?.
				matching(matcher)?.collect(collector)

		def snapshotDependencies = ["${project.name}": rootSnapshotDependencies ?: []]

		// get the snapshot dependencies on any sub projects
		project.subprojects?.each { Project subProject ->
			def subSnapshotDependencies = subProject.configurations.findByName('runtime')?.allDependencies?.
					matching(matcher)?.collect(collector)
			snapshotDependencies["${subProject.name}"] = subSnapshotDependencies ?: []
		}

		if (!snapshotDependencies.values()*.empty) {
			snapshotDependencies.each { pName, dList ->
				if (dList.empty) {
					snapshotDependencies.remove(pName)
				}
			}
			def message = "Snapshot dependencies detected: $snapshotDependencies"
			warnOrThrow(releaseConvention().failOnSnapshotDependencies, message)
		}
	}


	void unSnapshotVersion() {
		def version = project.version.toString()

		if (version.contains('-SNAPSHOT')) {
			project.setProperty('usesSnapshot', true)
			version -= '-SNAPSHOT'
			updateVersionProperty(version)
		}
		else {
			project.setProperty('usesSnapshot', false)
		}
	}


	void preTagCommit() {
		if (project.properties['usesSnapshot']) {
			// should only be committed if the project was using a snapshot version.
			scmPlugin.commit(releaseConvention().preTagCommitMessage + " '${ project.version }'.")
		}
	}


	void updateVersion() {
		def version = project.version.toString()
		Map<String, Closure> patterns = releaseConvention().versionPatterns

		for (entry in patterns) {

			String pattern = entry.key
			//noinspection GroovyUnusedAssignment
			Closure handler = entry.value
			Matcher matcher = version =~ pattern

			if (matcher.find()) {
				String nextVersion = handler(matcher)
				if (project.properties['usesSnapshot']) {
					nextVersion += '-SNAPSHOT'
				}
				updateVersionProperty(readLine("Enter the next version (current one released as [$version]):", nextVersion))
				return
			}
		}

		throw new GradleException("Failed to increase version [$version] - unknown pattern")
	}


	def commitNewVersion() {
		scmPlugin.commit(releaseConvention().newVersionCommitMessage + " '${ project.version }'.")
	}


	def checkPropertiesFile() {

		File propertiesFile = findPropertiesFile()

		Properties properties = new Properties()
		propertiesFile.withReader { properties.load(it) }

		assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
		assert releaseConvention().versionPatterns.keySet().any { (properties.version =~ it).find() },            \
                          "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
				releaseConvention().versionPatterns.keySet()
	}

	/**
	 * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
	 * @param project
	 */
	private BaseScmPlugin applyScmPlugin() {

		def projectPath = project.rootProject.projectDir.canonicalFile

		Class c = findScmType(projectPath)

		if (!c) {
			throw new GradleException(
					'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
							"[${ projectPath }] or its parent directories.")
		}

		assert BaseScmPlugin.isAssignableFrom(c)

		project.apply plugin: c
		project.plugins.findPlugin(c)
	}

	/**
	 * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
	 * @param directory the directory to start from
	 */
	private Class findScmType(File directory) {

		Class c = (Class) directory.list().with {
			delegate.grep('.svn') ? SvnReleasePlugin :
				delegate.grep('.bzr') ? BzrReleasePlugin :
					delegate.grep('.git') ? GitReleasePlugin :
						delegate.grep('.hg') ? HgReleasePlugin :
							null
		}

		if (!c && directory.parentFile) {
			c = findScmType(directory.parentFile)
		}
		
		c	
	}

}