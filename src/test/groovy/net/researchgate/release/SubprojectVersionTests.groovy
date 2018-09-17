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

import net.researchgate.release.tasks.PrepareVersions
import net.researchgate.release.tasks.UpdateVersion
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class SubprojectVersionTests extends Specification {

    Project project
    Project subproject1
    Project subproject2

    UpdateVersion updateVersionTask

    File testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        testDir.mkdirs()
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()

        File subProject1Dir = new File(project.getRootDir(), "subproject1")
        subProject1Dir.mkdir()
        subproject1 = ProjectBuilder.builder().withName("subproject1").withProjectDir(subProject1Dir).withParent(project).build()
        subproject1.version = '1.0'
        new File(subproject1.projectDir, "gradle.properties").withWriter {
            it << "version=1.0\n"
        }

        File subProject2Dir = new File(project.getRootDir(), "subproject2")
        subProject2Dir.mkdir()
        subproject2 = ProjectBuilder.builder().withName("subproject2").withProjectDir(subProject2Dir).withParent(project).build()
        subproject2.version = '2.0'
        new File(subproject2.projectDir, "gradle.properties").withWriter {
            it << "version=2.0\n"
        }

        project.apply plugin: ReleasePlugin
        project.release.scmAdapters = [TestAdapter]

        updateVersionTask = project.task('updateVersionTask', type: UpdateVersion) as UpdateVersion
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'Subproject versioning should work with automatic versioning'() {
        when:
        project.release {
            useMultipleVersionFiles = true
        }
        project.ext.set('Prelease.useAutomaticVersion', true)

        subproject1.task('subProjectVersionTask1', type: UpdateVersion).execute()
        def subProject1VersionLines = subproject1.file("gradle.properties").readLines()
        subproject2.task('subProjectVersionTask2', type: UpdateVersion).execute()
        def subProject2VersionLines = subproject2.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        subProject1VersionLines[0] == 'version=1.1'
        subProject2VersionLines[0] == 'version=2.1'
    }

    def 'A skipped subproject should not have its version updated'() {
        given:
        project.release {
            skipProjectRelease = { Project project ->
                project == subproject1
            }
            useMultipleVersionFiles = true
        }
        project.ext.set('Prelease.useAutomaticVersion', true)
        when:
        subproject1.task('subProjectVersionTask1', type: UpdateVersion).execute()
        def subProject1VersionLines = subproject1.file("gradle.properties").readLines()
        subproject2.task('subProjectVersionTask2', type: UpdateVersion).execute()
        def subProject2VersionLines = subproject2.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        subProject1VersionLines[0] == 'version=1.0'
        subProject2VersionLines[0] == 'version=2.1'
    }

    def 'Explicit release version should be used for a subproject'() {
        given:
        project.release {
            useMultipleVersionFiles = true
        }
        project.ext.set('release.subproject1.newVersion', '4.0')
        project.ext.set('Prelease.useAutomaticVersion', true)
        when:
        subproject1.task('subProjectVersionTask1', type: UpdateVersion).execute()
        def subProject1VersionLines = subproject1.file("gradle.properties").readLines()
        subproject2.task('subProjectVersionTask2', type: UpdateVersion).execute()
        def subProject2VersionLines = subproject2.file("gradle.properties").readLines()
        then:
        noExceptionThrown()
        subProject1VersionLines[0] == 'version=4.0'
        subProject2VersionLines[0] == 'version=2.1'
    }
}
