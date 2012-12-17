package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Ignore


public class PluginHelperVersionPropertyFileTests extends Specification {
    def project

    PluginHelper helper

    def testDir = new File("build/tmp/test/release")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.apply plugin: TestReleasePlugin
        helper = new PluginHelper(project: project)

        def props = project.file("gradle.properties")
        props.withWriter {it << '@@@@'}

    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'should find gradle.properties by default'() {
        expect:
        helper.findPropertiesFile().name == 'gradle.properties'
    }

    def 'should find properties from convention'() {
        given:
        def props = project.file("custom.properties")
        props.withWriter {it << '@@@@'}
        project.release {
            versionPropertyFile = 'custom.properties'
        }
        expect:
        helper.findPropertiesFile().name == 'custom.properties'
    }

}
