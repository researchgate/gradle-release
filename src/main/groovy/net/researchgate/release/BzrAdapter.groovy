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

import org.gradle.api.GradleException
import org.gradle.api.Project

class BzrAdapter extends BaseScmAdapter {

    private static final String ERROR = 'ERROR'
    private static final String DELIM = '\n  * '

    BzrAdapter(Project project) {
        super(project)
    }

    @Override
    Object createNewConfig() {
        return null
    }

    @Override
    boolean isSupported(File directory) {
        if (!directory.list().grep('.bzr')) {
            return directory.parentFile? isSupported(directory.parentFile) : false
        }

        true
    }

    @Override
    void init() {
        boolean hasXmlPlugin = exec(['bzr', 'plugins']).readLines().any { it.startsWith('xmloutput') }

        if (!hasXmlPlugin) {
            throw new GradleException('The required xmloutput plugin is not installed in Bazaar, please install it.')
        }
    }

    @Override
    void checkCommitNeeded() {
        String out = exec(['bzr', 'xmlstatus'])
        def xml = new XmlSlurper().parseText(out)
        def added = xml.added?.size() ?: 0
        def modified = xml.modified?.size() ?: 0
        def removed = xml.removed?.size() ?: 0
        def unknown = xml.unknown?.size() ?: 0

        def c = { String name ->
            ["${ capitalize(name)}:",
                    xml."$name".file.collect { it.text().trim() },
                    xml."$name".directory.collect { it.text().trim() }].
                    flatten().
                    join(DELIM) + '\n'
        }

        if (unknown) {
            warnOrThrow(extension.failOnUnversionedFiles, "You have un-versioned files:\n${c('unknown')}")
        } else if (added || modified || removed) {
            def message = 'You have un-committed files:\n' +
                    (added ? c('added') : '') +
                    (modified ? c('modified') : '') +
                    (removed ? c('removed') : '')
            warnOrThrow(extension.failOnCommitNeeded, message)
        }
    }

    @Override
    void checkUpdateNeeded() {
        String out = exec(['bzr', 'xmlmissing'])
        def xml = new XmlSlurper().parseText(out)
        int extra = ("${xml.extra_revisions?.@size}" ?: 0) as int
        int missing = ("${xml.missing_revisions?.@size}" ?: 0) as int

        Closure c = {
            int number, String name, String path ->

            ["You have $number $name changes${ number == 1 ? '' : 's' }:",
                    xml."$path".logs.log.collect {
                        int cutPosition = 40
                        String message = it.message.text()
                        message = message.readLines()[0].substring(0, Math.min(cutPosition, message.size())) +
                                (message.size() > cutPosition ? ' ..' : '')
                        "[$it.revno]: [$it.timestamp][$it.committer][$message]"
                    }].
                    flatten().
                    join(DELIM)
        }

        if (extra > 0) {
            warnOrThrow(extension.failOnPublishNeeded, c(extra, 'unpublished', 'extra_revisions'))
        }

        if (missing > 0) {
            warnOrThrow(extension.failOnUpdateNeeded, c(missing, 'missing', 'missing_revisions'))
        }
    }

    /**
     * @param message ignored.
     */
    @Override
    void createReleaseTag(String message) {
        // message is ignored
        exec(['bzr', 'tag', tagName()], errorMessage: 'Error creating tag', errorPatterns: [ERROR])
    }


    @Override
    void commit(String message) {
        exec(['bzr', 'ci', '-m', message], errorMessage: 'Error committing new version', errorPatterns: [ERROR])
        exec(['bzr', 'push', ':parent'], errorMessage: 'Error committing new version', errorPatterns: [ERROR])
    }

    @Override
    void revert() {
        exec(['bzr', 'revert', findPropertiesFile().name], errorMessage: 'Error reverting changes made by the release plugin.', errorPatterns: [ERROR])
    }
	
	@Override
	String assignReleaseVersionAutomatically(String currentVersion) {
		throw new GradleException("Method not implemented yet.")
	}
}
