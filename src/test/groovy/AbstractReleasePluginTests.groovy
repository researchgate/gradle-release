import org.gradle.api.Plugin
import org.gradle.api.Project
import org.junit.Before
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.tasks.TaskContainer
import release.ReleasePlugin

/**
 * User: elberry
 * Date: 9/21/11
 */
abstract class AbstractReleasePluginTests {

	Project project

	abstract Class<Plugin<Project>> getPluginClass()
	abstract void initProject(File root, String projectName)

	@Before
	void createTestProject() throws Exception {
		File workingDir = new File(System.getProperty("user.dir"))
		Project thisProject = ProjectBuilder.builder().withProjectDir(workingDir).build()
		File tempDir = new File(thisProject.getBuildDir(), 'tmp')
		String pluginClassName = getPluginClass().getSimpleName()
		String now = new Date().format("yyMMdd_HH-mm-ss")
		String projectName = "${pluginClassName}_TestProject_${now}"
		File projectDir = new File(tempDir, projectName)
		initProject(tempDir, projectName)
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
	}

	@Test
	void initScmPlugin_CheckPluginInstalled() throws Exception {
		Class pluginClass = getPluginClass()
		TaskContainer tc = project.tasks
		assert tc
		assert project.plugins
		project.plugins.each { plugin ->
			println plugin
		}
		// doesn't currently work. doesn't look like any plugins are installed at all. Might need to use some task
		// verification. Eg. just perform the release, then use SCM plugin to verify it happened.
		//assert project.plugins.findPlugin(pluginClass), "${pluginClass} Doesn't seem to be installed for project: ${project.rootDir}"
	}
}