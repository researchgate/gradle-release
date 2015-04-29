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

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class BaseScmPlugin<T> extends PluginHelper implements Plugin<Project> {

	private final String pluginName = this.class.simpleName
	private T convention

	void apply(Project project) {

		this.project = project

		project.task('checkCommitNeeded', group: ReleasePlugin.RELEASE_GROUP,
				description: 'Checks to see if there are any added, modified, removed, or un-versioned files.') << this.&checkCommitNeeded
		project.task('checkUpdateNeeded', group: ReleasePlugin.RELEASE_GROUP,
				description: 'Checks to see if there are any incoming or outgoing changes that haven\'t been applied locally.') << this.&checkUpdateNeeded

        setConvention()

    }

	/**
	 * Called by {@link ReleasePlugin} when plugin's convention needs to be set.
	 */
	final void setConvention() { convention = (T) setConvention(pluginName, buildConventionInstance()) }

	/**
	 * Convenience method for sub-classes to access their own convention instance.
	 * @return this plugin convention instance.
	 */
	@SuppressWarnings('ConfusingMethodName')
	final T convention() { convention(pluginName, convention.class)}

	/**
	 * Retrieves convention instance to be set for this plugin.
	 * @return convention instance to be set for this plugin.
	 */
	abstract T buildConventionInstance()

	abstract void init()

	abstract void checkCommitNeeded()

	abstract void checkUpdateNeeded()

	abstract void createReleaseTag(String message = "")

	abstract void commit(String message)

	abstract void revert()
}
