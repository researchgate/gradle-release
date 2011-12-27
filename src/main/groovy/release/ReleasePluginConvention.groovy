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
	String preTagCommitMessage = 'Gradle Release Plugin - pre tag commit: '
	String newVersionCommitMessage = 'Gradle Release Plugin - new version commit: '
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