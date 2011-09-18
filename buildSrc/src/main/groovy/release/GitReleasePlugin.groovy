package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends PluginHelper implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.GitReleasePlugin = new GitReleasePluginConvention()

		project.task('checkCommitNeeded') << {println 'checkCommitNeeded'}
		project.task('checkUpdateNeeded') << { println 'checkUpdateNeeded'}
		project.task('commitNewVersion') << {println 'commitNewVersion'}
		project.task('createReleaseTag') << {println 'createReleaseTag'}
		project.task('preTagCommit') << {println 'preTagCommit'}
	}
}