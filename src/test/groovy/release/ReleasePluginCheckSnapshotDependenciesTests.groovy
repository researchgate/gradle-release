package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


public class ReleasePluginCheckSnapshotDependenciesTests extends Specification {
    def project

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").build()
        project.apply plugin: 'groovy'
        project.apply plugin: TestReleasePlugin
    }

    def 'test smth'() {
        when:
        def a = 1
        then:
        a == 1
    }
}
