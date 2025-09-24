package net.researchgate.release

import net.researchgate.release.cli.Executor
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Serializable to be cached by Gradle.
 *
 * <p>Mark any fields that cannot be serialized as {@code transient} and handle {@code null} values (when deserialized).
 */
class CacheablePluginHelper implements Serializable {

    private final File projectRootDir
    private final File propertiesFile

    private final transient Logger log

    private transient Executor executor

    CacheablePluginHelper(Project project, ReleaseExtension extension) {
        projectRootDir = project.rootDir
        propertiesFile = project.file(extension.versionPropertyFile)
        log = project.logger
    }

    File getPropertiesFile() {
        propertiesFile
    }

    String exec(
            Map options = [:],
            List<String> commands
    ) {
        initExecutor()
        options['directory'] = options['directory'] ?: projectRootDir
        executor.exec(options, commands)
    }

    private void initExecutor() {
        if (!executor) {
            executor = new Executor(log ?: LoggerFactory.getLogger(this.class))
        }
    }
}
