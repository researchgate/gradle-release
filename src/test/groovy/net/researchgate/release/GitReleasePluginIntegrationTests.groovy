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
import org.eclipse.jgit.api.ResetCommand
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import static org.eclipse.jgit.lib.Repository.shortenRefName

class GitReleasePluginIntegrationTests extends GitSpecification {

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
            it << """
            rootProject.name = 'release-test'
            """
        }
        String jarVersion = System.properties.get('currentVersion')
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            buildscript{
                repositories{
                    flatDir {
                        dirs '../../../../libs'     }
                    }
                    dependencies {
                        classpath('net.researchgate:gradle-release:$jarVersion')
                    }
                }

                apply plugin: 'base'
                apply plugin: 'net.researchgate.release'
                release {
                    ignoredSnapshotDependencies.set(['net.researchgate:gradle-release'])
                    git {
                        requireBranch.set('master')
                    }
                }
        """
        }

    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'integration test'() {
        given: 'setting project version to 1.1'
        gitAddAndCommit(localGit, "gradle.properties") { it << "version=1.1\n" }
        localGit.push().setForce(true).call()
        when: 'calling release task'
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.0.1')
                .withArguments('release', '-Prelease.useAutomaticVersion=true', '-s')
                .withPluginClasspath()
                .build()
        def st = localGit.status().call()
        gitCheckoutBranch(remoteGit)
        remoteGit.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
        then: 'execution was successful'
        result.tasks.each {it.outcome == TaskOutcome.SUCCESS }
        and: 'project version updated'
        propertiesFile.text == 'version=1.2\n'
        and: 'mo modified files in local repo'
        st.modified.size() == 0
        st.added.size() == 0
        st.changed.size() == 0
        st.uncommittedChanges.size() == 0
        and: 'tag with old version 1.1 created in local repo'
        localGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
        and: 'tag with old version 1.1 pushed to remote repo'
        remoteGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
        and: 'property file updated to new version in local repo'
        new File(localGit.repository.workTree.getAbsolutePath(), 'gradle.properties').text == 'version=1.2\n'
        and: 'property file with new version pushed to remote repo'
        new File(remoteGit.repository.workTree.getAbsolutePath(), 'gradle.properties').text == 'version=1.2\n'
    }
}
