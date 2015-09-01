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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.StoredConfig
import spock.lang.Shared
import spock.lang.Specification

import static org.eclipse.jgit.api.ResetCommand.ResetType.HARD
import static org.eclipse.jgit.lib.Constants.HEAD
import static org.eclipse.jgit.lib.Constants.MASTER

abstract class GitSpecification extends Specification {

    @Shared File testDir = new File("build/tmp/test/${getClass().simpleName}")

    @Shared Git localGit
    @Shared Git remoteGit

    static void gitAdd(Git git, String name, Closure content) {
        new File(git.repository.getWorkTree(), name).withWriter content
        git.add().addFilepattern(name).call()
    }

    static void gitAddAndCommit(Git git, String name, Closure content) {
        gitAdd(git, name, content)
        git.commit().setAll(true).setMessage("commit $name").call()
    }
	
	static void gitCommit(Git git, String commitMessage) {
		git.commit().setAll(true).setMessage(commitMessage).call()
	}

    static void gitHardReset(Git git) {
        git.reset().setMode(HARD).setRef(HEAD).call()
    }

    static void gitCheckoutBranch(Git git, String branchName = MASTER, boolean createBranch = false) {
        git.checkout().setName(branchName).setCreateBranch(createBranch).setForce(true).call()
    }

    def setupSpec() {
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        File remoteRepo = new File(testDir, "remote")
        File localRepo = new File(testDir, "local")

        remoteGit = Git.init().setDirectory(remoteRepo).call()
        remoteGit.repository.config.setString("receive", null, "denyCurrentBranch", "ignore")
        remoteGit.repository.config.save()

        gitAddAndCommit(remoteGit, 'gradle.properties') {
            it << 'version=0.0'
        }

        localGit = Git.cloneRepository().setDirectory(localRepo).setURI(remoteRepo.canonicalPath).call()
        StoredConfig config = localGit.getRepository().getConfig();
        config.setString("user", null, "name", "Unit Test");
        config.setString("user", null, "email", "unit@test");
        config.save();
    }

    def cleanupSpec() {
        localGit.repository.lockDirCache().unlock()
        localGit.repository.close()
        if (testDir.exists()) testDir.deleteDir()
    }
}
