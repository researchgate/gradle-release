package net.researchgate.release.tasks

import groovy.text.SimpleTemplateEngine
import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.apache.tools.ant.BuildException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BaseReleaseTask extends DefaultTask {

    static final String RELEASE_GROUP = 'Release'

    private static final String LINE_SEP = System.getProperty('line.separator')
    private static final String PROMPT = "${LINE_SEP}??>"

    ReleaseExtension extension
    Map<String, Object> pluginAttributes

    Project getRootProject() {
        def project = getProject()
        if (project.getParent() != null) {
            return project.getParent()
        }
        return project
    }

    BaseReleaseTask() {
        group = RELEASE_GROUP
        extension = getRootProject().extensions.getByName('release') as ReleaseExtension
        pluginAttributes = extension.attributes
    }

    BaseScmAdapter getScmAdapter() {
        return extension.scmAdapter
    }

    /**
     * Retrieves SLF4J {@link org.slf4j.Logger} instance.
     *
     * The logger is taken from the {@link Project} instance if it's initialized already
     * or from SLF4J {@link org.slf4j.LoggerFactory} if it's not.
     *
     * @return SLF4J {@link org.slf4j.Logger} instance
     */
    Logger getLog() { getProject()?.logger ?: LoggerFactory.getLogger(this.class) }

    boolean useAutomaticVersion() {
        findProperty('release.useAutomaticVersion') == 'true'
    }

    File findPropertiesFile(Project project) {
        File propertiesFile = project.file(extension.versionPropertyFile.get())
        Map<String, Object> projectAttributes = extension.attributes
        if (!propertiesFile.file) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion('1.0.0')
            }

            if (!useAutomaticVersion() && promptYesOrNo('Do you want to use SNAPSHOT versions in between releases')) {
                projectAttributes.usesSnapshot = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, 'version', project.version)
                projectAttributes.propertiesFileCreated = true
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
                getProject().ant.echo(file: file, message: "$key=$version")
            } else {
                // we use replace here as other ant tasks escape and modify the whole file
                getProject().ant.replaceregexp(file: file, byline: true) {
                    regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).+\$")
                    substitution(expression: "\\1$key\\2$version")
                }
            }
        } catch (BuildException be) {
            throw new GradleException('Unable to write version property.', be)
        }
    }

    boolean isVersionDefined() {
        getProject().version && Project.DEFAULT_VERSION != getProject().version
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
        def engine = new SimpleTemplateEngine()
        def binding = [
                "version": project.version,
                "name"   : project.name
        ]
        tagName = engine.createTemplate(extension.tagTemplate.get()).make(binding).toString()
        return tagName
    }

    String findProperty(String key, Object defaultVal = null, String deprecatedKey = null) {
        Project project = getRootProject()
        def property = System.getProperty(key) ?: project.hasProperty(key) ? project.property(key) : null

        if (!property && deprecatedKey) {
            property = System.getProperty(deprecatedKey) ?: project.hasProperty(deprecatedKey) ? project.property(deprecatedKey) : null
            if (property) {
                log.warn("You are using the deprecated parameter '${deprecatedKey}'. Please use the new parameter '$key'. The deprecated parameter will be removed in 3.0")
            }
        }

        property ?: defaultVal
    }

    String getReleaseVersion(String candidateVersion = null) {
        if (candidateVersion == null) {
            candidateVersion = "${project.version}"
        }

        String key = "release.releaseVersion"
        String releaseVersion = findProperty(key, null, 'releaseVersion')

        if (releaseVersion != null) {
            return releaseVersion
        } else if (useAutomaticVersion()) {
            return candidateVersion
        }

        return readLine("This release version for " + project.name + ":", releaseVersion ?: candidateVersion)
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

    static boolean promptYesOrNo(String message, boolean defaultValue = false) {
        String defaultStr = defaultValue ? 'y' : 'n'
        String consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }

        defaultValue
    }
}
