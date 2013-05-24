package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

@Mixin(PluginHelper)
class GitReleasePluginScmRootTests extends GitSpecification {
	def setup() {
		final subdirectoryInRepo = new File(localGit.repository.getWorkTree(), "subdirectory")
		project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(subdirectoryInRepo).build()
		project.apply plugin: ReleasePlugin
	}

	def "scmRootDir parameter as relative path should allow to use git root located above project directory"() {
		given:
		project.git.scmRootDir = "../"
		when:
		project.checkUpdateNeeded.execute()
		then:
		noExceptionThrown()
	}

	def "scmRootDir parameter as absolute path should allow to use git root located above project directory"() {
		given:
		project.git.scmRootDir = localGit.repository.getWorkTree().absolutePath
		when:
		project.checkUpdateNeeded.execute()
		then:
		noExceptionThrown()
	}

	def "should fail without set scmRoot for project in repo subdirectory"() {
		given:
		project.git.scmRootDir = null
		when:
		project.checkUpdateNeeded.execute()
		then:
		GradleException e = thrown()
		e.cause.message.contains("Not a git repository")
	}
}
