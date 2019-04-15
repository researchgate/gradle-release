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

import org.gradle.api.Project

class TestAdapter extends BaseScmAdapter {

    TestAdapter(Project project, Map<String, Object> attributes) {
        super(project, attributes)
    }

    class TestConfig {
        String testOption = ''
    }

    @Override
    Object createNewConfig() {
        new TestConfig()
    }

    @Override
    boolean isSupported(File directory) {
        return true
    }

    @Override
    void init() {
    }

    @Override
    void checkCommitNeeded() {
    }

    @Override
    void checkUpdateNeeded() {
    }

    @Override
    void createReleaseTag(String message) {
    }

    @Override
    void commit(String message) {
    }

    @Override
    void add(File file) {
    }

    @Override
    void revert() {
    }
}
