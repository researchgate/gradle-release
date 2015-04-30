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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GitReleasePluginCommitNewVersionTests extends GitSpecification {

    Project project

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'should push new version to remote tracking branch by default'() {
        given:
        project.file('gradle.properties').withWriter { it << "version=${project.version}" }
        when:
        project.commitNewVersion.execute()
        gitHardReset(remoteGit)
        then: 'remote repo contains updated properties file'
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=$project.version") }
    }
}
