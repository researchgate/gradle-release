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

abstract class BaseScmAdapter<C extends Cacheable> {

    protected final PluginHelper pluginHelper
    protected final C cacheableScmAdapter

    BaseScmAdapter(PluginHelper pluginHelper, C cacheableScmAdapter) {
        this.pluginHelper = pluginHelper
        this.cacheableScmAdapter = cacheableScmAdapter
    }

    abstract boolean isSupported(File directory)

    abstract void init()

    abstract void checkCommitNeeded()

    abstract void checkUpdateNeeded()

    abstract void createReleaseTag(String message)

    abstract void add(File file)

    abstract void commit(String message)

    void revert() {
        cacheableScmAdapter.revert()
    }

    void checkoutMergeToReleaseBranch() {
        throw new GradleException("Checkout and merge is supported only for GIT projects")
    }

    void checkoutMergeFromReleaseBranch() {
        throw new GradleException("Checkout and merge is supported only for GIT projects")
    }

    ReleaseExtension getExtension() {
        pluginHelper.extension
    }

    Map<String, Object> getAttributes() {
        pluginHelper.attributes
    }

    File getPropertiesFile() {
        pluginHelper.propertiesFile
    }

    String exec(Map options = [:], List<String> commands) {
        pluginHelper.exec(options, commands)
    }

    void warnOrThrow(boolean doThrow, String message) {
        pluginHelper.warnOrThrow(doThrow, message)
    }

    C toCacheable() {
        cacheableScmAdapter
    }

    /**
     * Abstract class and subclasses must be serializable to be cached by Gradle.
     *
     * <p>Mark any fields that cannot be serialized as {@code transient} and handle {@code null} values (when deserialized).
     */
    static abstract class Cacheable implements Serializable {

        protected final CacheablePluginHelper cacheablePluginHelper

        Cacheable(CacheablePluginHelper cacheablePluginHelper) {
            this.cacheablePluginHelper = cacheablePluginHelper
        }

        abstract void revert()

        File getPropertiesFile() {
            cacheablePluginHelper.propertiesFile
        }

        void exec(Map options = [:], List<String> commands) {
            cacheablePluginHelper.exec(options, commands)
        }
    }
}
