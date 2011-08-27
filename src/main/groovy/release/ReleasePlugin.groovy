package release

import java.util.regex.Matcher
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

/**
 * @author elberry
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin implements Plugin<Project> {
	static final String lineSep = System.getProperty("line.separator")
	static final String inputPrompt = "${lineSep}??>"

	static String prompt(String message, String defaultValue = null) {
		if (defaultValue) {
			return System.console().readLine("${inputPrompt} ${message} [${defaultValue}] ") ?: defaultValue
		}
		return System.console().readLine("${inputPrompt} ${message} ") ?: defaultValue
	}

	static void updateVersionProperty(Project project, def newVersion) {
		File propsFile = new File(project.rootDir, 'gradle.properties')
		Properties gradleProps = new Properties()
		gradleProps.load(propsFile.newReader())
		gradleProps.version = newVersion
		gradleProps.store(propsFile.newWriter(), "Version updated to '${newVersion}', by Gradle release plugin.")
	}

	void apply(Project project) {

		project.convention.plugins.release = new ReleasePluginConvention()

		applyScmPlugin(project)

		project.task('checkSnapshotDependencies') << {
			project.configurations.runtime.allDependencies.each { dependency ->
				if (dependency?.version?.endsWith('SNAPSHOT')) {
					def message = "Snapshot dependency detected: ${dependency.group ?: ''}:${dependency.name}:${dependency.version}"
					if (project.convention.plugins.release.failOnSnapshotDependencies) {
						throw new GradleException(message)
					} else {
						println "WARNING: $message"
					}
				}
			}
		}

		project.task('release', type: GradleBuild) {
			// Release task should perform the following tasks.
			tasks = [
					//  1. Check to see if source is out of date
					//'checkUpdateNeeded',
					//  2. Check to see if source needs to be checked in.
					//'checkCommitNeeded',
					//  3. Check for SNAPSHOT dependencies if required.
					'checkSnapshotDependencies',
					//  4. Build && run Unit tests
					'build',
					//  5. Run any other tasks the user specifies in convention.
					project.convention.plugins.release.requiredTasks,
					//  6. Update Snapshot version if used
					'unSnapshotVersion',
					//  7. Commit Snapshot update (if done)
					'preTagCommit',
					//  8. Create tag of release.
					'createReleaseTag',
					//  9. Update version to next version.
					'updateVersion',
					// 10. Commit version update.
					'commitNewVersion'
			].flatten()
		}

		project.task('unSnapshotVersion') << {
			println 'unSnaphotVersion'
			def version = "${project.version}"
			if (version.contains('-SNAPSHOT')) {
				project.setProperty('usesSnapshot', true)
				version = version.replace('-SNAPSHOT', '')
				project.version = version
				ReleasePlugin.updateVersionProperty(project, version)
			} else {
				project.setProperty('usesSnapshot', false)
			}
		}

		project.task('updateVersion') << {
			println 'updateVersion'
			def version = "${project.version}"
			Map<String, Closure> patterns = project.convention.plugins.release.versionPatterns
			for (Map.Entry<String, Closure> entry: patterns) {
				String pattern = entry.key
				Closure handler = entry.value
				Matcher matcher = version =~ pattern
				if (matcher.matches()) {
					def output = handler.call(project, matcher)
					project.setProperty('releaseTag', output.tag)
					String next = output.next
					if (project.hasProperty('usesSnapshot') && project.usesSnapshot) {
						next = "${next}-SNAPSHOT"
					}
					next = ReleasePlugin.prompt('Enter the next version:', next)
					ReleasePlugin.updateVersionProperty(project, next)
					return;
				}
			}
		}
	}

	/**
	 * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
	 * @param project
	 */
	private void applyScmPlugin(Project project) {
		// apply scm tasks
		for (String name: project.projectDir.list()) {
			switch (name) {
				case '.svn':
					project.apply plugin: SvnReleasePlugin
					return
				case '.bzr':
					project.apply plugin: BzrReleasePlugin
					return
				case '.git':
					project.apply plugin: GitReleasePlugin
					return
				case '.hg':
					project.apply plugin: HgReleasePlugin
					return
			}
		}
	}
}