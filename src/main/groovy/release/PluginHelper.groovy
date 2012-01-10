package release

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Helper object extended by plugins.
 * @author evgenyg
 */
class PluginHelper {

	private static final String LINE_SEP = System.getProperty('line.separator')
	private static final String PROMPT = "${LINE_SEP}>"

	@SuppressWarnings('StatelessClass')
	Project project

	/**
	 * Retrieves SLF4J {@link Logger} instance.
	 *
	 * The logger is taken from the {@link Project} instance if it's initialized already
	 * or from SLF4J {@link LoggerFactory} if it's not.
	 *
	 * @return SLF4J {@link Logger} instance
	 */
	Logger getLog() { project?.logger ?: LoggerFactory.getLogger(this.class) }

	/**
	 * Sets convention specified under the plugin name provided.
	 *
	 * @param pluginName name of the plugin
	 * @param convention convention object to set
	 * @return convention instance set
	 */
	Object setConvention(String pluginName, Object convention) {
		assert pluginName && convention
		project.convention.plugins[pluginName] = convention
	}

	/**
	 * Retrieves plugin convention of the type specified.
	 *
	 * @param project current Gradle project
	 * @param pluginName plugin name
	 * @param conventionType convention type
	 * @return plugin convention of the type specified
	 */
	@SuppressWarnings('UnnecessaryPublicModifier')
	public <T> T convention(String pluginName, Class<T> conventionType) {

		Object convention = project.convention.plugins[pluginName]

		assert convention, "Project contains no \"$pluginName\" plugin convention"
		assert conventionType.isInstance(convention),     \
                "Project contains \"$pluginName\" plugin convention, " +
				"but it's of type [${ convention.class.name }] rather than [${ conventionType.name }]"

		(T) convention
	}

	/**
	 * Gets current {@link ReleasePluginConvention}.
	 *
	 * @param project current Gradle project
	 * @return current {@link ReleasePluginConvention}.
	 */
	ReleasePluginConvention releaseConvention() {
		convention('release', ReleasePluginConvention)
	}

	/**
	 * Executes command specified and retrieves its "stdout" output.
	 *
	 * @param failOnStderr whether execution should fail if there's any "stderr" output produced, "true" by default.
	 * @param commands commands to execute
	 * @return command "stdout" output
	 */
	String exec(boolean failOnStderr = true, Map env = [:], File directory = null, String... commands) {

		def out = new StringBuffer()
		def err = new StringBuffer()
		def logMessage = "Running \"${commands.join(' ')}\"${ directory ? ' in [' + directory.canonicalPath + ']' : '' }"
		def process = (env || directory) ?
			(commands as List).execute(env.collect { "$it.key=$it.value" } as String[], directory) :
			(commands as List).execute()

		log.info(logMessage)

		process.waitForProcessOutput(out, err)

		log.info("$logMessage: [$out][$err]")

		if (err.toString()) {
			def message = "$logMessage produced an error: [${err.toString().trim()}]"
			if (failOnStderr) {
				throw new GradleException(message)
			} else {
				log.warn(message)
			}
		}
		out.toString()
	}

	/**
	 * Executes command specified and verifies neither "stdout" or "stderr" contain an error pattern specified.
	 *
	 * @param commands commands to execute
	 * @param errorMessage error message to throw, optional
	 * @param errorPattern error patterns to look for, optional
	 */
	void exec(List<String> commands, String errorMessage, String... errorPattern) {
		def out = new StringBuffer()
		def err = new StringBuffer()
		def process = commands.execute()

		log.info(" >>> Running $commands")

		process.waitForProcessOutput(out, err)

		log.info(" >>> Running $commands: [$out][$err]")

		if ([out, err]*.toString().any { String s -> errorPattern.any { s.contains(it) }}) {
			throw new GradleException("${ errorMessage ?: 'Failed to run [' + commands.join(' ') + ']' } - [$out][$err]")
		}
	}

	/**
	 * Capitalizes first letter of the String specified.
	 *
	 * @param s String to capitalize
	 * @return String specified with first letter capitalized
	 */
	String capitalize(String s) {
		s[0].toUpperCase() + (s.size() > 1 ? s[1..-1] : '')
	}

	boolean promptYesOrNo(String message, boolean defaultValue = false) {
		def defaultStr = defaultValue ? 'Y' : 'n'
		String consoleVal = System.console().readLine("${PROMPT} ${message} (Y|n)[${defaultStr}] ")
		if (consoleVal) {
			return consoleVal.toLowerCase().startsWith('y')
		}
		return defaultValue
	}

	/**
	 * Reads user input from the console.
	 *
	 * @param message Message to display
	 * @param defaultValue (optional) default value to display
	 * @return User input entered or default value if user enters no data
	 */
	String readLine(String message, String defaultValue = null) {
		System.console().readLine("$PROMPT $message " + (defaultValue ? "[$defaultValue] " : '')) ?:
			defaultValue
	}

	/**
	 * Updates 'gradle.properties' file with new version specified.
	 *
	 * @param project current project
	 * @param newVersion new version to store in the file
	 */
	void updateVersionProperty(String newVersion) {

		project.version = newVersion
		File propertiesFile = findPropertiesFile()

		Properties gradleProps = new Properties()
		propertiesFile.withReader { gradleProps.load(it) }

		gradleProps.version = newVersion
		propertiesFile.withWriter {
			gradleProps.store(it, "Version updated to '${newVersion}', by Gradle release plugin (http://code.launchpad.net/~gradle-plugins/gradle-release/).")
		}
	}

	File findPropertiesFile() {
		File propertiesFile = project.file('gradle.properties')
		if (!propertiesFile.file) {
			boolean createIt = project.hasProperty("version") && promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")
			if (createIt) {
				propertiesFile.append("version = \"${project.version}\"")
			} else {
				throw new GradleException("[$propertiesFile.canonicalPath] not found, create it and specify version = ...")
			}
		}
		return propertiesFile
	}

	void warnOrThrow(boolean doThrow, String message) {
		if (doThrow) {
			throw new GradleException(message)
		} else {
			log.warn(message)
		}
	}
}
