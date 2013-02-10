package release

import org.eclipse.jgit.api.Git
import spock.lang.Shared
import spock.lang.Specification

abstract class GitSpecification extends Specification {

    @Shared File testDir = new File("build/tmp/test/${getClass().simpleName}")

    @Shared File localRepo = new File(testDir, "local")
    @Shared File remoteRepo = new File(testDir, "remote")

    @Shared Git localGit
    @Shared Git remoteGit

    static void addToGit(Git git, String name, Closure content) {
        new File(git.repository.getWorkTree(), name).withWriter content
        git.add().addFilepattern(name).call()
    }

    static void addAndCommitToGit(Git git, String name, Closure content) {
        addToGit(git, name, content)
        git.commit().setAll(true).setMessage("commit $name").call()
    }

    def setupSpec() {
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        remoteGit = Git.init().setDirectory(remoteRepo).call()
        remoteGit.repository.config.setString("receive", null, "denyCurrentBranch", "ignore")
        remoteGit.repository.config.save()

        addAndCommitToGit(remoteGit, 'gradle.properties') {
            it << 'version=0.0'
        }

        localGit = Git.cloneRepository().setDirectory(localRepo).setURI(remoteRepo.canonicalPath).call()
    }

    def cleanupSpec() {
        localGit.repository.lockDirCache().unlock()
        localGit.repository.close()
        if (testDir.exists()) testDir.deleteDir()
    }
}
