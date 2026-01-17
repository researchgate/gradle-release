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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.util.regex.Matcher

class GitAdapter extends BaseScmAdapter {

    private static final String LINE = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'

    private static final String UNCOMMITTED = 'uncommitted'
    private static final String UNVERSIONED = 'unversioned'
    private static final String AHEAD = 'ahead'
    private static final String BEHIND = 'behind'

    private String workingBranch
    private String releaseBranch

    private File workingDirectory

    static class GitConfig {

        @Input
        final Property<String> requireBranch
        @Optional
        @Input
        ListProperty<String> commitOptions
        @Optional
        @Input
        final Property<Object> pushToRemote
        @Optional
        @Input
        ListProperty<String> pushOptions
        @Input
        final Property<Boolean> signTag
        @Optional
        @Input
        final Property<String> pushToBranchPrefix
        @Input
        final Property<Boolean> commitVersionFileOnly

        GitConfig(Project project) {
            requireBranch = project.objects.property(String.class).convention('main')
            commitOptions = project.objects.listProperty(String.class).convention([])
            pushToRemote = project.objects.property(Object.class).convention('origin')
            pushOptions = project.objects.listProperty(String.class).convention([])
            signTag = project.objects.property(Boolean.class).convention(false)
            pushToBranchPrefix = project.objects.property(String)
            commitVersionFileOnly = project.objects.property(Boolean.class).convention(false)
        }
    }

    GitAdapter(Project project, Map<String, Object> attributes) {
        super(project, attributes)
    }

    @Override
    boolean isSupported(File directory) {
        if (!directory.list().grep('.git')) {
            return directory.parentFile? isSupported(directory.parentFile) : false
        }

        workingDirectory = directory
        true
    }

    @Override
    void init() {
        workingBranch = gitCurrentBranch()
        if (extension.pushReleaseVersionBranch.isPresent()) {
            releaseBranch = extension.pushReleaseVersionBranch.get()
        } else {
            releaseBranch = workingBranch
        }
        if (extension.git.requireBranch.isPresent() && !extension.git.requireBranch.get().isEmpty()) {
            if (!(workingBranch ==~ extension.git.requireBranch.get())) {
                throw new GradleException("Current Git branch is \"$workingBranch\" and not \"${ extension.git.requireBranch.get() }\".")
            }
        }
    }

    @Override
    void checkCommitNeeded() {
        def status = gitStatus()

        if (status[UNVERSIONED]) {
            warnOrThrow(extension.failOnUnversionedFiles.get(),
                    (['You have unversioned files:', LINE, * status[UNVERSIONED], LINE] as String[]).join('\n'))
        }

        if (status[UNCOMMITTED]) {
            warnOrThrow(extension.failOnCommitNeeded.get(),
                    (['You have uncommitted files:', LINE, * status[UNCOMMITTED], LINE] as String[]).join('\n'))
        }
    }

    @Override
    void checkUpdateNeeded() {
        exec(['git', 'remote', 'update'], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])

        def status = gitRemoteStatus()

        if (status[AHEAD]) {
            warnOrThrow(extension.failOnPublishNeeded.get(), "You have ${status[AHEAD]} local change(s) to push.")
        }

