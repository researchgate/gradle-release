package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginCheckUpdateNeededTests extends Specification {

    def testDir = new File("build/tmp/test/release")

    def localRepo = new File(testDir, "GitReleasePluginTestLocal")
    def remoteRepo = new File(testDir, "GitReleasePluginTestRemote")

    def setup() {
        testDir.mkdirs()

        exec(true, [:], testDir, 'git', 'init', "GitReleasePluginTestRemote")//create remote repo
        exec(true, [:], remoteRepo, 'git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore')//suppress errors when pushing

        new File(remoteRepo, "test.txt").withWriter {it << "000"}
        exec(true, [:], remoteRepo, 'git', 'add', 'test.txt')
        exec(true, [:], remoteRepo, 'git', 'commit', "-m", "test", 'test.txt')

        exec(false, [:], testDir, 'git', 'clone', remoteRepo.canonicalPath, 'GitReleasePluginTestLocal')

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def '`checkUpdateNeeded` should detect local changes to push'() {
        given:
        project.file("test.txt").withWriter {it << "111"}
        exec(true, [:], localRepo, 'git', 'commit', '-a', "-m", "111")
        when:
        project.checkUpdateNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have 1 local change(s) to push."
    }

    def '`checkUpdateNeeded` should detect remote changes to pull'() {
        given:
        new File(remoteRepo, "test.txt").withWriter {it << "222"}
        exec(true, [:], remoteRepo, 'git', 'commit', '-a', "-m", "222")
        when:
        project.checkUpdateNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have 1 remote change(s) to pull."
    }
}
