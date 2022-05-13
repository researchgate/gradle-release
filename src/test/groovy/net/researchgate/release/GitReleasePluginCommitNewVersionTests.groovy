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

import org.eclipse.jgit.lib.Constants
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class GitReleasePluginCommitNewVersionTests extends GitSpecification {

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
        gitAdd(localGit, 'build.gradle') {
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
        gitAddAndCommit(localGit, 'gradle.properties') {
            it << 'version=1.1'
        }
        localGit.push().call()
    }

    def cleanup() {
        new File(localDir, 'gradle.properties').delete()
        localGit.commit().setAll(true).setMessage("cleanup").call()
        localGit.push().call()
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'should push new version to remote tracking branch by default'() {
        given:
        new File(localDir, 'gradle.properties').withWriter { it << "version=1.1" }
        assert !remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.1") }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('commitNewVersion')
                .withPluginClasspath()
                .build()
        gitHardReset(remoteGit)
        then: 'remote repo contains updated properties file'
        result.task(':createScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':commitNewVersion').outcome == TaskOutcome.SUCCESS
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.1") }
    }

    def 'should push new version to branch using the branch prefix when it is specified'() {
        given:
        gitAdd(localGit, 'gradle.properties') { it << "version=1.1" }
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    pushToBranchPrefix = 'refs/for/'
                    requireBranch = 'master'
                }
            }
        """
        }
        localGit.push().call()
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('commitNewVersion')
                .withPluginClasspath()
                .build()
        gitCheckoutBranch(remoteGit, "refs/for/$Constants.MASTER")
        then: 'remote repo contains updated properties file'
        result.task(':commitNewVersion').outcome == TaskOutcome.SUCCESS
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.1") }
    }

    def 'should only push the version file to branch when pushVersionFileOnly is true'() {
        given:
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    commitVersionFileOnly = true
                    requireBranch = 'master'
                }
            }
        """
        }
        gitAdd(localGit, 'test.txt') {
            it << 'testTarget'
        }
        assert !remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.1") }
        new File(localDir, 'gradle.properties').withWriter { it << "version=1.1" }
        new File(localDir,'test.txt').withWriter { it << "testTarget" }
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('commitNewVersion')
                .withPluginClasspath()
                .build()
        gitHardReset(remoteGit)
        then: 'remote repo does not get the .gitignore update'
        result.task(':commitNewVersion').outcome == TaskOutcome.SUCCESS
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.1") }
        ! remoteGit.repository.workTree.listFiles().any { it.name == 'test.txt' && it.text.contains('testTarget') }
    }
}
