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

@Mixin(PluginHelper)
class GitReleasePluginCheckCommitNeededTests extends GitSpecification {
    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.getWorkTree()).build()
        project.apply plugin: ReleasePlugin
        project.findScmPlugin.execute()
    }

    def cleanup() {
        project.fileTree('.').matching { include: '*.txt' }.each { it.delete() }
    }

    def '`checkCommitNeeded` should detect untracked files'() {
        given:
        project.file('untracked.txt').withWriter { it << "untracked" }
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have unversioned files"
        ex.cause.message.contains "untracked.txt"
    }

    def '`checkCommitNeeded` should detect added files'() {
        given:
        gitAdd(localGit, 'added.txt') { it << 'added' }
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "added.txt"
    }

    def '`checkCommitNeeded` should detect changed files'() {
        given:
        gitAddAndCommit(localGit, 'changed.txt') { it << 'changed1' }
        project.file("changed.txt").withWriter { it << "changed2" }
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "changed.txt"
    }

    def '`checkCommitNeeded` should detect modified files'() {
        given:
        gitAddAndCommit(localGit, 'modified.txt') { it << 'modified1' }
        gitAdd(localGit, 'modified.txt') { it << 'modified2' }
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "modified.txt"
    }
}
