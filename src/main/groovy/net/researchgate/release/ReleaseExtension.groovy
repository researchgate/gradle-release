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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import org.gradle.util.ConfigureUtil

import javax.swing.Action
import java.util.regex.Matcher
import java.util.regex.Pattern

class ReleaseExtension {

    @Input
    final Property<Boolean> failOnCommitNeeded = project.objects.property(Boolean.class).convention(true)

    @Input
    final Property<Boolean> failOnPublishNeeded = project.objects.property(Boolean.class).convention(true)

    @Input
    final Property<Boolean> failOnSnapshotDependencies = project.objects.property(Boolean.class).convention(true)

    @Input
    final Property<Boolean> failOnUnversionedFiles = project.objects.property(Boolean.class).convention(true)

    @Input
    final Property<Boolean> failOnUpdateNeeded = project.objects.property(Boolean.class).convention(true)

    @Input
    final Property<Boolean> revertOnFail = project.objects.property(Boolean.class).convention(true)

    @Optional
    @Input
    final Property<String> pushReleaseVersionBranch = project.objects.property(String.class)

    @Input
    final Property<String> preCommitText = project.objects.property(String.class).convention('[Gradle Release Plugin]')

    @Input
    final Property<String> preTagCommitMessage = project.objects.property(String.class).convention('pre tag commit: ')

    @Input
    final Property<String> tagCommitMessage = project.objects.property(String.class).convention('creating tag: ')

    @Input
    final Property<String> newVersionCommitMessage = project.objects.property(String.class).convention('new version commit: ')

    @Input
    final Property<String> snapshotSuffix = project.objects.property(String.class).convention('-SNAPSHOT')

    @Input
    final Property<String> tagTemplate = project.objects.property(String.class).convention('$version')

    @Input
    final Property<String> versionPropertyFile = project.objects.property(String.class).convention('gradle.properties')

    @Input
    final ListProperty<String> versionProperties = project.objects.listProperty(String.class).convention([])

    @Input
    final ListProperty<String> buildTasks = project.objects.listProperty(String.class).convention([])

    @Input
    final ListProperty<String> ignoredSnapshotDependencies = project.objects.listProperty(String.class).convention([])

    @Input
    Map<String, Closure<String>> versionPatterns = [
        // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]

    @Nested
    final GitAdapter.GitConfig git

    @Nested
    final SvnAdapter.SvnConfig svn

    @Internal
    List<Class<? extends BaseScmAdapter>> scmAdapters = [
        GitAdapter,
        SvnAdapter,
        HgAdapter,
        BzrAdapter
    ]

    @Internal
    BaseScmAdapter scmAdapter

    private Project project

    @Internal
    Map<String, Object> attributes // General plugin attributes

    ReleaseExtension(Project project, Map<String, Object> attributes) {
        this.attributes = attributes
        this.project = project
        git = new GitAdapter.GitConfig(project)
        svn = new SvnAdapter.SvnConfig(project)
    }

    void git(Closure<GitAdapter.GitConfig> closure) {
        project.configure(git, closure)
    }

    void git(org.gradle.api.Action<? super GitAdapter.GitConfig> config) {
        project.configure([git], config)
    }

    void svn(Closure<SvnAdapter.SvnConfig> closure) {
        project.configure(svn, closure)
    }

    void svn(org.gradle.api.Action<? super SvnAdapter.SvnConfig> config) {
        project.configure([svn], config)
    }
}
