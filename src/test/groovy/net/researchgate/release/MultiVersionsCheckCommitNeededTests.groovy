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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

import static org.eclipse.jgit.lib.Repository.shortenRefName

class MultiVersionsCheckCommitNeededTests extends GitSpecification {

    Project project
    Project subproject1
    Project subproject2

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()


        File rootDir = project.getProjectDir();
        File subProject1Dir = new File(rootDir, "subproject1")
        subProject1Dir.mkdir()
        subproject1 = ProjectBuilder.builder().withName("subproject1").withProjectDir(subProject1Dir).withParent(project).build()
        subproject1.version = '1.1'

        File subProject2Dir = new File(rootDir, "subproject2")
        subProject2Dir.mkdir()
        subproject2 = ProjectBuilder.builder().withName("subproject2").withProjectDir(subProject2Dir).withParent(project).build()
        subproject2.version = '2.1'

        project.apply plugin: 'java'
        project.apply plugin: ReleasePlugin
        project.createScmAdapter.execute()
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    @Override
    def createDefaultVersionFile() {
        return false;
    }

    public Task findByPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidUserDataException("A path must be specified!");
        } else if (!path.contains(":")) {
            return project.tasks.findByName(path);
        } else {
            def of1 = path.lastIndexOf(':');
            def projectPath = path.substring(0, of1)
            ProjectInternal project = this.project.findProject((projectPath == null || projectPath.isEmpty()) ? ":" : projectPath);
            if (project == null) {
                return null;
            } else {
                def of = path.lastIndexOf(':');
                def substring = path.substring(of)
                return (Task)project.getTasks().findByName(substring);
            }
        }
    }


    def 'Uncommited file should cause exception'() {
        given: 'multimodule project'

        gitAddAndCommit(localGit, "subproject1/gradle.properties") { it << "version=$subproject1.version" }
        gitAddAndCommit(localGit, "subproject2/gradle.properties") { it << "version=$subproject2.version" }

        project.file('untracked.txt').withWriter { it << "untracked" }

        project.release {
            useMultipleVersionFiles = true
        }

        localGit.push().setForce(true).call()
        when: 'calling release task indirectly'
        project.tasks['release'].tasks.each { task ->

            if (task == ':runBuildTasks') {
                project.tasks.getByPath(task).tasks.each { buildTask ->
                    project.tasks.getByPath(buildTask).execute()
                }
            } else {
                project.tasks.getByPath(task).execute()
            }
        }
        def st = localGit.status().call()
        gitHardReset(remoteGit)
        then: 'Exception expected from an untracked file'
        GradleException ex = thrown()
        ex.cause.message.contains "You have unversioned files"
        ex.cause.message.contains "untracked.txt"
    }
}
