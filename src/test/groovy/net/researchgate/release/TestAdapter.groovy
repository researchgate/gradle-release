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

import groovy.transform.CompileStatic

class TestAdapter extends BaseScmAdapter {

    TestAdapter(PluginHelper pluginHelper) {
        super(pluginHelper, new Cacheable(cacheablePluginHelper: pluginHelper.toCacheable()))
    }

    class TestConfig {
        String testOption = ''
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

    @CompileStatic
    static class Cacheable extends BaseScmAdapter.Cacheable {

        @Override
        void revert() {
        }
    }
}
