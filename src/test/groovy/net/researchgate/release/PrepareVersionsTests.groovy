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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PrepareVersionsTests extends Specification {

    Project project
    Project subproject1
    Project subproject2

    PrepareVersions prepareVersionsTask

    File testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()

        File subProject1Dir = new File(project.getRootDir(), "subproject1")
        subProject1Dir.mkdir()
        subproject1 = ProjectBuilder.builder().withName("subproject1").withProjectDir(subProject1Dir).withParent(project).build()
        subproject1.version = '1.0'
        subproject1.file("gradle.properties").withWriter {
            it << "version=1.0\n"
        }

        File subProject2Dir = new File(project.getRootDir(), "subproject2")
        subProject2Dir.mkdir()
        subproject2 = ProjectBuilder.builder().withName("subproject2").withProjectDir(subProject2Dir).withParent(project).build()
        subproject2.version = '2.0'
        subproject2.file("gradle.properties").withWriter {
            it << "version=2.0\n"
        }

        project.apply plugin: ReleasePlugin
        project.release.scmAdapters = [TestAdapter]

        prepareVersionsTask = project.task('prepareVersionsTask', type: PrepareVersions)
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'A root level properties file should not exist if useMultipleVersionFiles=true'() {
        when:
        project.file("gradle.properties").withWriter {
            it << "version=1.0\n"
        }
        project.release {
            useMultipleVersionFiles = true
        }
        prepareVersionsTask.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "'useMultipleVersionFiles' config is set to true"
    }

}
