package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

@Mixin(PluginHelper)
class GitReleasePluginCheckCommitNeededTests extends GitSpecification {
    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.getWorkTree()).build()
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        project.fileTree('.').matching {include: '*.txt'}.each {it.delete()}
    }

    def '`checkCommitNeeded` should detect untracked files'() {
        given:
        project.file('untracked.txt').withWriter {it << "untracked"}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have unversioned files"
        ex.cause.message.contains "untracked.txt"
    }

    def '`checkCommitNeeded` should detect added files'() {
        given:
        addToGit(localGit, 'added.txt') {it << 'added'}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "added.txt"
    }

    def '`checkCommitNeeded` should detect changed files'() {
        given:
        addAndCommitToGit(localGit, 'changed.txt') {it << 'changed1'}
        project.file("changed.txt").withWriter {it << "changed2"}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "changed.txt"
    }

    def '`checkCommitNeeded` should detect modified files'() {
        given:
        addAndCommitToGit(localGit, 'modified.txt') {it << 'modified1'}
        addToGit(localGit, 'modified.txt') {it << 'modified2'}
        when:
        project.checkCommitNeeded.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "You have uncommitted files"
        ex.cause.message.contains "modified.txt"
    }
}
