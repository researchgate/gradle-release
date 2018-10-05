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

import net.researchgate.release.tasks.CreateReleaseTag
import net.researchgate.release.tasks.UnSnapshotVersion
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GitReleasePluginCreateReleaseTagTests extends GitSpecification {

    Project project

    CreateReleaseTag createReleaseTagTask;

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.apply plugin: ReleasePlugin
        project.createScmAdapter.execute()

        createReleaseTagTask = project.task('createReleaseTagTask', type: CreateReleaseTag)
    }

    def 'createReleaseTag should create tag and push to remote'() {
        given:
        project.version = '1.1'
        when:
        createReleaseTagTask.execute()
        then:
        localGit.tagList().call()*.name == ["refs/tags/${createReleaseTagTask.tagName(project)}"]
        remoteGit.tagList().call()*.name == ["refs/tags/${createReleaseTagTask.tagName(project)}"]
    }

    def 'createReleaseTag should throw exception when tag exist'() {
        given:
        project.version = '1.2'
        localGit.tag().setName(createReleaseTagTask.tagName(project)).call()
        when:
        createReleaseTagTask.execute()
        then:
        thrown GradleException
    }

    def 'createReleaseTag with disabled pushing changes should only create tag and not push to remote'() {
        given:
        project.version = '1.3'
        project.release.git.pushToRemote = null
        when:
        createReleaseTagTask.execute()
        then:
        localGit.tagList().call().findAll { it.name == "refs/tags/${createReleaseTagTask.tagName(project)}" }.size() == 1
        remoteGit.tagList().call().findAll { it.name == "refs/tags/${createReleaseTagTask.tagName(project)}" }.isEmpty()
    }

    def 'createReleaseTag with configured but non existent remote should throw exception'() {
        given:
        project.version = '1.4'
        project.release.git.pushToRemote = 'myremote'
        when:
        createReleaseTagTask.execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains "myremote"
    }

    def 'createReleaseTag with configured remote should push to it'() {
        given:
        project.version = '1.5'
        project.release.git.pushToRemote = 'myremote'
        localGit.repository.config.setString("remote", "myremote", "url", remoteGit.repository.directory.canonicalPath);
        localGit.repository.config.save();
        when:
        createReleaseTagTask.execute()
        then:
        localGit.tagList().call().findAll { it.name == "refs/tags/${createReleaseTagTask.tagName(project)}" }.size() == 1
        remoteGit.tagList().call().findAll { it.name == "refs/tags/${createReleaseTagTask.tagName(project)}" }.size() == 1
    }
}
