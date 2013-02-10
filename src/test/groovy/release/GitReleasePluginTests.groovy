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

    def 'should push new version to remote tracking branch by default'() {
        given:
        project.file('gradle.properties').withWriter { it << "version=${project.version}" }
        when:
        project.commitNewVersion.execute()
        gitHardReset(remoteGit)
        then: 'remote repo contains updated properties file'
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=$project.version") }
    }

    def 'when tracking branch missing then push new version to remote branch with same name as local'() {
        given:
        gitCheckoutBranch(localGit, 'myBranch', true)
        project.file('gradle.properties').withWriter { it << "version=2.2" }
        when:
        project.commitNewVersion.execute()
        gitCheckoutBranch(remoteGit, 'myBranch')
        gitHardReset(remoteGit)
        then:
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=2.2") }
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
