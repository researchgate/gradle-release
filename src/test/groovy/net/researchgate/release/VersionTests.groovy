/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release


import net.researchgate.release.tasks.InitScmAdapter
import net.researchgate.release.tasks.PrepareVersions
import net.researchgate.release.tasks.UpdateVersion
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class VersionTests extends Specification {

    Project project

    PrepareVersions prepareVersionsTask
    UpdateVersion updateVersionTask

    File testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        testDir.mkdirs()
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.apply plugin: ReleasePlugin
        project.release.scmAdapters = [TestAdapter]

        def props = new File(project.projectDir, "gradle.properties")
        props.withWriter {it << "version=${project.version}"}

        prepareVersionsTask = project.task('prepareVersionsTask', type: PrepareVersions) as PrepareVersions
        updateVersionTask = project.task('updateVersionTask', type: UpdateVersion) as UpdateVersion
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'should find gradle.properties by default'() {
        given:
        prepareVersionsTask.execute()
        expect:
        prepareVersionsTask.findPropertiesFile(project).name == 'gradle.properties'
    }

    def 'should find properties from convention'() {
        given:
        def props = project.file("custom.properties")
        props.withWriter {it << '@@@@'}
        project.release {
            versionPropertyFile = 'custom.properties'
        }
        prepareVersionsTask.execute()
        expect:
        prepareVersionsTask.findPropertiesFile(project).name == 'custom.properties'
    }

    def 'by default should update `version` property from props file'() {
        given:
        updateVersionTask.execute()
        expect:
        project.file("gradle.properties").readLines()[0] == 'version=1.2'
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
        updateVersionTask.execute()
        def lines = project.file("custom.properties").readLines()
        then:
        lines[0] == 'version=1.2'
        lines[1] == 'version1=1.2'
        lines[2] == 'version2=1.1'
    }

    def 'should update version of project and subprojects'() {
        given:
        def proj1 = ProjectBuilder.builder().withParent(project).withName("proj1").build()
        proj1.version = project.version
        def proj2 = ProjectBuilder.builder().withParent(project).withName("proj2").build()
        proj2.version = project.version
        when:
        updateVersionTask.execute()
        then:
        assert project.version == '1.2'
        assert proj1.version == project.version
        assert proj2.version == project.version
    }

    def 'should not fail when version contains spaces'() {
        given:
        project.version = '2.5'
        def props = project.file("gradle.properties")
        props.withWriter {
            it << "version = ${project.version}\n"
            it << "version1 : ${project.version}\n"
            it << "version2   ${project.version}\n"
        }
        project.release {
            versionProperties = ['version1', 'version2']
        }
        project.createScmAdapter.execute()
        when:
        project.task('task', type: InitScmAdapter).execute()
        updateVersionTask.execute()
        def lines = project.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        lines[0] == 'version = 2.6'
        lines[1] == 'version1 : 2.6'
        lines[2] == 'version2   2.6'
    }

    def 'should not escape other stuff'() {
        given:
        project.version = '3.0'
        def props = project.file("gradle.properties")
        props.withWriter {
            it << "version=${project.version}\n"
            it << "something=http://www.gradle.org/test\n"
            it << "  another.prop.version =  1.1\n"
        }
        project.createScmAdapter.execute()
        when:
        project.task('task', type: InitScmAdapter).execute()
        updateVersionTask.execute()
        def lines = project.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        lines[0] == 'version=3.1'
        lines[1] == 'something=http://www.gradle.org/test'
        lines[2] == '  another.prop.version =  1.1'
    }

    def 'should not fail on other property separators'() {
        given:
        project.version = '3.2'
        def props = project.file("gradle.properties")
        props.withWriter {
            it << "version:${project.version}\n"
            it << "version1=${project.version}\n"
            it << "version2 ${project.version}\n"
        }
        project.release {
            versionProperties = ['version1', 'version2']
        }
        when:
        updateVersionTask.execute()
        def lines = project.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        lines[0] == 'version:3.3'
        lines[1] == 'version1=3.3'
        lines[2] == 'version2 3.3'
    }
}
