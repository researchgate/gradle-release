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

import net.researchgate.release.cli.Executor
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PluginHelper {

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

    void warnOrThrow(boolean doThrow, String message) {
        if (doThrow) {
            throw new GradleException(message)
        } else {
            log.warn("!!WARNING!! $message")
        }
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

}
