/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release.cli

import org.gradle.api.GradleException
import org.slf4j.Logger

class Executor {

    private Logger logger

    Executor(Logger logger = null) {
        this.logger = logger
    }

    String exec(
        Map options = [:],
        List<String> commands
    ) {
        StringBuffer out = new StringBuffer()
        StringBuffer err = new StringBuffer()

        File directory = options['directory'] ? options['directory'] as File : null
        Map processEnv = options['env'] ? System.getenv() + (options['env'] as Map) : System.getenv()

        logger?.info("Running $commands in [$directory]")
        Process process = commands.execute(processEnv.collect { "$it.key=$it.value" }, directory)
        process.waitForProcessOutput(out, err)
        logger?.info("Running $commands produced output: [${out.toString().trim()}]")

        if (process.exitValue()) {
            def message = "Running $commands produced an error: [${err.toString().trim()}]"

            if (options['failOnStderr'] as boolean) {
                throw new GradleException(message)
            } else {
                logger?.warn(message)
            }
        }

        if (options['errorPatterns'] && [out, err]*.toString().any { String s -> (options['errorPatterns'] as List<String>).any { s.contains(it) } }) {
            throw new GradleException("${ options['errorMessage'] ? options['errorMessage'] as String : 'Failed to run [' + commands.join(' ') + ']' } - [$out][$err]")
        }

        out.toString()
    }
}
