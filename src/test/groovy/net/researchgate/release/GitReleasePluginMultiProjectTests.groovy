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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class GitReleasePluginMultiProjectTests extends GitSpecification {

    File settingsFile
    File buildFile
    File propertiesFile
    File localDir

    def setup() {
        final subproject = new File(localGit.repository.getWorkTree(), "subproject")
        subproject.mkdir()

        localDir = localGit.getRepository().getWorkTree()
        settingsFile = new File(localDir, "settings.gradle");
        buildFile = new File(localDir, "build.gradle");
        propertiesFile = new File(localDir, "gradle.properties");
        gitAdd(localGit, '.gitignore') {
            it << '.gradle/'
        }
        gitAdd(localGit, 'settings.gradle') {
            it << """
                rootProject.name = 'release-test'
                include 'subproject'
                """
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
        localGit.push().setForce(true).call()
    }

    def "subproject should work with git beeing in parentProject"() {
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('checkUpdateNeeded')
                .withPluginClasspath()
                .build()
        then:
        noExceptionThrown()
        result.task(':checkUpdateNeeded').outcome == TaskOutcome.SUCCESS
    }

    def '`checkUpdateNeeded` should detect remote changes to pull in subproject'() {
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
        result.output.contains "You have 1 remote change(s) to pull"
        result.task(':checkUpdateNeeded').outcome == TaskOutcome.FAILED
    }
}
