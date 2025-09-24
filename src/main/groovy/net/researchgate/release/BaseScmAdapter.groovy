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
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseScmAdapter<C extends Cacheable> {

    protected final PluginHelper pluginHelper
    protected final C cacheableScmAdapter

    protected final Logger log

    BaseScmAdapter(PluginHelper pluginHelper, C cacheableScmAdapter) {
        this.pluginHelper = pluginHelper
        this.cacheableScmAdapter = cacheableScmAdapter
        log = pluginHelper.project.logger ?: LoggerFactory.getLogger(this.class)
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

    String findProperty(String key, Object defaultVal = null, String deprecatedKey = null) {
        pluginHelper.findProperty(key, defaultVal, deprecatedKey)
    }

    String tagName() {
        pluginHelper.tagName()
    }

    void updateVersionProperty(String newVersion) {
        pluginHelper.updateVersionProperty(newVersion)
    }

    void warnOrThrow(boolean doThrow, String message) {
        pluginHelper.warnOrThrow(doThrow, message)
    }

    C toCacheable() {
        cacheableScmAdapter
    }

    /**
     * Serializable to be cached by Gradle.
     */
    @CompileStatic
    static abstract class Cacheable implements Externalizable {

        PluginHelper.Cacheable cacheablePluginHelper

        @Override
        void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.writeObject(cacheablePluginHelper)
        }

        @Override
        void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
            cacheablePluginHelper = objectInput.readObject() as PluginHelper.Cacheable
        }

        abstract void revert()

        File getPropertiesFile() {
            cacheablePluginHelper.propertiesFile
        }

        String exec(Map options = [:], List<String> commands) {
            cacheablePluginHelper.exec(options, commands)
        }
    }
}
