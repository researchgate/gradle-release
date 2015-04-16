package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginCheckUpdateNeededTests extends GitSpecification {
    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()
    }

    def cleanup() {
        localGit.pull().call()
        localGit.push().setForce(true).call()
    }

    def '`checkUpdateNeeded` should detect local changes to push'() {
        given:
        gitAddAndCommit(localGit, 'gradle.properties') { it << '111' }
        when:
        project.checkUpdateNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have 1 local change(s) to push."
    }

    def '`checkUpdateNeeded` should detect remote changes to pull'() {
        given:
        gitAddAndCommit(remoteGit, 'gradle.properties') { it << '222' }
        when:
        project.checkUpdateNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have 1 remote change(s) to pull."
    }
}
