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

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginTests extends Specification {

    def testDir = new File("build/tmp/test/release")

    File localRepo = new File(testDir, 'GitReleasePluginTestLocal')
    File remoteRepo = new File(testDir, 'GitReleasePluginTestRemote')

    def setup() {
        testDir.mkdirs()

        // create remote repository
        exec(['git', 'init', 'GitReleasePluginTestRemote'], failOnStderr: true, directory: testDir, env: [:])
        // suppress errors while pushing
        exec(['git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore'], failOnStderr: true, directory: remoteRepo, env: [:])

        exec(['git', 'clone', remoteRepo.canonicalPath, 'GitReleasePluginTestLocal'], failOnStderr: false, directory: testDir, env: [:])
        exec(['git', 'config', '--add', 'user.name', 'Unit Test'], failOnStderr: true, directory: localRepo, env: [:])
        exec(['git', 'config', '--add', 'user.email', 'unit@test'], failOnStderr: true, directory: localRepo, env: [:])

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()

        project.file("test.txt").withWriter {it << "test"}
        exec(['git', 'add', 'test.txt'], failOnStderr: true, directory: localRepo, env: [:])
        exec(['git', 'commit', "-m", "test", 'test.txt'], failOnStderr: true, directory: localRepo, env: [:])

        def props = project.file("gradle.properties")
        props.withWriter { it << "version=${project.version}" }
        exec(['git', 'add', 'gradle.properties'], failOnStderr: true, directory: localRepo, env: [:])

    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'should apply ReleasePlugin and GitReleasePlugin plugin'() {
        expect:
        project.plugins.findPlugin(ReleasePlugin)
        and:
        project.plugins.findPlugin(GitReleasePlugin)
    }

    def 'when requireBranch is configured then throw exception when different branch'() {
        given:
        project.git.requireBranch = 'myBranch'
        when:
        project.plugins.findPlugin(GitReleasePlugin).init()
        then:
        GradleException ex = thrown()
        ex.message == 'Current Git branch is "master" and not "myBranch".'

    }

    def 'should push new version to remote tracking branch by default'() {
        when:
        project.commitNewVersion.execute()
        exec(['git', 'reset', '--hard', 'HEAD'], failOnStderr: true, directory: remoteRepo, env: [:])
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

    def 'when pushToCurrentBranch then push new version to remote branch with same name as working'() {
        given:
        project.git.pushToCurrentBranch = true
        exec(['git', 'checkout', '-B', 'myBranch'], failOnStderr: false, directory: localRepo, env: [:])
        when:
        project.commitNewVersion.execute()
        exec(['git', 'checkout', 'myBranch'], failOnStderr: false, directory: remoteRepo, env: [:])
        exec(['git', 'reset', '--hard', 'HEAD'], failOnStderr: false, directory: remoteRepo, env: [:])
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }
}