        if (status[BEHIND]) {
            warnOrThrow(extension.failOnUpdateNeeded.get(), "You have ${status[BEHIND]} remote change(s) to pull.")
        }
    }

    @Override
    void createReleaseTag(String message) {
        def tagName = tagName()
        def params = ['git', 'tag', '-a', tagName, '-m', message]
        if (extension.git.signTag.get()) {
            params.add('-s')
        }
        exec(params, directory: workingDirectory, errorMessage: "Duplicate tag [$tagName] or signing error", errorPatterns: ['already exists', 'failed to sign'])
        if (shouldPush()) {
            exec(['git', 'push', '--porcelain', extension.git.pushToRemote.get().toString(), tagName] + extension.git.pushOptions.get(), directory: workingDirectory, errorMessage: "Failed to push tag [$tagName] to remote", errorPatterns: ['[rejected]', 'error: ', 'fatal: '])
        }
    }

    @Override
    void commit(String message) {
        List<String> command = ['git', 'commit', '-m', message]
        if (extension.git.commitVersionFileOnly.get()) {
            command << project.file(extension.versionPropertyFile.get())
        } else {
            command << '-a'
        }
        command += extension.git.commitOptions.get()

        exec(command, directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])

        if (shouldPush()) {
            def branch = gitCurrentBranch()
            if (extension.git.pushToBranchPrefix.isPresent()) {
                branch = "HEAD:${extension.git.pushToBranchPrefix.get()}${branch}"
            }
            exec(['git', 'push', '--porcelain', extension.git.pushToRemote.get().toString(), branch] + extension.git.pushOptions.get(), directory: workingDirectory, errorMessage: 'Failed to push to remote', errorPatterns: ['[rejected]', 'error: ', 'fatal: '])
        }
    }

    @Override
    void add(File file) {
        exec(['git', 'add', file.path], directory: workingDirectory, errorMessage: "Error adding file ${file.name}", errorPatterns: ['error: ', 'fatal: '])
    }

    @Override
    void revert() {
        // Revert changes on gradle.properties
        exec(['git', 'checkout', findPropertiesFile().name], directory: workingDirectory, errorMessage: 'Error reverting changes made by the release plugin.')
    }

    @Override
    void checkoutMergeToReleaseBranch() {
        checkoutMerge(workingBranch, releaseBranch)
    }

    @Override
    void checkoutMergeFromReleaseBranch() {
        checkoutMerge(releaseBranch, workingBranch)
    }

    private checkoutMerge(String fromBranch, String toBranch) {
        exec(['git', 'fetch'], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])
        exec(['git', 'checkout', toBranch], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])
        exec(['git', 'merge', '--no-ff', '--no-commit', fromBranch], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: ', 'CONFLICT'])
    }

    private boolean shouldPush() {
        def shouldPush = false
        if (extension.git.pushToRemote.get()) {
            exec(['git', 'remote'], directory: workingDirectory).eachLine { line ->
                Matcher matcher = line =~ ~/^\s*(.*)\s*$/
                if (matcher.matches() && matcher.group(1) == extension.git.pushToRemote.get()) {
                    shouldPush = true
                }
            }
            if (!shouldPush && extension.git.pushToRemote.get() != 'origin') {
                throw new GradleException("Could not push to remote ${extension.git.pushToRemote.get()} as repository has no such remote")
            }
        }

        shouldPush
    }

    private String gitCurrentBranch() {
        def matches = exec(['git', 'branch', '--no-color', '--no-column'], directory: workingDirectory).readLines().grep(~/\s*\*.*/)
        if (!matches.isEmpty()) {
            matches[0].trim() - (~/^\*\s+/)
        } else {
            throw new GradleException('Error, this repository is empty.')
        }
    }

    private Map<String, List<String>> gitStatus() {
        exec(['git', 'status', '--porcelain'], directory: workingDirectory).readLines().groupBy {
            if (it ==~ /^\s*\?{2}.*/) {
                UNVERSIONED
            } else {
                UNCOMMITTED
            }
        }
    }

    private Map<String, Integer> gitRemoteStatus() {
        def branchStatus = exec(['git', 'status', '--porcelain', '-b'], directory: workingDirectory).readLines()[0]
        def aheadMatcher = branchStatus =~ /.*ahead (\d+).*/
        def behindMatcher = branchStatus =~ /.*behind (\d+).*/

        def remoteStatus = [:]

        if (aheadMatcher.matches()) {
            remoteStatus[AHEAD] = aheadMatcher[0][1]
        }
        if (behindMatcher.matches()) {
            remoteStatus[BEHIND] = behindMatcher[0][1]
        }
        remoteStatus
    }
}
