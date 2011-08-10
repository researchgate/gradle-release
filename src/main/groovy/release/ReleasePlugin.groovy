package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.release = new ReleasePluginConvention()
		// check for scm type
		def scm = 'svn'
		project.projectDir.eachDir {
			switch (it.name) {
				case '.svn':
					scm = 'svn'
					break
				case '.bzr':
					scm = 'bzr'
					break
				case '.git':
					scm = 'git'
					break
				case '.hg':
					scm = 'hg'
					break
			}
		}
		// apply scm tasks

		project.task('validateSource', dependsOn: ['checkUpdateNeeded', 'checkCommitNeeded']) {}

		project.task('release') {
			// Release task should perform the following tasks.
			// 1. Check to see if source is out of date
			// 2. Check to see if source needs to be checked in.
			// 3. Check for SNAPSHOT versions if required.
			// 3. Build && run Unit tests
			// 4. Run any other tasks the user specifies in convention.
			// 5. Update Snapshot version
		}
	}
}