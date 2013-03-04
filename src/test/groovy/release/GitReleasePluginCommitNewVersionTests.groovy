package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginCommitNewVersionTests extends GitSpecification {

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
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

    def 'when commitNewVersion then only properties file pushed'() {
        given:
        gitAdd(localGit, "file1.txt") {it << "content"}
        project.file('gradle.properties').withWriter { it << "version=3.3" }
        when: 'calling task and resetting remote git to get valid state'
        project.commitNewVersion.execute()
        gitHardReset(remoteGit)
        then:
        remoteGit.repository.workTree.listFiles().any {it.name == 'gradle.properties' && it.text.contains("version=3.3") }
        and:
        !remoteGit.repository.workTree.listFiles().any {it.name == 'file1.txt'}
    }

    def 'when commitNewVersion and specific property file is configured then only it affected'() {
        given: 'create configure specific property file'
        project.release {
            versionPropertyFile = 'custom.properties'
        }
        gitAdd(localGit, "custom.properties") {it << "version=4.4"}
        project.file('gradle.properties').withWriter { it << "version=3.3" }
        when: 'calling task and resetting remote git to get valid state'
        project.commitNewVersion.execute()
        gitHardReset(remoteGit)
        then:
        remoteGit.repository.workTree.listFiles().any {it.name == 'custom.properties' && it.text.contains("version=4.4") }
        and:
        !remoteGit.repository.workTree.listFiles().any {it.name == 'gradle.properties' && it.text.contains("version=4.4") }
    }
}
