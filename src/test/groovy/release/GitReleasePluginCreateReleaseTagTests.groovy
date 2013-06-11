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
        //TODO: MZA: It won't work with parallel tests
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

    def 'createReleaseTag with disabled pushing changes should only create tag and not push to remote'() {
        given:
        project.version = '1.3'
        project.release {
            pushChanges = false
        }
        when:
        project.createReleaseTag.execute()
        then:
        localGit.tagList().call().findAll { it.name == "refs/tags/${tagName()}" }.size() == 1
        remoteGit.tagList().call().findAll { it.name == "refs/tags/${tagName()}" }.isEmpty()
    }
}
