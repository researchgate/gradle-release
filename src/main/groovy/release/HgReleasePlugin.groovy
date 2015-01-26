package release

/**
 * @author elberry
 * @author evgenyg
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends BaseScmPlugin<HgReleasePluginConvention> {

	private static final String ERROR = 'abort:'

	@Override
	void init() {
	}

	@Override
	HgReleasePluginConvention buildConventionInstance() { new HgReleasePluginConvention() }

	@Override
	void checkCommitNeeded() {
		def modifications = ['A': [], 'M': [], 'R': [], '?': []]
		exec('hg', 'status').eachLine {line ->
			def mods = modifications[line[0]]
			if (mods != null) { mods << line }
		}
		if (modifications['?']) {
			warnOrThrow(releaseConvention().failOnUnversionedFiles, "You have ${modifications['?'].size()} un-versioned files.")
		}
		if (modifications.count { k, v -> v }) {
			def c = { count, label ->
				count ? "$count $label" : ''
			}
			def message = 'You have ' + c(modifications['A'].size(), 'added') + c(modifications['M'].size(), 'modified') +
					c(modifications['R'].size(), 'removed')
			warnOrThrow(releaseConvention().failOnCommitNeeded, message)
		}
	}

	@Override
	void checkUpdateNeeded() {
		def modifications = ['in': [], 'out': []]
		exec('hg', 'in', '-q', '-b', hgCurrentBranch()).eachLine { line ->
			modifications['in'] << line
		}
		exec('hg', 'out', '-q', '-b', hgCurrentBranch()).eachLine { line ->
			modifications['out'] << line
		}
		if (modifications['in']) {
			warnOrThrow(releaseConvention().failOnUpdateNeeded, "You have ${modifications['in'].size()} incoming changes")
		}
		if (modifications['out']) {
			warnOrThrow(releaseConvention().failOnPublishNeeded, "You have ${modifications['out'].size()} outgoing changes")
		}
	}

	@Override
	void createReleaseTag(String message = "") {
		def tagName = tagName()
		exec(['hg', 'tag', "-m", message ?: "Created by Release Plugin: ${tagName}", tagName], 'Error creating tag', ERROR)
	}

	@Override
	void commit(String message) {
		exec(['hg', 'ci', '-m', message], 'Error committing new version', ERROR)
		exec(['hg', 'push'], 'Error committing new version', ERROR)
	}

	void revert() {
		exec(['hg', 'revert', findPropertiesFile().name], 'Error reverting changes made by the release plugin.', ERROR)
	}

  private String hgCurrentBranch() {
    exec('hg', 'branch').readLines()[0]
  }
}
