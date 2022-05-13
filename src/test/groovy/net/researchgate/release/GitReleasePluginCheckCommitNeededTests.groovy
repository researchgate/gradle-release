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


import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class GitReleasePluginCheckCommitNeededTests extends GitSpecification {

    File settingsFile
    File buildFile
    File propertiesFile
    File localDir

    def setup() {
        localDir = localGit.getRepository().getWorkTree()
        settingsFile = new File(localDir, "settings.gradle");
        buildFile = new File(localDir, "build.gradle");
        propertiesFile = new File(localDir, "gradle.properties");
        gitAdd(localGit, '.gitignore') {
            it << '.gradle/'
        }
        gitAdd(localGit, 'settings.gradle') {
            it << "rootProject.name = 'release-test'"
        }
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    requireBranch = 'master'
                }
            }
        """
        }
    }

    def cleanup() {
        new ProjectBuilder().withProjectDir(localDir).build().fileTree('.').matching { include: '*.txt' }.each { it.delete() }
    }

    def '`checkCommitNeeded` should detect untracked files'() {
        given:
        new File(localDir, 'untracked.txt').withWriter { it << "untracked" }

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(localDir)
            .withGradleVersion('6.9.2')
            .withArguments('checkCommitNeeded')
            .withPluginClasspath()
            .buildAndFail()

        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkCommitNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have unversioned files"
        result.output.contains "untracked.txt"
    }

    def '`checkCommitNeeded` should detect added files'() {
        given:
        gitAdd(localGit, 'added.txt') { it << 'added' }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkCommitNeeded')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkCommitNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have uncommitted files"
        result.output.contains "added.txt"
    }

    def '`checkCommitNeeded` should detect changed files'() {
        given:
        gitAddAndCommit(localGit, 'changed.txt') { it << 'changed1' }
        new File(localDir, "changed.txt").withWriter { it << "changed2" }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkCommitNeeded')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkCommitNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have uncommitted files"
        result.output.contains "changed.txt"
    }

    def '`checkCommitNeeded` should detect modified files'() {
        given:
        gitAddAndCommit(localGit, 'modified.txt') { it << 'modified1' }
        gitAdd(localGit, 'modified.txt') { it << 'modified2' }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkCommitNeeded')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkCommitNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have uncommitted files"
        result.output.contains "modified.txt"
    }
}
