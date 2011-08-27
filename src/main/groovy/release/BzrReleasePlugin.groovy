package release

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 23:26:04 PDT 2011
 */
class BzrReleasePlugin implements Plugin<Project> {
	void apply(Project project) {
		checkForXmlOutput()
		project.convention.plugins.BzrReleasePlugin = new BzrReleasePluginConvention()
		project.task('checkCommitNeeded') << {
			StringBuilder out = new StringBuilder()
			StringBuilder err = new StringBuilder()
			Process process = ['bzr', 'xmlstatus'].execute()
			process.waitForProcessOutput(out, err)
			def xml = new XmlSlurper().parseText("$out")
			def added = xml.added?.size() ?: 0
			def modified = xml.modified?.size() ?: 0
			def removed = xml.removed?.size() ?: 0
			def unknown = xml.unknown?.size() ?: 0
			if (added || modified || removed) {
				throw new GradleException("You have un-committed changes.")
			}
			if (project.convention.plugins.release.failOnUnversionedFiles && unknown) {
				throw new GradleException("You have un-versioned files.")
			}
		}
		project.task('checkUpdateNeeded') << {
			StringBuilder out = new StringBuilder()
			StringBuilder err = new StringBuilder()
			Process process = ['bzr', 'xmlmissing'].execute()
			process.waitForProcessOutput(out, err)
			def xml = new XmlSlurper().parseText("$out")
			def extra = "${xml.extra_revisions?.@size}" ?: 0
			def missing = "${xml.missing_revisions?.@size}" ?: 0
			if (extra) {
				throw new GradleException("You have $extra unpublished change${extra > 1 ? 's' : ''}.")
			}
			if (missing) {
				throw new GradleException("You are missing $missing changes.")
			}
		}
		project.task('commitNewVersion') << {
			println 'createReleaseTag'

			String newVersionCommitMessage = project.convention.plugins.release.newVersionCommitMessage

			StringBuilder out = new StringBuilder()
			StringBuilder err = new StringBuilder()
			Process process = ['bzr', 'ci', '-m', newVersionCommitMessage].execute()
			process.waitForProcessOutput(out, err)
			if ("${out}".contains("ERROR") || "${err}".contains("ERROR")) {
				throw new GradleException("Error committing new version - ${out}${err}")
			}
			if ("${err}".contains("ERROR")) {
				throw new GradleException("Error committing new version - ${err}")
			}
			/*
			out = new StringBuilder()
			err = new StringBuilder()
			process = ['bzr', 'push', ':parent'].execute()
			process.waitForProcessOutput(out, err)

			if ("${out}".contains("ERROR") || "${err}".contains("ERROR")) {
				StringBuilder _out = new StringBuilder()
				StringBuilder _err = new StringBuilder()
				process = ['bzr', 'push', ':parent'].execute()
				process.waitForProcessOutput(out, err)
				throw new GradleException("Error committing new version - ${out}${err}")
			}
			*/
		}
		project.task('createReleaseTag') << {
			println 'createReleaseTag'
			String newVersionCommitMessage = project.convention.plugins.release.newVersionCommitMessage
			String tag = project.hasProperty('releaseTag') ? project.releaseTag : new Date().format('yyyymmdd_hh')

			StringBuilder out = new StringBuilder()
			StringBuilder err = new StringBuilder()
			Process process = ['bzr', 'tag', tag, newVersionCommitMessage].execute()
			process.waitForProcessOutput(out, err)
			if ("${out}".contains("ERROR") || "${err}".contains("ERROR")) {
				throw new GradleException("Error committing new version - ${out}${err}")
			}
			if ("${err}".contains("ERROR")) {
				throw new GradleException("Error committing new version - ${err}")
			}
		}
		project.task('preTagCommit') << {println 'preTagCommit'}
	}

	private void checkForXmlOutput() {
		StringBuilder out = new StringBuilder()
		StringBuilder err = new StringBuilder()
		Process process = ['bzr', 'plugins'].execute()
		process.waitForProcessOutput(out, err)
		boolean hasXmlOutput = false
		"$out".eachLine {
			if (it.startsWith('xmloutput')) {
				hasXmlOutput = true
			}
		}
		if (!hasXmlOutput) {
			throw new IllegalStateException("The required xmloutput plugin is not installed in Bazaar, please install it.")
		}
	}
}