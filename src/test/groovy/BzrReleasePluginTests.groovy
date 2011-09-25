import org.gradle.api.Plugin
import org.gradle.api.Project
import release.BzrReleasePlugin

/**
 * User: elberry
 * Date: 9/21/11
 */
class BzrReleasePluginTests extends AbstractReleasePluginTests {
	@Override
	Class<Plugin<Project>> getPluginClass() {
		return BzrReleasePlugin
	}

	@Override
	void initProject(File root, String projectName) {
		def out = new StringBuffer()
		def err = new StringBuffer()
		def commands = ['bzr', 'init', projectName]
		def process = commands.execute([], root)

		println " >>> Running $commands"

		process.waitForProcessOutput(out, err)

		println " >>> Running $commands: [$out][$err]"

		assert err.length() < 1, "Running $commands produced an stderr output: [$err]"

		println out

		File projectRoot = new File(root, projectName)

		new File(projectRoot, "build.gradle").text = """
		apply plugin: 'groovy'
		apply plugin: 'release'
		""".stripIndent()
	}
}
