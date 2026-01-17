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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.util.regex.Matcher

/**
 * This ManualAdapter does not interact with any SCM. Instead, it informs the user of manual steps to be executed.
 */
class ManualAdapter extends BaseScmAdapter {

    private File workingDirectory

    ManualAdapter(Project project, Map<String, Object> attributes) {
        super(project, attributes)
    }

    // The ManualAdapter is always supported.
    @Override
    boolean isSupported(File directory) {
        true
    }

    @Override
    void init() {
    }

    @Override
    void checkCommitNeeded() {
        if (!promptYesOrNo('Have all modified files been committed?', true)) {
            warnOrThrow(true, 'You have uncommitted changes.')
        }
    }

    @Override
    void checkUpdateNeeded() {
        if (!promptYesOrNo('Have all remote modifications been pulled, and local changes pushed?', true)) {
            warnOrThrow(true, 'You have unmerged remote changes.')
        }
    }

    @Override
    void createReleaseTag(String message) {
        def tagName = tagName()
        if (!promptYesOrNo("You should now tag the release. Suggested name: [$tagName]. Did you create the tag?", true)) {
            warnOrThrow(true, 'You did not create a release tag.')
        }
    }

    @Override
    void commit(String message) {
        if (!promptYesOrNo("You should now commit the changes. Suggested commit message: [$message]. Did you commit the changes?", true)) {
            warnOrThrow(true, 'You did not commit the changes.')
        }
    }

    @Override
    void add(File file) {
    }

    @Override
    void revert() {
        def pptsFile = findPropertiesFile().name
        println "You should revert the changes to [$pptsFile]"
    }
}
