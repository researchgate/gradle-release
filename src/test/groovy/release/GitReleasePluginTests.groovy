package release

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginTests extends Specification {

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
        def props = project.file("gradle.properties")
        props.withWriter { it << "version=${project.version}" }
        exec(true, [:], localRepo, 'git', 'add', 'gradle.properties')
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'should apply ReleasePlugin and GitReleasePlugin plugin'() {
        expect:
        project.plugins.findPlugin(ReleasePlugin)
        and:
        project.plugins.findPlugin(GitReleasePlugin)
    }

    def 'should push new version to remote tracking branch by default'() {
        when:
        project.commitNewVersion.execute()
        exec(true, [:], remoteRepo, 'git', 'reset', '--hard', 'HEAD')
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

    def 'when pushToCurrentBranch then push new version to remote branch with same name as working'() {
        given:
        project.git.pushToCurrentBranch = true
        exec(true, [:], localRepo, 'git', 'checkout', '-B', 'myBranch')
        when:
        project.commitNewVersion.execute()
        exec(false, [:], remoteRepo, 'git', 'checkout', 'myBranch')
        exec(false, [:], remoteRepo, 'git', 'reset', '--hard', 'HEAD')
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

}
