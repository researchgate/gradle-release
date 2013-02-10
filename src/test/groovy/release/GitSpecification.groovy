package release

import org.eclipse.jgit.api.Git
import org.junit.Before
import spock.lang.Shared
import spock.lang.Specification

abstract class GitSpecification extends Specification {

    @Shared File testDir = new File("build/tmp/test/${getClass().simpleName}")

    @Shared File localRepo = new File(testDir, "local")
    @Shared File remoteRepo = new File(testDir, "remote")

    @Shared Git localGit
    @Shared Git remoteGit

    protected void newFileToRemote(String name, String content) {
        new File(remoteRepo, name).withWriter {it << content}
        remoteGit.add().addFilepattern(name).call()
        remoteGit.commit().setAll(true).setMessage("commit $name").call()
    }

    def setupSpec() {
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        remoteGit = Git.init().setDirectory(remoteRepo).call()
        remoteGit.repository.config.setString("receive", null, "denyCurrentBranch", "ignore")
        remoteGit.repository.config.save()

        newFileToRemote('gradle.properties', 'version=0.0')

        localGit = Git.cloneRepository().setDirectory(localRepo).setURI(remoteRepo.canonicalPath).call()
    }

    def cleanupSpec() {
        localGit.repository.lockDirCache().unlock()
        localGit.repository.close()
        if (testDir.exists()) testDir.deleteDir()
    }
}
