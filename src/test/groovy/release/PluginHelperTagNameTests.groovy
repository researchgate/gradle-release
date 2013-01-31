package release

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

public class PluginHelperTagNameTests extends Specification {
    def project

    PluginHelper helper

    def testDir = new File("build/tmp/test/release")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.apply plugin: TestReleasePlugin
        helper = new PluginHelper(project: project)
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'when no includeProjectNameInTag then tag name is version'() {
        expect:
        helper.tagName() == '1.1'
    }

    def 'when includeProjectNameInTag then tag name starts from project name'() {
        given:
        project.release {
            includeProjectNameInTag = true
        }
        expect:
        helper.tagName() == 'ReleasePluginTest-1.1'
    }

    def 'when tagPrefix not blank then it added to tag ignoring project name'() {
        given:
        project.release {
            includeProjectNameInTag = includeProjectName
            tagPrefix = 'PREF'
        }
        expect:
        helper.tagName() == 'PREF-1.1'
        where:
        includeProjectName << [true, false]
    }
}
