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

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import java.util.regex.Matcher

class ReleaseExtension {

    boolean failOnCommitNeeded = true

    boolean failOnPublishNeeded = true

    boolean failOnSnapshotDependencies = true

    boolean failOnUnversionedFiles = true

    boolean failOnUpdateNeeded = true

    boolean revertOnFail = true

    String preCommitText = ""

    String preTagCommitMessage = "[Gradle Release Plugin] - pre tag commit: "

    String tagCommitMessage = "[Gradle Release Plugin] - creating tag: "

    String newVersionCommitMessage = "[Gradle Release Plugin] - new version commit: "

    /**
     * as of 3.0 set this to "$version" by default
     */
    String tagTemplate

    GitConfig git = new GitConfig()

    SvnConfig svn = new SvnConfig()

    String versionPropertyFile = 'gradle.properties'

    List versionProperties = []

    def versionPatterns = [
        // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]

    def git(Closure closure) {
        ConfigureUtil.configure(closure, git)
    }

    def svn(Closure closure) {
        ConfigureUtil.configure(closure, svn)
    }

    /**
     * @deprecated to be removed in 3.0 see tagTemplate
     */
    @Deprecated
    boolean includeProjectNameInTag = false

    /**
     * @deprecated to be removed in 3.0 see tagTemplate
     */
    @Deprecated
    String tagPrefix

    class SvnConfig {
        String username
        String password
    }

    class GitConfig {
        String requireBranch = 'master'
        String pushToRemote = 'origin'
        boolean pushToCurrentBranch = false
    }
}
