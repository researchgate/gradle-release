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
import net.researchgate.release.tasks.CommitNewVersion
import net.researchgate.release.tasks.InitScmAdapter
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GitReleasePluginTests extends Specification {

    Project project

    def testDir = new File("build/tmp/test/release")

    File localRepo = new File(testDir, 'GitReleasePluginTestLocal')

    File remoteRepo = new File(testDir, 'GitReleasePluginTestRemote')

    private Executor executor

    def setup() {
        testDir.mkdirs()

        executor = new Executor()

        // create remote repository
        this.executor.exec(['git', 'init', 'GitReleasePluginTestRemote'], failOnStderr: true, directory: testDir, env: [:])
        // suppress errors while pushing
        this.executor.exec(['git', 'config', '--add', 'receive.denyCurrentBranch', 'ignore'], failOnStderr: true, directory: remoteRepo, env: [:])

        this.executor.exec(['git', 'clone', remoteRepo.canonicalPath, 'GitReleasePluginTestLocal'], failOnStderr: false, directory: testDir, env: [:])
        this.executor.exec(['git', 'config', '--add', 'user.name', 'Unit Test'], failOnStderr: true, directory: localRepo, env: [:])
        this.executor.exec(['git', 'config', '--add', 'user.email', 'unit@test'], failOnStderr: true, directory: localRepo, env: [:])

        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localRepo).build()
        project.version = "1.1"
        project.plugins.apply(BasePlugin.class)
        ReleasePlugin releasePlugin = project.plugins.apply(ReleasePlugin.class)

        project.file("somename.txt").withWriter {it << "test"}
        this.executor.exec(['git', 'add', 'somename.txt'], failOnStderr: true, directory: localRepo, env: [:])
        this.executor.exec(['git', 'commit', "-m", "test", 'somename.txt'], failOnStderr: true, directory: localRepo, env: [:])

        releasePlugin.createScmAdapter()

        def props = project.file("gradle.properties")
        props.withWriter { it << "version=${project.version}" }
        this.executor.exec(['git', 'add', 'gradle.properties'], failOnStderr: true, directory: localRepo, env: [:])
    }

    def cleanup() {
        if (testDir.exists()) testDir.deleteDir()
    }

    def 'when requireBranch is configured then throw exception when different branch'() {
        given:
        // Ensure we're on a known branch (master) for this test
        this.executor.exec(['git', 'checkout', '-B', 'master'], failOnStderr: false, directory: localRepo, env: [:])
        
        // Get the actual current branch name
        def currentBranch = this.executor.exec(['git', 'branch', '--show-current'], failOnStderr: false, directory: localRepo, env: [:]).trim()
        
        project.release.git.requireBranch.set('myBranch')
        when:
        (new GitAdapter(project, [:])).init()
        then:
        GradleException ex = thrown()
        ex.message == "Current Git branch is \"${currentBranch}\" and not \"myBranch\"."
    }

    def 'when requireBranch is configured using a regex that matches current branch then don\'t throw exception'() {
        given:
        project.release.git.requireBranch.set(/myBranch|master|main/)
        when:
        (new GitAdapter(project, [:])).init()
        then:
        noExceptionThrown()
    }

    def 'should accept config as closure'() {
        when:
        project.release {
            git {
                requireBranch.set('myBranch')
                pushOptions.set(['--no-verify', '--verbose'])
            }
        }
        then:
        noExceptionThrown()
    }

    def 'should push new version to remote tracking branch by default'() {
        when:
        (project.tasks.commitNewVersion as CommitNewVersion).commitNewVersion()
        executor.exec(['git', 'reset', '--hard', 'HEAD'], failOnStderr: true, directory: remoteRepo, env: [:])
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

    def 'push new version to current branch to remote with same branch name as working'() {
        given:
        executor.exec(['git', 'checkout', '-B', 'myBranch'], failOnStderr: false, directory: localRepo, env: [:])
        when:
        (project.tasks.commitNewVersion as CommitNewVersion).commitNewVersion()
        executor.exec(['git', 'checkout', 'myBranch'], failOnStderr: false, directory: remoteRepo, env: [:])
        executor.exec(['git', 'reset', '--hard', 'HEAD'], failOnStderr: false, directory: remoteRepo, env: [:])
        then:
        remoteRepo.list().any { it == 'gradle.properties' }
    }

    def 'commitOptions are passed through to git command'() {
        given:
        executor.exec(['git', 'checkout', '-B', 'myBranch'], failOnStderr: false, directory: localRepo, env: [:])
        when:
        project.release {
            git {
                requireBranch.set('myBranch')
                commitOptions.set(['-s'])
            }
        }
        (project.tasks.commitNewVersion as CommitNewVersion).commitNewVersion()
        String newestCommit = executor.exec(['git', 'show', 'myBranch', '-s', '--pretty=medium'], failOnStderr: false, directory: remoteRepo, env: [:])
        executor.exec(['git', 'checkout', 'myBranch'], failOnStderr: false, directory: remoteRepo, env: [:])
        executor.exec(['git', 'reset', '--hard', 'HEAD'], failOnStderr: false, directory: remoteRepo, env: [:])
        then:
        newestCommit.contains("Signed-off-by: Unit Test <unit@test>")
    }

    def 'accept empty string to ignore requireBranch'() {
        given:
        executor.exec(['git', 'checkout', '-B', 'myBranch'], failOnStderr: false, directory: localRepo, env: [:])
        when:
        project.release {
            git {
                requireBranch.set('')
            }
        }
        (project.tasks.initScmAdapter as InitScmAdapter).initScmAdapter()
        then:
        (project.tasks.initScmAdapter as InitScmAdapter).getScmAdapter().class.equals(GitAdapter.class)
    }
}
