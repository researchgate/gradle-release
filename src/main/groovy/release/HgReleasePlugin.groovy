package release

import org.gradle.api.GradleException

/**
 * @author elberry
 * @author evgenyg
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends BaseScmPlugin {

	private static final String ERROR = "abort:"

	void init() {
		setConvention('HgReleasePlugin', new HgReleasePluginConvention())
	}


	void checkCommitNeeded() {
		def modifications = ['A': [], 'M': [], 'R': [], '?': []]
		exec('hg', 'status').eachLine {line ->
			def mods = modifications[line[0]]
			if (mods) mods << line
		}
		if (modifications['?']) {
			def message = "You have ${modifications['?'].size()} un-versioned files."
			if (releaseConvention().failOnUnversionedFiles) {
				throw new GradleException(message)
			} else {
				log.warn(message)
			}
		}
		if (modifications.count { k, v -> v }) {
			def c = { count, label ->
				count ? "$count $label" : ""
			}
			def message = "You have " + c(modifications["A"].size(), "added") + c(modifications["M"].size(), "modified") +
					c(modifications["R"].size(), "removed")
			if (releaseConvention().failOnCommitNeeded) {
				throw new GradleException(message)
			} else {
				log.warn(message)
			}
		}
	}

	void checkUpdateNeeded() {
		def modifications = ["in": [], "out": []]
		exec("hg", "in", "-q").eachLine { line ->
			modifications["in"] << line
		}
		exec("hg", "out", "-q").eachLine { line ->
			modifications["out"] << line
		}
		if (modifications["in"]) {
			def message = "You have ${modifications["in"].size()} incoming changes"
			if (releaseConvention().failOnUpdateNeeded) {
				throw new GradleException(message)
			} else {
				log.warn(message)
			}
		}
		if (modifications["out"]) {
			def message = "You have ${modifications["out"].size()} outgoing changes"
			if (releaseConvention().failOnPublishNeeded) {
				throw new GradleException(message)
			} else {
				log.warn(message)
			}
		}
	}

	void createReleaseTag() {
		exec(['hg', 'tag', project.properties.version], 'Error creating tag', ERROR)
	}

	void commit(String message) {
		exec(['hg', 'ci', '-m', message], 'Error committing new version', ERROR)
		exec(['hg', 'push'], 'Error committing new version', ERROR)
	}
}