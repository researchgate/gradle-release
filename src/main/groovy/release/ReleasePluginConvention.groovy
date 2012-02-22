package release

import java.util.regex.Matcher

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePluginConvention {

	boolean failOnCommitNeeded = true
	boolean failOnPublishNeeded = true
	boolean failOnSnapshotDependencies = true
	boolean failOnUnversionedFiles = true
	boolean failOnUpdateNeeded = true
	boolean revertOnFail = true // will use the SCM plugin to revert any uncommitted changes in the project.
	String preTagCommitMessage = 'Gradle Release Plugin - pre tag commit: '
	String newVersionCommitMessage = 'Gradle Release Plugin - new version commit: '

	/**
	 * If true, tag names and messages will include the project name (e.g. project-name-version)
	 * otherwise only version is used.
	 */
	boolean includeProjectNameInTag = false

	def requiredTasks = []
	def versionPatterns = [
			// Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
			/(\d+)([^\d]*$)/: { Matcher m -> m.replaceAll("${ (m[0][1] as int) + 1 }${ m[0][2] }") }
	]

	void release(Closure closure) {
		closure.delegate = this
		closure.call()
	}
}