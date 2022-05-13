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
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class GitReleasePluginCheckUpdateNeededTests extends GitSpecification {

    File settingsFile
    File buildFile
    File propertiesFile
    File localDir

    def setup() {
        localDir = localGit.getRepository().getWorkTree()
        settingsFile = new File(localDir, "settings.gradle");
        buildFile = new File(localDir, "build.gradle");
        propertiesFile = new File(localDir, "gradle.properties");
        gitAddAndCommit(localGit, '.gitignore') {
            it << '.gradle/'
        }

        gitAddAndCommit(localGit, 'settings.gradle') {
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
        localGit.push().call()
    }

    def cleanup() {
        localGit.pull().call()
        localGit.push().setForce(true).call()
    }

    def '`checkUpdateNeeded` should detect local changes to push'() {
        given:
        gitAddAndCommit(localGit, 'gradle.properties') { it << '111' }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkUpdateNeeded')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkUpdateNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have 1 local change(s) to push."
    }

    def '`checkUpdateNeeded` should detect remote changes to pull'() {
        given:
        gitAddAndCommit(remoteGit, 'gradle.properties') { it << '222' }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkUpdateNeeded')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':checkUpdateNeeded').outcome == TaskOutcome.FAILED
        result.output.contains "You have 1 remote change(s) to pull."
    }
}
