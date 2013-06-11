package release

import java.util.regex.Matcher
import org.gradle.api.Project

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
    boolean pushChanges = true  // push changes to remote repo (supported only in DVCS)
	String preCommitText = "" // good place for code review overrides and ticket numbers
	String preTagCommitMessage = "[Gradle Release Plugin] - pre tag commit: "
	String tagCommitMessage = "[Gradle Release Plugin] - creating tag: "
	String newVersionCommitMessage = "[Gradle Release Plugin] - new version commit: "

	/**
	 * If true, tag names and messages will include the project name (e.g. project-name-version)
	 * otherwise only version is used.
	 */
	boolean includeProjectNameInTag = false

	def requiredTasks = []
	def versionPatterns = [
			// Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
			/(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${ (m[0][1] as int) + 1 }${ m[0][2] }") }
	]

	def git = new GitReleasePluginConvention()

    String versionPropertyFile = 'gradle.properties'
    def versionProperties = []
    String tagPrefix = null

	void release(Closure closure) {
		closure.delegate = this
		closure.call()
	}
}
