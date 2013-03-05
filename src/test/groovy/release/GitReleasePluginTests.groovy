package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginTests extends GitSpecification {

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
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

    def 'revert should discard uncommited changes to configured *.properties'() {
        given: 'custom properties file modified and not commited'
        project.release {
            versionPropertyFile = 'my.properties'
        }
        gitAddAndCommit(localGit, 'my.properties') { it << 'version=0.0' }
        project.file('my.properties').withWriter { it << 'version=X.X' }
        when:
        project.plugins.findPlugin(GitReleasePlugin).revert()
        then:
        project.file('my.properties').text == 'version=0.0'
    }

}
