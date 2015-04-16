package net.researchgate.release

import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginCommitNewVersionTests extends GitSpecification {

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
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

	/*
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
    */
}
