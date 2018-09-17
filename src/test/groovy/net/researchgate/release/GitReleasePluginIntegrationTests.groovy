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

import org.eclipse.jgit.api.Status
import org.gradle.testkit.runner.GradleRunner

import static org.eclipse.jgit.lib.Repository.shortenRefName

class GitReleasePluginIntegrationTests extends GitSpecification {

    File projectDir

    def setup() {
        projectDir = localGit.repository.getWorkTree()

        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)

        gitAddAndCommit(localGit, "settings.gradle") { it << """
            rootProject.name = 'GitReleasePluginTest'
        """ }
        gitAddAndCommit(localGit, "build.gradle") { it << """
             buildscript {
                repositories {
                    flatDir {
                        dirs '${new File('./build/tmp/testJars/').absolutePath}'
                    }
                }
                dependencies {
                    classpath 'net.researchgate:gradle-release:0.0.0'
                }
            }
             apply plugin: 'java'
            apply plugin: 'net.researchgate.release'
        """ }
        gitAddAndCommit(localGit, '.gitignore') { it << ".gradle/"}
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'integration test'() {
        given: 'setting project version to 1.1'
        new File(projectDir, 'gradle.properties').text == 'version=1.1'
        gitAddAndCommit(localGit, "gradle.properties") { it << "version=1.1" }
        localGit.push().setForce(true).call()
        when: 'calling release task indirectly'
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments('release', '-Prelease.useAutomaticVersion = true')
                .withPluginClasspath()
                .build()

        Status st = localGit.status().call()
        gitHardReset(remoteGit)
        then: 'project version updated'
        new File(projectDir, 'gradle.properties').text == 'version=1.2'
        and: 'mo modified files in local repo'
        st.modified.size() == 0 && st.added.size() == 0 && st.changed.size() == 0
        and: 'tag with old version 1.1 created in local repo'
        localGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
        and: 'property file updated to new version in local repo'
        localGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.2") }
        and: 'property file with new version pushed to remote repo'
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.2") }
        and: 'tag with old version 1.1 pushed to remote repo'
        remoteGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
    }
}
