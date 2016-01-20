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

import net.researchgate.release.cli.Executor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class SvnAdapterTests extends Specification {
    Project project
    SvnAdapter svnAdapter

    def setup() {
        project = ProjectBuilder.builder().withName('ReleasePluginTest').build()
        project.apply plugin: ReleasePlugin
        project.version = '1.0.0'

        svnAdapter = new SvnAdapter(project, [:])
        svnAdapter.executor = Mock(Executor)
        svnAdapter.attributes.svnUrl  = 'svn://server/repo'
        svnAdapter.attributes.svnRev  = 123
        svnAdapter.attributes.svnRoot = '/root'
    }

    def "pin externals - default case"() {
        when:
        svnAdapter.createReleaseTag("my test tag")

        then:
        1 * svnAdapter.executor.exec(_, ['svn', 'copy', 'svn://server/repo@123', '/root/tags/1.0.0', '--parents', '-m', 'my test tag'])
    }

    def "pin externals - disabled"() {
        given:
        project.release.svn.pinExternals = false

        when:
        svnAdapter.createReleaseTag("my test tag")

        then:
        1 * svnAdapter.executor.exec(_, ['svn', 'copy', 'svn://server/repo@123', '/root/tags/1.0.0', '--parents', '-m', 'my test tag'])
    }

    def "pin externals - enabled"() {
        given:
        project.release.svn.pinExternals = true

        when:
        svnAdapter.createReleaseTag("my test tag")

        then:
        1 * svnAdapter.executor.exec(_, ['svn', 'copy', 'svn://server/repo@123', '/root/tags/1.0.0', '--parents', '-m', 'my test tag', '--pin-externals'])
    }
}
