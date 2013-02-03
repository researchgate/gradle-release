package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginCheckCommitNeededTests extends Specification {

    def testDir = new File("build/tmp/test/release")

    def localRepo = new File(testDir, "GitReleasePluginTestLocal")
    def remoteRepo = new File(testDir, "GitReleasePluginTestRemote")

    def setup() {
        testDir.mkdirs()

        exec(true, [:], testDir, 'git', 'init', "GitReleasePluginTestRemote")//create remote repo
        exec(true, [:], remoteRepo, 'git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore')//suppress errors when pushing

        exec(false, [:], testDir, 'git', 'clone', remoteRepo.canonicalPath, 'GitReleasePluginTestLocal')

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin

        project.file("test.txt").withWriter {it << "test"}
        exec(true, [:], localRepo, 'git', 'add', 'test.txt')
        exec(true, [:], localRepo, 'git', 'commit', "-m", "test", 'test.txt')

        def props = project.file("gradle.properties")
        props.withWriter { it << "version=${project.version}" }
        exec(true, [:], localRepo, 'git', 'add', 'gradle.properties')
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def '`checkCommitNeeded` should detect untracked files'() {
        given:
        project.file('untracked.txt').withWriter {it << "test"}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have unversioned files"
        ex.cause.message.contains "untracked.txt"
    }

    def '`checkCommitNeeded` should detect added files'() {
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "gradle.properties"
    }

    def '`checkCommitNeeded` should detect changed files'() {
        given:
        project.file("test.txt").withWriter {it << "update test"}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "test.txt"
    }

    def '`checkCommitNeeded` should detect modified files'() {
        given:
        project.file("test.txt").withWriter {it << "update test"}
        exec(true, [:], localRepo, 'git', 'add', 'test.txt')
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "test.txt"
    }
}
