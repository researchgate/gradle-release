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
import java.util.regex.Pattern

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

    String versionPropertyFile = 'gradle.properties'

    List versionProperties = []

    def versionPatterns = [
        // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]

    def scmAdapters = [
        GitAdapter,
        SvnAdapter,
        HgAdapter,
        BzrAdapter
    ];

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

    private Project project

    ReleaseExtension(Project project) {
        this.project = project
        ExpandoMetaClass mc = new ExpandoMetaClass(ReleaseExtension, false, true)
        mc.initialize()
        this.metaClass = mc
    }

    def propertyMissing(String name) {
        println "create new propery $name"
        BaseScmAdapter adapter = getAdapterForName(name)

        if (!adapter || !adapter.createNewConfig()) {
            throw new MissingPropertyException(name, this.class)
        }

        Object result = adapter.createNewConfig()
        this.metaClass."$name" = result
        println "created new propery $name"

        result
    }

    def methodMissing(String name, Object[] args) {
        println "create new method $name"
        Closure closure = args[0] as Closure;

        if (!this.properties[name]) {
            throw new MissingMethodException(name, this.class, args)
        }

        println "created new method $name"

        ConfigureUtil.configure(closure, this.properties[name])
    }

    private getAdapterForName(String name) {
        BaseScmAdapter adapter = null
        this.scmAdapters.find {
            Pattern pattern = Pattern.compile("^${name}", Pattern.CASE_INSENSITIVE);
            if (!pattern.matcher(it.simpleName).find()) {
                return false
            }
            assert BaseScmAdapter.isAssignableFrom(it)

            adapter = it.getConstructor(Project.class).newInstance(project)

            return true
        }

        adapter
    }
}
