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

class ManualAdapterTests extends Specification {
    Project project
    ManualAdapter manualAdapter
    ByteArrayOutputStream stdout

    File testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        project = project = ProjectBuilder.builder().withName("ManualAdapterTests").withProjectDir(testDir).build()
        project.apply plugin: ReleasePlugin
        project.version = '1.0.0'

        def props = project.file("gradle.properties")
        props.withWriter {it << "version=${project.version}"}

        manualAdapter = new ManualAdapter(project, [:])
        manualAdapter.executor = Mock(Executor)
        // Reassign stdout so we can perform assertions on it
        stdout = new ByteArrayOutputStream()
        System.out = new PrintStream(stdout)
    }

    def "manual adapter is always supported"() {
        when:
        def supported = manualAdapter.isSupported(null)

        then:
        supported == true
    }

    def "manual adapter checkCommitNeeded - no commit needed"() {
        when:
        manualAdapter.checkCommitNeeded()

        then:
        stdout.toString().contains('Have all modified files been committed?')
    }

    def "manual adapter checkUpdateNeeded - no update needed"() {
        when:
        manualAdapter.checkUpdateNeeded()

        then:
        stdout.toString().contains('Have all remote modifications been pulled')
    }

    def "manual adapter commit - no automatic commits"() {
        when:
        manualAdapter.commit('test message')

        then:
        stdout.toString().contains('You should now commit the changes. Suggested commit message: [test message]. Did you commit the changes?')
    }

    def "manual adapter revert"() {
        when:
        manualAdapter.revert()

        then:
        stdout.toString().contains('You should revert the changes to [gradle.properties]')
    }
}
