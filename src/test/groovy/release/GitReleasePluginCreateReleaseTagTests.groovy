package release

import org.eclipse.jgit.api.Git
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginCreateReleaseTagTests extends Specification {

    static def testDir

    @Shared def localRepo
    @Shared def remoteRepo

    @Shared Git localGit
    @Shared Git remoteGit

    def setupSpec() {
        testDir = new File("build/tmp/test/release/${getClass().simpleName}")
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        localRepo = new File(testDir, "local")
        remoteRepo = new File(testDir, "remote")

        remoteGit = Git.init().setDirectory(remoteRepo).call()
        remoteGit.repository.config.setString("receive", null, "denyCurrentBranch", "ignore")
        remoteGit.repository.config.save()

        new File(remoteRepo, "gradle.properties").withWriter { it << "version=0.0" }
        remoteGit.add().addFilepattern("gradle.properties").call()
        remoteGit.commit().setAll(true).setMessage("initial").call()

        localGit = Git.cloneRepository().setDirectory(localRepo).setURI(remoteRepo.canonicalPath).call()
    }

    def cleanupSpec() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.apply plugin: ReleasePlugin
    }

    def 'createReleaseTag should create tag and push to remote'() {
        given:
        project.version = '1.1'
        when:
        project.createReleaseTag.execute()
        then:
        localGit.tagList().call()*.name == ["refs/tags/${tagName()}"]
        remoteGit.tagList().call()*.name == ["refs/tags/${tagName()}"]
    }

    def 'createReleaseTag should throw exception when tag exist'() {
        given:
        project.version = '1.2'
        localGit.tag().setName(tagName()).call()
        when:
        project.createReleaseTag.execute()
        then:
        thrown GradleException
    }

}
