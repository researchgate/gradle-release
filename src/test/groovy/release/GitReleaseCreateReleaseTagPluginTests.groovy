package release

import org.eclipse.jgit.api.Git
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleaseCreateReleaseTagPluginTests extends Specification {

    def testDir = new File("build/tmp/test/release")

    def localRepo = new File(testDir, "GitReleasePluginTestLocal")
    def remoteRepo = new File(testDir, "GitReleasePluginTestRemote")

    def setup() {
        testDir.mkdirs()

        exec(true, [:], testDir, 'git', 'init', "GitReleasePluginTestRemote")//create remote repo
        exec(true, [:], remoteRepo, 'git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore')//suppress errors when pushing

        new File(remoteRepo, "gradle.properties").withWriter { it << "version=0.0" }
        exec(true, [:], remoteRepo, 'git', 'add', 'gradle.properties')
        exec(true, [:], remoteRepo, 'git', 'commit', '-a', '-m', 'initial')

        exec(false, [:], testDir, 'git', 'clone', remoteRepo.canonicalPath, 'GitReleasePluginTestLocal')

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'when createReleaseTag then tag is created'() {
        given:
        asserty Git.open(localRepo).tagList().call().size() == 0
        when:
        project.createReleaseTag.execute()
        then:
        asserty Git.open(localRepo).tagList().call().size() == 1
        true
    }

    def 'when createReleaseTag then tag is pushed to remote'() {
        given:
        when:
        project.createReleaseTag.execute()
        then:
        true
    }

    def 'when createReleaseTag and tag already exist then exception'() {
        given:
        when:
        project.createReleaseTag.execute()
        then:
        true
    }

}
