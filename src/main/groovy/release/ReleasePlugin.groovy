package release

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

/**
 * @author elberry
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin implements Plugin<Project> {
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
					'checkCommitNeeded',
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

		project.task('unSnapshotVersion') << { println 'unSnaphotVersion' }

		project.task('updateVersion') << { println 'updateVersion' }
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