package net.researchgate.release

import org.gradle.api.GradleException
import java.util.regex.Matcher

class GitReleasePlugin extends BaseScmPlugin<GitReleasePluginConvention> {

	private static final String LINE = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'

	private static final String UNCOMMITTED = 'uncommitted'
	private static final String UNVERSIONED = 'unversioned'
	private static final String AHEAD = 'ahead'
	private static final String BEHIND = 'behind'

	@Override
	void init() {
		if (convention().requireBranch) {

			def branch = gitCurrentBranch()

			if (!(branch == convention().requireBranch)) {
				throw new GradleException("Current Git branch is \"$branch\" and not \"${ convention().requireBranch }\".")
			}
		}
	}

	@Override
	GitReleasePluginConvention buildConventionInstance() { releaseConvention().git }

	@Override
	void checkCommitNeeded() {

		def status = gitStatus()

		if (status[UNVERSIONED]) {
			warnOrThrow(releaseConvention().failOnUnversionedFiles,
					(['You have unversioned files:', LINE, * status[UNVERSIONED], LINE] as String[]).join('\n'))
		}

		if (status[UNCOMMITTED]) {
			warnOrThrow(releaseConvention().failOnCommitNeeded,
					(['You have uncommitted files:', LINE, * status[UNCOMMITTED], LINE] as String[]).join('\n'))
		}
	}

	@Override
	void checkUpdateNeeded() {
		exec(['git', 'remote', 'update'], '', 'error: ', 'fatal: ')

		def status = gitRemoteStatus()

		if (status[AHEAD]) {
			warnOrThrow(releaseConvention().failOnPublishNeeded, "You have ${status[AHEAD]} local change(s) to push.")
		}

		if (status[BEHIND]) {
			warnOrThrow(releaseConvention().failOnUpdateNeeded, "You have ${status[BEHIND]} remote change(s) to pull.")
		}
	}

	@Override
	void createReleaseTag(String message = "") {
		def tagName = tagName()
		exec(['git', 'tag', '-a', tagName, '-m', message ?: "Created by Release Plugin: ${tagName}"], "Duplicate tag [$tagName]", 'already exists')
        if (shouldPush()) {
            exec(['git', 'push', 'origin', tagName], '', '! [rejected]', 'error: ', 'fatal: ')
        }
	}

	@Override
	void commit(String message) {
		exec(['git', 'commit', '-a', '-m', message], '')
        if (shouldPush()) {
            def branch
            if (convention().pushToCurrentBranch) {
                branch = gitCurrentBranch()
            } else {
                def requireBranch = convention().requireBranch
                log.debug("commit - {requireBranch: ${requireBranch}}")
                if(requireBranch) {
                    branch = requireBranch
                } else {
                    branch = 'master'
                }
            }
            exec(['git', 'push', convention().pushToRemote, branch], 'Failed to push to remote', '! [rejected]', 'error: ', 'fatal: ')
        }
	}

	@Override
	void revert() {
		exec(['git', 'checkout', findPropertiesFile().name], "Error reverting changes made by the release plugin.")
	}

    private boolean shouldPush() {
        def shouldPush = false
        if (convention().pushToRemote) {
            exec('git', 'remote').eachLine { line ->
                Matcher matcher = line =~ ~/^\s*(.*)\s*$/
                if (matcher.matches() && matcher.group(1) == convention().pushToRemote) {
                    shouldPush = true
                }
            }
            if (!shouldPush && convention().pushToRemote != 'origin') {
                throw new GradleException("Could not push to remote ${convention().pushToRemote} as repository has no such remote")
            }
        }

        shouldPush
    }

	private String gitCurrentBranch() {
		def matches = exec('git', 'branch').readLines().grep(~/\s*\*.*/)
		matches[0].trim() - (~/^\*\s+/)
	}

	private Map<String, List<String>> gitStatus() {
		exec('git', 'status', '--porcelain').readLines().groupBy {
			if (it ==~ /^\s*\?{2}.*/) {
				UNVERSIONED
			} else {
				UNCOMMITTED
			}
		}
	}

	private Map<String, Integer> gitRemoteStatus() {
		def branchStatus = exec('git', 'status', '-sb').readLines()[0]
		def aheadMatcher = branchStatus =~ /.*ahead (\d+).*/
		def behindMatcher = branchStatus =~ /.*behind (\d+).*/

		def remoteStatus = [:]

		if (aheadMatcher.matches()) {
			remoteStatus[AHEAD] = aheadMatcher[0][1]
		}
		if (behindMatcher.matches()) {
			remoteStatus[BEHIND] = behindMatcher[0][1]
		}
		remoteStatus
	}
}
