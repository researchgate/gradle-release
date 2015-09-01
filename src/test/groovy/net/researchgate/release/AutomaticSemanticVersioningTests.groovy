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
import org.gradle.testfixtures.ProjectBuilder

class AutomaticSemanticVersioningTests extends GitSpecification {

    Project project

    PluginHelper helper

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.apply plugin: ReleasePlugin
        project.createScmAdapter.execute()

        helper = new PluginHelper(project: project, extension: project.extensions['release'] as ReleaseExtension)
    }
	
	def cleanup() {
		localGit.tagDelete().setTags("1.0.0").call()
	}

    def 'feature version should be incremented when no previous tags exist based on project version'() {
        given:
        project.version = '1.0.0-SNAPSHOT' 
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'feature-JIRA-1\' into \'master\'')
        when:
        project.confirmReleaseVersion.execute()
        then:
		project.version == '1.1.0'
    }
	
	def 'patch version should be incremented when no previous tags exist based on project version'() {
		given:
		project.version = '1.0.0-SNAPSHOT'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'patch-JIRA-1\' into \'master\'')
		when:
		project.confirmReleaseVersion.execute()
		then:
		project.version == '1.0.1'
	}
	
	def 'major version should be incremented when no previous tags exist based on project version'() {
		given:
		project.version = '1.0.0-SNAPSHOT'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'major-JIRA-1\' into \'master\'')
		when:
		project.confirmReleaseVersion.execute()
		then:
		project.version == '2.0.0'
	}

    def 'feature version should be incremented based on last tag version'() {
        given:
        project.version = '1.0.0'
        localGit.tag().setName(helper.tagName()).call()
		
		project.version = '1.0.1'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'feature-JIRA-1\' into \'master\'')
        when:
        project.confirmReleaseVersion.execute()
        then:
        project.version == '1.1.0'
    }
	
	def 'patch version should be incremented based on last tag version'() {
		given:
		project.version = '1.0.0'
		localGit.tag().setName(helper.tagName()).call()
		
		project.version = '1.0.1'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'patch-JIRA-1\' into \'master\'')
		when:
		project.confirmReleaseVersion.execute()
		then:
		project.version == '1.0.1'
	}
	
	def 'major version should be incremented based on last tag version'() {
		given:
		project.version = '1.0.0'
		localGit.tag().setName(helper.tagName()).call()
		
		project.version = '1.0.1'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'Merge branch \'major-JIRA-1\' into \'master\'')
		when:
		project.confirmReleaseVersion.execute()
		then:
		project.version == '2.0.0'
	}
	
	def 'cant version because there is no enough information on last commit message'() {
		given:
		project.version = '1.0.0'
		localGit.tag().setName(helper.tagName()).call()
		
		project.version = '1.0.1'
		project.setProperty('release.useAutomaticVersion',  "true")
		gitAdd(localGit, 'modified.txt') { it << "version=$project.version" }
		gitCommit(localGit, 'something different from a branch merge')
		when:
		project.confirmReleaseVersion.execute()
		then:
		GradleException ex = thrown()
		ex.cause.message.contains "Could not assign release version automatically"
	}
	
}
