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

    def setupSpec() {
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        remoteGit = Git.init().setDirectory(remoteRepo).call()
        remoteGit.repository.config.setString("receive", null, "denyCurrentBranch", "ignore")
        remoteGit.repository.config.save()

        new File(remoteRepo, "gradle.properties").withWriter { it << "version=0.0" }
        remoteGit.add().addFilepattern("gradle.properties").call()
        remoteGit.commit().setAll(true).setMessage("initial").call()

        localGit = Git.cloneRepository().setDirectory(localRepo).setURI(remoteRepo.canonicalPath).call()
    }

    def cleanupSpec() {
        localGit.repository.lockDirCache().unlock()
        localGit.repository.close()
        if (testDir.exists()) testDir.deleteDir()
    }
}
