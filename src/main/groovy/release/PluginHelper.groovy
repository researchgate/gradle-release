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
	private static final String PROMPT = "${LINE_SEP}??>"

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
		assert conventionType.isInstance(convention),       \
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

	boolean useAutomaticVersion() {
		project.hasProperty('gradle.release.useAutomaticVersion') && project.getProperty('gradle.release.useAutomaticVersion') == "true"
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

		if ([out, err]*.toString().any { String s -> errorPattern.any { s.contains(it) } }) {
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
		String consoleVal = readLine("${message} (Y|n)", defaultStr)
		if (consoleVal) {
			return consoleVal.toLowerCase().startsWith('y')
		}
		defaultValue
	}

	/**
	 * Reads user input from the console.
	 *
	 * @param message Message to display
	 * @param defaultValue (optional) default value to display
	 * @return User input entered or default value if user enters no data
	 */
	String readLine(String message, String defaultValue = null) {
		String _message = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")
		if (System.console()) {
			return System.console().readLine(_message) ?: defaultValue
		}
		println "$_message (WAITING FOR INPUT BELOW)"
		return System.in.newReader().readLine() ?: defaultValue
	}

	/**
	 * Updates properties file (<code>gradle.properties</code> by default) with new version specified.
	 * If configured in plugin convention then updates other properties in file additionally to <code>version</code> property
	 *
	 * @param newVersion new version to store in the file
	 */
	void updateVersionProperty(String newVersion) {
		def oldVersion = "${project.version}"
		if (oldVersion != newVersion) {
			project.version = newVersion
			project.ext.set('versionModified', true)
			project.subprojects?.each { Project subProject ->
				subProject.version = newVersion
			}
			(releaseConvention().versionProperties + 'version').each {
				project.ant.replace(file: findPropertiesFile(), token: "${it}=${oldVersion}", value: "${it}=${newVersion}")
			}
		}
	}

	File findPropertiesFile() {
		File propertiesFile = project.file(releaseConvention().versionPropertyFile)
		if (!propertiesFile.file) {
			if (!project.version || "unspecified" == project.version) {
				project.version = useAutomaticVersion() ? "1.0" : readLine("Version property not set, please set it now:", "1.0")
			}
			boolean createIt = project.hasProperty('version') && promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")
			if (createIt) {
				propertiesFile.append("version=${project.version}")
			} else {
				log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
				throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and and specify the version property.")
			}
		}
		propertiesFile
	}

	void warnOrThrow(boolean doThrow, String message) {
		if (doThrow) {
			throw new GradleException(message)
		} else {
			log.warn("!!WARNING!! $message")
		}
	}

	String tagName() {
		String prefix = releaseConvention().tagPrefix ? "${releaseConvention().tagPrefix}-" : (releaseConvention().includeProjectNameInTag ? "${project.rootProject.name}-" : "")
		return "${prefix}${project.version}"
	}

	String findProperty(String key, String defaultVal = "") {
		System.properties[key] ?: project.properties[key] ?: defaultVal
	}
}
