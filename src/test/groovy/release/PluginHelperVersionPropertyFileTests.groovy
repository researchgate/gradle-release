package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Ignore


public class PluginHelperVersionPropertyFileTests extends Specification {
    def project

    PluginHelper helper

    def testDir = new File("build/tmp/test/release/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.apply plugin: TestReleasePlugin
        helper = new PluginHelper(project: project)

        def props = project.file("gradle.properties")
        props.withWriter {it << "version=${project.version}"}
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

    def 'by default should update `version` property from props file'() {
        given:
        helper.updateVersionProperty("2.2")
        expect:
        project.file("gradle.properties").readLines()[0] == 'version=2.2'
    }

    def 'when configured then update `version` and additional properties from props file'() {
        given:
        def props = project.file("custom.properties")
        props.withWriter {
            it << "version=${project.version}\nversion1=${project.version}\nversion2=${project.version}\n"
        }
        project.release {
            versionPropertyFile = 'custom.properties'
            versionProperties = ['version1']
        }
        when:
        helper.updateVersionProperty("2.2")
        def lines = project.file("custom.properties").readLines()
        then:
        lines[0] == 'version=2.2'
        lines[1] == 'version1=2.2'
        lines[2] == 'version2=1.1'
    }

    def 'should update version of project and subprojects'() {
        given:
        def proj1 = ProjectBuilder.builder().withParent(project).withName("proj1").build()
        proj1.version = project.version
        def proj2 = ProjectBuilder.builder().withParent(project).withName("proj2").build()
        proj2.version = project.version
        when:
        helper.updateVersionProperty("2.2")
        then:
        assert project.version == '2.2'
        assert proj1.version == project.version
        assert proj2.version == project.version
    }

}
