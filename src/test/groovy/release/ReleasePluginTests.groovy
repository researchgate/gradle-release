package release

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class ReleasePluginTests extends Specification {

    def testDir = new File("build/tmp/test/${getClass().simpleName}")



    def setup() {
        project = ProjectBuilder.builder().withName('ReleasePluginTest').withProjectDir(testDir).build()
        def testVersionPropertyFile = project.file('version.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.2'
        }
        project.apply plugin: TestReleasePlugin
    }

    def 'when a custom properties file is used to specify the version'() {
        given:
        project.release {
            versionPropertyFile = 'version.properties'
        }
        project.initScmPlugin.execute()
        expect:
        project.version == '1.2'

    }

}
