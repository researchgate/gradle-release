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

class HgAdapter extends BaseScmAdapter {

    private static final String ERROR = 'abort:'

    HgAdapter(Project project) {
        super(project)
    }

    @Override
    Object createNewConfig() {
        return null
    }

    @Override
    boolean isSupported(File directory) {
        if (!directory.list().grep('.hg')) {
            return directory.parentFile? isSupported(directory.parentFile) : false
        }

        true
    }

    @Override
    void init() {
    }

    @Override
    void checkCommitNeeded() {
        def modifications = ['A': [], 'M': [], 'R': [], '?': []]
        exec(['hg', 'status']).eachLine {line ->
            def mods = modifications[line[0]]
            if (mods != null) {
                mods << line
            }
        }
        if (modifications['?']) {
            warnOrThrow(extension.failOnUnversionedFiles, "You have ${modifications['?'].size()} un-versioned files.")
        }
        if (modifications.count { k, v -> v }) {
            def c = { count, label ->
                count ? "$count $label" : ''
            }
            def message = 'You have ' + c(modifications['A'].size(), 'added') + c(modifications['M'].size(), 'modified') +
                    c(modifications['R'].size(), 'removed')
            warnOrThrow(extension.failOnCommitNeeded, message)
        }
    }

    @Override
    void checkUpdateNeeded() {
        def modifications = ['in': [], 'out': []]
        String currentBranch = hgCurrentBranch();
        exec(['hg', 'in', '-q', '-b', currentBranch]).eachLine { line ->
            modifications['in'] << line
        }
        exec(['hg', 'out', '-q', '-b', currentBranch]).eachLine { line ->
            modifications['out'] << line
        }
        if (modifications['in']) {
            warnOrThrow(extension.failOnUpdateNeeded, "You have ${modifications['in'].size()} incoming changes")
        }
        if (modifications['out']) {
            warnOrThrow(extension.failOnPublishNeeded, "You have ${modifications['out'].size()} outgoing changes")
        }
    }

    @Override
    void createReleaseTag(String message) {
        def tagName = tagName()
        exec(['hg', 'tag', "-m", message, tagName], errorMessage: 'Error creating tag', errorPatterns: [ERROR])
    }

    @Override
    void commit(String message) {
        exec(['hg', 'ci', '-m', message], errorMessage: 'Error committing new version', errorPatterns: [ERROR])
        exec(['hg', 'push'], errorMessage: 'Error committing new version', errorPatterns: [ERROR])
    }

    void revert() {
        exec(['hg', 'revert', findPropertiesFile().name], errorMessage: 'Error reverting changes made by the release plugin.', errorPatterns: [ERROR])
    }

    private String hgCurrentBranch() {
        exec(['hg', 'branch']).readLines()[0]
    }
	
	@Override
	String assignReleaseVersionAutomatically(String currentVersion) {
		throw new GradleException("Method not implemented yet.")
	}
}
