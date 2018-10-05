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

import net.researchgate.release.tasks.CreateReleaseTag
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TagNameTests extends Specification {

    Project project

    CreateReleaseTag createReleaseTagTask

    def testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.apply plugin: ReleasePlugin
        project.release.scmAdapters = [TestAdapter]

        createReleaseTagTask = project.task('createReleaseTagTask', type: CreateReleaseTag) as CreateReleaseTag
    }

    def 'by default tag name is version'() {
        expect:
        createReleaseTagTask.tagName(project) == '1.1'
    }

    def 'when tagTemplate not blank then it is used as tag name'() {
        given:
        project.release {
            tagTemplate = 'PREF-$name-$version'
        }
        expect:
        createReleaseTagTask.tagName(project) == 'PREF-ReleasePluginTest-1.1'
    }
}
