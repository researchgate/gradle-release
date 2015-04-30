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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ReleasePluginTests extends Specification {

    Project project

    def testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName('ReleasePluginTest').withProjectDir(testDir).build()
        def testVersionPropertyFile = project.file('version.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.2'
        }
        project.apply plugin: TestReleasePlugin
        project.findScmPlugin.execute()
    }

    def 'plugin is successfully applied'() {
        expect:
        assert project.tasks.release

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
