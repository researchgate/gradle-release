package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification
import sun.misc.SharedSecrets

@Mixin(PluginHelper)
class GitReleasePluginCheckUpdateNeededTests extends Specification {

    static def testDir = new File("build/tmp/test/release/${getClass().simpleName}")

    @Shared def localRepo = new File(testDir, "local")
    @Shared def remoteRepo = new File(testDir, "remote")

    def setupSpec() {
        if (testDir.exists()) testDir.deleteDir()
        testDir.mkdirs()

        exec(true, [:], testDir, 'git', 'init', "remote")//create remote repo
        exec(true, [:], remoteRepo, 'git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore')//suppress errors when pushing

        new File(remoteRepo, "test.txt").withWriter {it << "000"}
        exec(true, [:], remoteRepo, 'git', 'add', 'test.txt')
        exec(true, [:], remoteRepo, 'git', 'commit', "-m", "test", 'test.txt')

        exec(false, [:], testDir, 'git', 'clone', remoteRepo.canonicalPath, 'local')
    }

    def cleanupSpec() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        exec(false, [:], localRepo, 'git', 'pull')
        exec(false, [:], localRepo, 'git', 'push', '-f')
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
