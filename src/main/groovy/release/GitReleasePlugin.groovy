package release

import org.gradle.api.GradleException

/**
 * @author elberry
 * @author evgenyg
 * @author szpak
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
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

		gitExec(['remote', 'update'], '')

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
		gitExec(['tag', '-a', tagName, '-m', message ?: "Created by Release Plugin: ${tagName}"], "Duplicate tag [$tagName]", 'already exists')
		gitExec(['push', 'origin', tagName], '', '! [rejected]', 'error: ', 'fatal: ')
	}


	@Override
	void commit(String message) {
		gitExec(['commit', '-a', '-m', message], '')
		def pushCmd = ['push', 'origin']
		if (convention().pushToCurrentBranch) {
			pushCmd << gitCurrentBranch()
		} else {
			def requireBranch = convention().requireBranch
			log.debug("commit - {requireBranch: ${requireBranch}}")
			if(requireBranch != null) {
				pushCmd << requireBranch
			} else {
				pushCmd << 'master'
			}
		}
		gitExec(pushCmd, '', '! [rejected]', 'error: ', 'fatal: ')
	}

	@Override
	void revert() {
		gitExec(['checkout', findPropertiesFile().name], "Error reverting changes made by the release plugin.")
	}



	private String gitCurrentBranch() {
		def matches = gitExec('branch').readLines().grep(~/\s*\*.*/)
		matches[0].trim() - (~/^\*\s+/)
	}

	private Map<String, List<String>> gitStatus() {
		gitExec('status', '--porcelain').readLines().groupBy {
			if (it ==~ /^\s*\?{2}.*/) {
				UNVERSIONED
			} else {
				UNCOMMITTED
			}
		}
	}

	private Map<String, Integer> gitRemoteStatus() {
		def branchStatus = gitExec('status', '-sb').readLines()[0]
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

	String gitExec(Collection<String> params, String errorMessage, String... errorPattern) {
		def gitDir = resolveGitDir()
		def workTree = resolveWorkTree()
		def cmdLine = ['git', "--git-dir=${gitDir}", "--work-tree=${workTree}"].plus(params)
		log.debug("gitExec - 1 - {cmdLine: ${cmdLine}, errorMessage: ${errorMessage}, errorPattern: ${errorPattern}}")
		return exec(cmdLine, errorMessage, errorPattern)
	}

	private String resolveGitDir() {
		if (convention().scmRootDir) {
			project.rootProject.file(convention().scmRootDir + "/.git").canonicalPath.replaceAll("\\\\", "/")
		} else {
			project.rootProject.file(".git").canonicalPath.replaceAll("\\\\", "/")
		}
	}

	private String resolveWorkTree() {
		if (convention().scmRootDir) {
			project.rootProject.file(convention().scmRootDir).canonicalPath.replaceAll("\\\\", "/")
		} else {
			project.rootProject.projectDir.canonicalPath.replaceAll("\\\\", "/")
		}
	}

	String gitExec(String... commands) {
		def gitDir = resolveGitDir()
		def workTree = resolveWorkTree()
		def cmdLine = ['git', "--git-dir=${gitDir}", "--work-tree=${workTree}"]
		cmdLine.addAll commands
		log.debug("gitExec - 2 - {cmdLine: ${cmdLine}}")
		return exec(cmdLine as String[])
	}
}
