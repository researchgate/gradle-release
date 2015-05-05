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

import java.util.regex.Matcher
import org.gradle.api.Project

class ReleasePluginConvention {

	boolean failOnCommitNeeded = true
	boolean failOnPublishNeeded = true
	boolean failOnSnapshotDependencies = true
	boolean failOnUnversionedFiles = true
	boolean failOnUpdateNeeded = true
	boolean revertOnFail = true // will use the SCM plugin to revert any uncommitted changes in the project.
	String preCommitText = "" // good place for code review overrides and ticket numbers
	String preTagCommitMessage = "[Gradle Release Plugin] - pre tag commit: "
	String tagCommitMessage = "[Gradle Release Plugin] - creating tag: "
	String newVersionCommitMessage = "[Gradle Release Plugin] - new version commit: "

	/**
	 * @deprecated to be removed in 3.0 see tagTemplate
	 */
    @Deprecated
	boolean includeProjectNameInTag = false

    /**
     * @deprecated to be removed in 3.0 see tagTemplate
     */
    @Deprecated
    String tagPrefix = null

    /**
     * Custom template for the tag
     * Possible template variables are $version and $name
     * example "$name-$version" -> "myproject-1.1"
     *
     * as of 3.0 set this to "$version" by default
     */
    String tagTemplate = null

	def requiredTasks = []
	def versionPatterns = [
			// Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
			/(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${ (m[0][1] as int) + 1 }${ m[0][2] }") }
	]

	def git = new GitReleasePluginConvention()

    String versionPropertyFile = 'gradle.properties'
    def versionProperties = []

	Class<BaseScmPlugin> customScmPlugin = null

	void release(Closure closure) {
		closure.delegate = this
		closure.call()
	}
}
