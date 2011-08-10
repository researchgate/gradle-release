package release

/**
 * @author elberry
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePluginConvention {
	boolean failOnSnapshotDependencies = true
	def requiredTasks = []
	def versionPatterns = [
			/(\d+\.(\d+))-SNAPSHOT/: {
				[tag: '', next: '']
			},
	]

	void release(Closure closure) {
		closure.delegate = this
		closure.call()
	}
}