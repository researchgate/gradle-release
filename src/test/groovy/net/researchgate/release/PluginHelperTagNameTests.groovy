package net.researchgate.release

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
public class PluginHelperTagNameTests extends Specification {

    def testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.apply plugin: TestReleasePlugin
    }

    def 'when no includeProjectNameInTag then tag name is version'() {
        expect:
        tagName() == '1.1'
    }

    def 'when includeProjectNameInTag then tag name starts from project name'() {
        given:
        project.release {
            includeProjectNameInTag = true
        }
        expect:
        tagName() == "$project.name-$project.version"
    }

    def 'when tagPrefix not blank then it added to tag ignoring project name'() {
        given:
        project.release {
            includeProjectNameInTag = includeProjectName
            tagPrefix = 'PREF'
        }
        expect:
        tagName() == 'PREF-1.1'
        where:
        includeProjectName << [true, false]
    }

    def 'when includeHyphenInTag is false and tagPrefix not blank then add it to tag without a hyphen separating it'() {
        given:
        project.release {
            includeProjectNameInTag = includeProjectName
            tagPrefix = 'PREF'
            includeHyphenInTag = false
        }
        expect:
        tagName() == 'PREF1.1'
        where:
        includeProjectName << [true, false]
    }
}
