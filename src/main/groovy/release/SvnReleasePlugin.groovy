package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 23:25:18 PDT 2011
 */
class SvnReleasePlugin implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.SvnReleasePlugin = new SvnReleasePluginConvention()

		project.task('checkCommitNeeded') << {
			// svn status
			println 'checkCommitNeeded'
		}
		project.task('checkUpdateNeeded') << {
			// svn status -q -u
			println 'checkUpdateNeeded'
		}
		project.task('commitNewVersion') << {println 'commitNewVersion'}
		project.task('createReleaseTag') << {println 'createReleaseTag'}
		project.task('preTagCommit') << {println 'preTagCommit'}
	}
}