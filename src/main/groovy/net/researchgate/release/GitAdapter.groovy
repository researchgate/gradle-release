/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Matcher

class GitAdapter extends BaseScmAdapter {

    private static final String LINE = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'

    private static final String UNCOMMITTED = 'uncommitted'
    private static final String UNVERSIONED = 'unversioned'
    private static final String AHEAD = 'ahead'
    private static final String BEHIND = 'behind'

    class GitConfig {
        String requireBranch = 'master'
        def pushToRemote = 'origin' // needs to be def as can be boolean or string
        boolean pushToCurrentBranch = false
    }

    GitAdapter(Project project) {
        super(project)
    }

    @Override
    Object createNewConfig() {
        return new GitConfig()
    }

    @Override
    boolean isSupported(File directory) {
        if (!directory.list().grep('.git')) {
            return directory.parentFile? isSupported(directory.parentFile) : false
        }

        true
    }

    @Override
    void init() {
        if (extension.git.requireBranch) {
            def branch = gitCurrentBranch()
            if (!(branch ==~ extension.git.requireBranch)) {
                throw new GradleException("Current Git branch is \"$branch\" and not \"${ extension.git.requireBranch }\".")
            }
        }
    }

    @Override
    void checkCommitNeeded() {

        def status = gitStatus()

        if (status[UNVERSIONED]) {
            warnOrThrow(extension.failOnUnversionedFiles,
                    (['You have unversioned files:', LINE, * status[UNVERSIONED], LINE] as String[]).join('\n'))
        }

        if (status[UNCOMMITTED]) {
            warnOrThrow(extension.failOnCommitNeeded,
                    (['You have uncommitted files:', LINE, * status[UNCOMMITTED], LINE] as String[]).join('\n'))
        }
    }

    @Override
    void checkUpdateNeeded() {
        exec(['git', 'remote', 'update'], errorPatterns: ['error: ', 'fatal: '])

        def status = gitRemoteStatus()

        if (status[AHEAD]) {
            warnOrThrow(extension.failOnPublishNeeded, "You have ${status[AHEAD]} local change(s) to push.")
        }

        if (status[BEHIND]) {
            warnOrThrow(extension.failOnUpdateNeeded, "You have ${status[BEHIND]} remote change(s) to pull.")
        }
    }

    @Override
    void createReleaseTag(String message) {
        def tagName = tagName()
        exec(['git', 'tag', '-a', tagName, '-m', message], errorMessage: "Duplicate tag [$tagName]", errorPatterns: ['already exists'])
        if (shouldPush()) {
            exec(['git', 'push', '--porcelain', 'origin', tagName], errorMessage: "Failed to push tag [$tagName] to remote", errorPatterns: ['[rejected]', 'error: ', 'fatal: '])
        }
    }

    @Override
    void commit(String message) {
        exec(['git', 'commit', '-a', '-m', message])
        if (shouldPush()) {
            def branch
            if (extension.git.pushToCurrentBranch) {
                branch = gitCurrentBranch()
            } else {
                branch = extension.git.requireBranch ? extension.git.requireBranch : 'master'
            }
            exec(['git', 'push', '--porcelain', extension.git.pushToRemote, branch], errorMessage: 'Failed to push to remote', errorPatterns: ['[rejected]', 'error: ', 'fatal: '])
        }
    }

    @Override
    void revert() {
        exec(['git', 'checkout', findPropertiesFile().name], errorMessage: 'Error reverting changes made by the release plugin.')
    }
	
	@Override
	String assignReleaseVersionAutomatically(String currentVersion) {
		def lastCommitMessage = exec(['git', 'log', '-1', '--pretty=%B']).readLines()[0]
		def latestTag = getLatestTag()
		def version = new SemanticVersion(currentVersion)
		if (latestTag != null) {
			version = new SemanticVersion(latestTag)
		}
		
		def newVersion = null
		if (lastCommitMessage.contains('feature-')) {
			newVersion = version.newFeature()
		} else if (lastCommitMessage.contains('patch-')) {
			newVersion = version.newPatch()
		} else if (lastCommitMessage.contains('major-')) {
			newVersion = version.newMajor()
		} else {
			throw new GradleException("Could not assign release version automatically because " +  
				"there is no information on last commit message to identify whether the change is major, feature or patch." +
				"Last commit message should contain one of these keywords: major-, feature-, patch- in order to version automatically")
		}
		return newVersion.toString()
	}
	
	private String getLatestTag() {
		def latestTag = exec(['git', 'describe', '--abbrev=0']).readLines()[0]
		return latestTag
	}

    private boolean shouldPush() {
        def shouldPush = false
        if (extension.git.pushToRemote) {
            exec(['git', 'remote']).eachLine { line ->
                Matcher matcher = line =~ ~/^\s*(.*)\s*$/
                if (matcher.matches() && matcher.group(1) == extension.git.pushToRemote) {
                    shouldPush = true
                }
            }
            if (!shouldPush && extension.git.pushToRemote != 'origin') {
                throw new GradleException("Could not push to remote ${extension.git.pushToRemote} as repository has no such remote")
            }
        }

        shouldPush
    }

    private String gitCurrentBranch() {
        def matches = exec(['git', 'branch']).readLines().grep(~/\s*\*.*/)
        matches[0].trim() - (~/^\*\s+/)
    }

    private Map<String, List<String>> gitStatus() {
        exec(['git', 'status', '--porcelain']).readLines().groupBy {
            if (it ==~ /^\s*\?{2}.*/) {
                UNVERSIONED
            } else {
                UNCOMMITTED
            }
        }
    }

    private Map<String, Integer> gitRemoteStatus() {
        def branchStatus = exec(['git', 'status', '-sb']).readLines()[0]
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
