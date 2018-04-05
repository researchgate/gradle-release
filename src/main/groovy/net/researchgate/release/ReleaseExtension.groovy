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

    String preCommitText = ''

    String preTagCommitMessage = '[Gradle Release Plugin] - pre tag commit: '

    String tagCommitMessage = '[Gradle Release Plugin] - creating tag: '

    String newVersionCommitMessage = '[Gradle Release Plugin] - new version commit: '

    /**
     * as of 3.0 set this to "$version" by default
     */
    String tagTemplate

    String versionPropertyFile = 'gradle.properties'

    List versionProperties = []

    List buildTasks = ['build']
	
    List ignoredSnapshotDependencies = []

    Map<String, Closure<String>> versionPatterns = [
        // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]

    List<Class<? extends BaseScmAdapter>> scmAdapters = [
        GitAdapter,
        SvnAdapter,
        HgAdapter,
        BzrAdapter
    ];

    private Project project
    private Map<String, Object> attributes

    ReleaseExtension(Project project, Map<String, Object> attributes) {
        this.attributes = attributes
        this.project = project
        ExpandoMetaClass mc = new ExpandoMetaClass(ReleaseExtension, false, true)
        mc.initialize()
        metaClass = mc
    }

    def propertyMissing(String name) {
        if (isDeprecatedOption(name)) {
            def value = null
            if (name == 'includeProjectNameInTag') {
                value = false
            }

            return metaClass."$name" = value
        }
        BaseScmAdapter adapter = getAdapterForName(name)
        Object result = adapter?.createNewConfig()

        if (!adapter || !result) {
            throw new MissingPropertyException(name, this.class)
        }

        metaClass."$name" = result
    }

    def propertyMissing(String name, value) {
        if (isDeprecatedOption(name)) {
            project.logger?.warn("You are setting the deprecated option '${name}'. The deprecated option will be removed in 3.0")
            project.logger?.warn("Please upgrade your configuration to use 'tagTemplate'. See https://github.com/researchgate/gradle-release/blob/master/UPGRADE.md#migrate-to-new-tagtemplate-configuration")

            return metaClass."$name" = value
        }
        BaseScmAdapter adapter = getAdapterForName(name)

        if (!adapter) {
            throw new MissingPropertyException(name, this.class)
        }
        metaClass."$name" = value
    }

    def methodMissing(String name, args) {
        metaClass."$name" = { Closure varClosure ->
            return ConfigureUtil.configure(varClosure, this."$name")
        }

        try {
            return ConfigureUtil.configure(args[0] as Closure, this."$name")
        } catch (MissingPropertyException ignored) {
            throw new MissingMethodException(name, this.class, args)
        }
    }

    private boolean isDeprecatedOption(String name) {
        name == 'includeProjectNameInTag' || name == 'tagPrefix'
    }

    private BaseScmAdapter getAdapterForName(String name) {
        BaseScmAdapter adapter = null
        scmAdapters.find {
            assert BaseScmAdapter.isAssignableFrom(it)

            Pattern pattern = Pattern.compile("^${name}", Pattern.CASE_INSENSITIVE);
            if (!pattern.matcher(it.simpleName).find()) {
                return false
            }

            adapter = it.getConstructor(Project.class, Map.class).newInstance(project, attributes)

            return true
        }

        adapter
    }
}
