package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Base class for all SCM-specific plugins
 * @author evgenyg
 */
abstract class BaseScmPlugin<T> extends PluginHelper implements Plugin<Project> {

	private final String pluginName = this.class.simpleName
	private T convention

	final void apply(Project project) {

		this.project = project

		project.task('checkCommitNeeded',
				description: 'Checks to see if there are any added, modified, removed, or un-versioned files.') << this.&checkCommitNeeded
		project.task('checkUpdateNeeded',
				description: 'Checks to see if there are any incoming or outgoing changes that haven\'t been applied locally.') << this.&checkUpdateNeeded
		project.task('createReleaseTag',
				description: 'Creates a tag in SCM for the current (un-snapshotted) version.') << this.&createReleaseTag

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

	abstract void createReleaseTag()

	abstract void commit(String message)
}
