package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends PluginHelper implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.HgReleasePlugin = new HgReleasePluginConvention()

		project.task('checkCommitNeeded') << {println 'checkCommitNeeded'}
		project.task('checkUpdateNeeded') << { println 'checkUpdateNeeded'}
		project.task('commitNewVersion') << {println 'commitNewVersion'}
		project.task('createReleaseTag') << {println 'createReleaseTag'}
		project.task('preTagCommit') << {println 'preTagCommit'}
	}
}