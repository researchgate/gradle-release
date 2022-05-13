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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.util.ConfigureUtil

import java.util.regex.Matcher
import java.util.regex.Pattern

class ReleaseExtension {

    Property<Boolean> failOnCommitNeeded = project.objects.property(Boolean.class).convention(true)

    Property<Boolean> failOnPublishNeeded = project.objects.property(Boolean.class).convention(true)

    Property<Boolean> failOnSnapshotDependencies = project.objects.property(Boolean.class).convention(true)

    Property<Boolean> failOnUnversionedFiles = project.objects.property(Boolean.class).convention(true)

    Property<Boolean> failOnUpdateNeeded = project.objects.property(Boolean.class).convention(true)

    Property<Boolean> revertOnFail = project.objects.property(Boolean.class).convention(true)

    Property<String> pushReleaseVersionBranch = project.objects.property(String.class)

    Property<String> preCommitText = project.objects.property(String.class).convention('')

    Property<String> preTagCommitMessage = project.objects.property(String.class).convention('[Gradle Release Plugin] - pre tag commit: ')

    Property<String> tagCommitMessage = project.objects.property(String.class).convention('[Gradle Release Plugin] - creating tag: ')

    Property<String> newVersionCommitMessage = project.objects.property(String.class).convention('[Gradle Release Plugin] - new version commit: ')

    Property<String> snapshotSuffix = project.objects.property(String.class).convention('-SNAPSHOT')

    Property<String> tagTemplate = project.objects.property(String.class).convention('$version')

    Property<String> versionPropertyFile = project.objects.property(String.class).convention('gradle.properties')

    ListProperty<String> versionProperties = project.objects.listProperty(String.class).convention([])

    ListProperty<String> buildTasks = project.objects.listProperty(String.class).convention([])

    ListProperty<String> ignoredSnapshotDependencies = project.objects.listProperty(String.class).convention([])

    Map<String, Closure<String>> versionPatterns = [
        // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]

    List<Class<? extends BaseScmAdapter>> scmAdapters = [
        GitAdapter,
        SvnAdapter,
        HgAdapter,
        BzrAdapter
    ]

    BaseScmAdapter scmAdapter

    private Project project

    Map<String, Object> attributes // General plugin attributes

    ReleaseExtension(Project project, Map<String, Object> attributes) {
        this.attributes = attributes
        this.project = project
        ExpandoMetaClass mc = new ExpandoMetaClass(ReleaseExtension, false, true)
        mc.initialize()
        metaClass = mc
    }

    def propertyMissing(String name) {
        BaseScmAdapter adapter = getAdapterForName(name)
        Object result = adapter?.createNewConfig()

        if (!adapter || !result) {
            throw new MissingPropertyException(name, this.class)
        }

        metaClass."$name" = result
    }

    def propertyMissing(String name, value) {
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

    private BaseScmAdapter getAdapterForName(String name) {
        BaseScmAdapter adapter = null
        scmAdapters.find {
            assert BaseScmAdapter.isAssignableFrom(it)

            Pattern pattern = Pattern.compile("^${name}", Pattern.CASE_INSENSITIVE)



            if (!pattern.matcher(it.simpleName).find()) {
                return false
            }



            adapter = it.getConstructor(Project.class, Map.class).newInstance(project, attributes)

            return true
        }

        adapter
    }
}
