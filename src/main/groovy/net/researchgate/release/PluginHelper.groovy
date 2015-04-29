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

import groovy.text.SimpleTemplateEngine
import org.apache.tools.ant.BuildException
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

    boolean useAutomaticVersion() {
        project.hasProperty('gradle.release.useAutomaticVersion') && project.getProperty('gradle.release.useAutomaticVersion') == "true"
    }

	/**
	 * Executes command specified and retrieves its "stdout" output.
	 *
	 * @param failOnStderr whether execution should fail if there's any "stderr" output produced, "true" by default.
	 * @param commands commands to execute
	 * @return command "stdout" output
	 */
	String exec(
        Map options = [:],
        List<String> commands
    ) {
        StringBuffer out = new StringBuffer()
        StringBuffer err = new StringBuffer()

        File directory = options['directory'] ? options['directory'] as File : project.rootDir
        List processEnv = options['env'] ? ((options['env'] as Map) << System.getenv()).collect { "$it.key=$it.value" } : null

        log.info("Running $commands in [$directory]")
        Process process = commands.execute(processEnv, directory)
        log.info("Running $commands produced output: [${out.toString().trim()}]")

        process.waitForProcessOutput(out, err)

        if (err.toString()) {
            def message = "Running $commands produced an error: [${err.toString().trim()}]"

            if (options['failOnStderr'] as boolean) {
                throw new GradleException(message)
            } else {
                log.warn(message)
            }
        }

        if (options['errorPatterns'] && [out, err]*.toString().any { String s -> (options['errorPatterns'] as List<String>).any { s.contains(it) } }) {
            throw new GradleException("${ options['errorMessage'] ? options['errorMessage'] as String : 'Failed to run [' + commands.join(' ') + ']' } - [$out][$err]")
        }

        out.toString()
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
			def versionProperties = releaseConvention().versionProperties + 'version'
			def propFile = findPropertiesFile()
			versionProperties.each { prop ->
				try {
                    project.ant.propertyfile(file: propFile) {
                        entry(key: prop, value: project.version)
                    }
				} catch (BuildException be) {
					throw new GradleException("Unable to update version property.", be)
				}
			}
		}
	}

	File findPropertiesFile() {
		File propertiesFile = project.file(releaseConvention().versionPropertyFile)
		if (!propertiesFile.file) {
			if (!isVersionDefined()) {
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

    boolean isVersionDefined() {
        project.version && "unspecified" != project.version
    }

    void warnOrThrow(boolean doThrow, String message) {
		if (doThrow) {
			throw new GradleException(message)
		} else {
			log.warn("!!WARNING!! $message")
		}
	}

	String tagName() {
        def options = releaseConvention()
        def tagName
        if (options.tagTemplate) {
            def engine = new SimpleTemplateEngine()
            def binding = [
                "version": project.version,
                "name"   : project.rootProject.name
            ]
            tagName = engine.createTemplate(options.tagTemplate).make(binding).toString()
        } else {
            // Backward compatible remove in version 3.0
            String prefix = options.tagPrefix ? "${options.tagPrefix}-" : (options.includeProjectNameInTag ? "${project.rootProject.name}-" : "")
            tagName = "${prefix}${project.version}"
        }

        tagName
	}

	String findProperty(String key, String defaultVal = "") {
		System.properties[key] ?: project.properties[key] ?: defaultVal
	}

    /**
     * Capitalizes first letter of the String specified.
     *
     * @param s String to capitalize
     * @return String specified with first letter capitalized
     */
    protected static String capitalize(String s) {
        s[0].toUpperCase() + (s.size() > 1 ? s[1..-1] : '')
    }

    /**
     * Reads user input from the console.
     *
     * @param message Message to display
     * @param defaultValue (optional) default value to display
     * @return User input entered or default value if user enters no data
     */
    protected static String readLine(String message, String defaultValue = null) {
        String _message = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")
        if (System.console()) {
            return System.console().readLine(_message) ?: defaultValue
        }
        println "$_message (WAITING FOR INPUT BELOW)"

        return System.in.newReader().readLine() ?: defaultValue
    }

    private static boolean promptYesOrNo(String message, boolean defaultValue = false) {
        def defaultStr = defaultValue ? 'Y' : 'n'
        String consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }
        defaultValue
    }
}
