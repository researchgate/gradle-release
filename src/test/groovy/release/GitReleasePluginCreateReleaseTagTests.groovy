package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginCreateReleaseTagTests extends GitSpecification {

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.apply plugin: ReleasePlugin
    }

    def 'createReleaseTag should create tag and push to remote'() {
        given:
        project.version = '1.1'
        when:
        project.createReleaseTag.execute()
        then:
        localGit.tagList().call()*.name == ["refs/tags/${tagName()}"]
        remoteGit.tagList().call()*.name == ["refs/tags/${tagName()}"]
    }

    def 'createReleaseTag should throw exception when tag exist'() {
        given:
        project.version = '1.2'
        localGit.tag().setName(tagName()).call()
        when:
        project.createReleaseTag.execute()
        then:
        thrown GradleException
    }

}
