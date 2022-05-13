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
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

public class PluginHelperTagNameTests extends Specification {

    Project project

    PluginHelper helper

    def testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").withProjectDir(testDir).build()
        project.version = '1.1'
        project.plugins.apply(BasePlugin.class)
        ReleasePlugin releasePlugin = project.plugins.apply(ReleasePlugin.class)
        project.extensions.release.scmAdapters = [TestAdapter]

        releasePlugin.createScmAdapter()

        helper = new PluginHelper(project: project, extension: project.extensions['release'] as ReleaseExtension)
    }

    def 'by default the tag name is version'() {
        expect:
        helper.tagName() == '1.1'
    }

    def 'when tagTemplate contains name and version then it is processed in the tag name'() {
        given:
        (project.extensions.release as ReleaseExtension).tagTemplate.set('PREF-$name-$version')
        expect:
        helper.tagName() == 'PREF-ReleasePluginTest-1.1'
    }
}
