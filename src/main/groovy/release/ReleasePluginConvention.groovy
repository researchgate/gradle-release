package release

import java.util.regex.Matcher
import org.gradle.api.Project

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePluginConvention {
    boolean failOnSnapshotDependencies  = true
    boolean failOnUnversionedFiles      = true
    String  preTagCommitMessage         = 'Gradle Release Plugin - pre tag commit.'
    String  tagCommitMessage            = 'Gradle Release Plugin - tagging commit.'
    String  newVersionCommitMessage     = 'Gradle Release Plugin - new version commit.'
    def     requiredTasks               = []
    def     versionPatterns             = [
            /(.*[^\d])(\d*)/: {
                Project project, Matcher matcher ->
                int lastDigit = matcher.group(2) as int
                matcher.replaceAll( "\$1${lastDigit + 1}" )
            },
            /(\d+)/: {
                Project project, Matcher matcher ->
                int lastDigit = matcher.group(1) as int
                ( lastDigit + 1 ) as String
            }
    ]

    void release(Closure closure) {
        closure.delegate = this
        closure.call()
    }
}