package net.researchgate.release

import org.gradle.api.GradleException
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
        exec(true, [:], localRepo, 'git', 'config', '--add', 'user.name', 'Unit Test')
        exec(true, [:], localRepo, 'git', 'config', '--add', 'user.email', 'unit@test')

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()

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

    def 'should apply ReleasePlugin and GitReleasePlugin plugin'() {
        expect:
        project.plugins.findPlugin(ReleasePlugin)
        and:
        project.plugins.findPlugin(GitReleasePlugin)
    }

    def 'when requireBranch is configured then throw exception when different branch'() {
        given:
        project.git.requireBranch = 'myBranch'
        when:
        project.plugins.findPlugin(GitReleasePlugin).init()
        then:
        GradleException ex = thrown()
        ex.message == 'Current Git branch is "master" and not "myBranch".'

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
        exec(false, [:], localRepo, 'git', 'checkout', '-B', 'myBranch')
        when:
        project.commitNewVersion.execute()
        exec(false, [:], remoteRepo, 'git', 'checkout', 'myBranch')
        exec(false, [:], remoteRepo, 'git', 'reset', '--hard', 'HEAD')
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

}
