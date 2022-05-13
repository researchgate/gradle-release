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
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class GitReleasePluginCreateReleaseTagTests extends GitSpecification {

    File settingsFile
    File buildFile
    File propertiesFile
    File localDir

    Project project
    PluginHelper helper

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
                    pushToBranchPrefix = 'refs/for/'
                    requireBranch = 'master'
                }
            }
        """
        }
        gitAddAndCommit(localGit, 'gradle.properties') {
            it << 'version=1.1'
        }
        localGit.push().call()

        project = new ProjectBuilder().withProjectDir(localDir).build()
        project.plugins.apply(BasePlugin.class)
        project.plugins.apply(ReleasePlugin.class)
        helper = new PluginHelper(project: project, extension: project.extensions['release'] as ReleaseExtension)
    }

    def 'createReleaseTag should create tag and push to remote'() {
        given:
        project.version = '1.1'
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('createReleaseTag')
                .withPluginClasspath()
                .build()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':createReleaseTag').outcome == TaskOutcome.SUCCESS

        localGit.tagList().call()*.name == ["refs/tags/${helper.tagName()}"]
        remoteGit.tagList().call()*.name == ["refs/tags/${helper.tagName()}"]
    }

    def 'createReleaseTag should throw exception when tag exist'() {
        given:
        project.version = '1.2'
        localGit.tag().setName(helper.tagName()).call()
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('createReleaseTag')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':createReleaseTag').outcome == TaskOutcome.FAILED
        result.output.contains "Duplicate tag [1.1]"
    }

    def 'createReleaseTag with disabled pushing changes should only create tag and not push to remote'() {
        given:
        gitAdd(localGit, 'gradle.properties') { it << "version=1.3" }
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    pushToRemote = null
                    requireBranch = 'master'
                }
            }
        """
        }
        localGit.push().call()
        project.version = '1.3'
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('createReleaseTag')
                .withPluginClasspath()
                .build()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':createReleaseTag').outcome == TaskOutcome.SUCCESS

        localGit.tagList().call().findAll { it.name == "refs/tags/${helper.tagName()}" }.size() == 1
        remoteGit.tagList().call().findAll { it.name == "refs/tags/${helper.tagName()}" }.isEmpty()
    }

    def 'createReleaseTag with configured but non existent remote should throw exception'() {
        given:
        gitAdd(localGit, 'gradle.properties') { it << "version=1.4" }
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    pushToRemote = 'myremote'
                    requireBranch = 'master'
                }
            }
        """
        }
        localGit.push().call()
        project.version = '1.4'
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('createReleaseTag')
                .withPluginClasspath()
                .buildAndFail()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':createReleaseTag').outcome == TaskOutcome.FAILED

        result.output.contains "myremote"
    }

    def 'createReleaseTag with configured remote should push to it'() {
        given:
        gitAdd(localGit, 'gradle.properties') { it << "version=1.5" }
        gitAddAndCommit(localGit, 'build.gradle') {
            it << """
            plugins {
                id 'base'
                id 'net.researchgate.release'
            }
            release {
                git {
                    pushToRemote = 'myremote'
                    requireBranch = 'master'
                }
            }
        """
        }
        localGit.push().call()
        project.version = '1.5'
        localGit.repository.config.setString("remote", "myremote", "url", remoteGit.repository.directory.canonicalPath);
        localGit.repository.config.save();
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(localDir)
                .withGradleVersion('6.9.2')
                .withArguments('createReleaseTag')
                .withPluginClasspath()
                .build()
        then:
        result.task(':initScmAdapter').outcome == TaskOutcome.SUCCESS
        result.task(':createReleaseTag').outcome == TaskOutcome.SUCCESS

        localGit.tagList().call().findAll { it.name == "refs/tags/${helper.tagName()}" }.size() == 1
        remoteGit.tagList().call().findAll { it.name == "refs/tags/${helper.tagName()}" }.size() == 1
    }
}
