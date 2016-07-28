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
import net.researchgate.release.cli.Executor
import org.apache.tools.ant.BuildException
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PluginHelper {

    private static final String LINE_SEP = System.getProperty('line.separator')
    private static final String PROMPT = "${LINE_SEP}??>"

    protected Project project

    protected ReleaseExtension extension

    protected Executor executor

    protected Map<String, Object> attributes = [:]

    /**
     * Retrieves SLF4J {@link Logger} instance.
     *
     * The logger is taken from the {@link Project} instance if it's initialized already
     * or from SLF4J {@link LoggerFactory} if it's not.
     *
     * @return SLF4J {@link Logger} instance
     */
    Logger getLog() { project?.logger ?: LoggerFactory.getLogger(this.class) }

    boolean useAutomaticVersion() {
        findProperty('release.useAutomaticVersion', null, 'gradle.release.useAutomaticVersion') == 'true'
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
        initExecutor()
        options['directory'] = options['directory'] ?: project.rootDir
        executor.exec(options, commands)
    }

    private void initExecutor() {
        if (!executor) {
            executor = new Executor(log)
        }
    }

    File findPropertiesFile() {
        File propertiesFile = project.file(extension.versionPropertyFile)
        if (!propertiesFile.file) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion('1.0.0')
            }

            if (!useAutomaticVersion() && promptYesOrNo('Do you want to use SNAPSHOT versions inbetween releases')) {
                attributes.usesSnapshot = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, 'version', project.version)
                attributes.propertiesFileCreated = true
            } else {
                log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
                throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
            }
        }
        propertiesFile
    }

    protected void writeVersion(File file, String key, version) {
        try {
            if (!file.file) {
                project.ant.echo(file: file, message: "$key=$version")
            } else {
                // we use replace here as other ant tasks escape and modify the whole file
                project.ant.replaceregexp(file: file, byline: true) {
                    regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).+\$")
                    substitution(expression: "\\1$key\\2$version")
                }
            }
        } catch (BuildException be) {
            throw new GradleException('Unable to write version property.', be)
        }
    }

    boolean isVersionDefined() {
        project.version && Project.DEFAULT_VERSION != project.version
    }

    void warnOrThrow(boolean doThrow, String message) {
        if (doThrow) {
            throw new GradleException(message)
        } else {
            log.warn("!!WARNING!! $message")
        }
    }

    String tagName() {
        def tagName
        if (extension.tagTemplate) {
            def engine = new SimpleTemplateEngine()
            def binding = [
                "version": project.version,
                "name"   : project.name
            ]
            tagName = engine.createTemplate(extension.tagTemplate).make(binding).toString()
        } else {
            // Backward compatible remove in version 3.0
            String prefix = extension.tagPrefix ? "${extension.tagPrefix}-" : (extension.includeProjectNameInTag ? "${project.name}-" : "")
            tagName = "${prefix}${project.version}"
        }

        tagName
    }

    String findProperty(String key, Object defaultVal = null, String deprecatedKey = null) {
        def property = System.getProperty(key) ?: project.hasProperty(key) ? project.property(key) : null

        if (!property && deprecatedKey) {
            property = System.getProperty(deprecatedKey) ?: project.hasProperty(deprecatedKey) ? project.property(deprecatedKey) : null
            if (property) {
                log.warn("You are using the deprecated parameter '${deprecatedKey}'. Please use the new parameter '$key'. The deprecated parameter will be removed in 3.0")
            }
        }

        property ?: defaultVal
    }

    String getReleaseVersion(String candidateVersion = "${project.version}") {
        String releaseVersion = findProperty('release.releaseVersion', null, 'releaseVersion')

        if (useAutomaticVersion()) {
            return releaseVersion ?: candidateVersion
        }

        return readLine("This release version:", releaseVersion ?: candidateVersion)
    }

    /**
     * Updates properties file (<code>gradle.properties</code> by default) with new version specified.
     * If configured in plugin convention then updates other properties in file additionally to <code>version</code> property
     *
     * @param newVersion new version to store in the file
     */
    void updateVersionProperty(String newVersion) {
        String oldVersion = project.version as String
        if (oldVersion != newVersion) {
            project.version = newVersion
            attributes.versionModified = true
            project.subprojects?.each { it.version = newVersion }
            List<String> versionProperties = extension.versionProperties + 'version'
            versionProperties.each { writeVersion(findPropertiesFile(), it, project.version) }
        }
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

        System.in.newReader().readLine() ?: defaultValue
    }

    private static boolean promptYesOrNo(String message, boolean defaultValue = false) {
        String defaultStr = defaultValue ? 'Y' : 'n'
        String consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }

        defaultValue
    }
}
