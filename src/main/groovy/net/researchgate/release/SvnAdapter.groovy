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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

import java.util.regex.Matcher
import org.gradle.api.GradleException

class SvnAdapter extends BaseScmAdapter<Cacheable> {

    private static final String ERROR = 'Commit failed'

    private static final def urlPattern = ~/URL:\s(.*?)(\/(?i)(trunk|branches|tags).*?)$/

    private static final def revPattern = ~/Revision:\s(.*?)$/

    private static final def commitPattern = ~/Committed revision\s(.*?)\.$/

    private static final def environment = [LANG: 'C', LC_MESSAGES: 'C', LC_ALL: ''];

    SvnAdapter(PluginHelper pluginHelper) {
        super(pluginHelper, new Cacheable(pluginHelper.toCacheable()))
    }

    static class SvnConfig {
        @Internal
        final Property<String> username
        @Internal
        final Property<String> password
        @Input
        final Property<Boolean> pinExternals

        SvnConfig(Project project) {
            username = project.objects.property(String.class)
            password = project.objects.property(String.class)
            pinExternals = project.objects.property(Boolean.class).convention(false)
        }
    }

    @Override
    boolean isSupported(File directory) {
        if (!directory.list().grep('.svn')) {
            return directory.parentFile? isSupported(directory.parentFile) : false
        }

        true
    }

    void init() {
        String username = findProperty('release.svn.username')
        if (username) {
            extension.svn.username.set(username)
        }

        String password = findProperty('release.svn.password')
        if (password) {
            extension.svn.password.set(password)
        }

        findSvnUrl()
        attributes.svnRev = null
    }

    @Override
    void checkCommitNeeded() {
        String out = svnExec(['status'])
        int changes = 0
        int unknown = 0
        out.eachLine { line ->
            line = line.trim()
            if (line.length() >= 2 && line.charAt(1) == ' ' as char) {
                switch (line.charAt(0)) {
                    case '?':
                        log.info('Unknown file: ' + line)
                        unknown++
                        break
                    case 'X': // ignore externals declaration
                        break
                    default:
                        log.info('Changed file: ' + line)
                        changes++
                        break
                }
            }
        }
        if (changes > 0) {
            warnOrThrow(extension.failOnCommitNeeded.get(), "You have ${changes} un-commited changes.")
        }
        if (unknown > 0) {
            warnOrThrow(extension.failOnUnversionedFiles.get(), "You have ${unknown} un-versioned files.")
        }
    }

    @Override
    void checkUpdateNeeded() {
        String svnRev = attributes.initialSvnRev

        String out = svnExec(['status', '-q', '-u'])
        int missing = 0
        out.eachLine { line ->
            line = line.trim()
            if (line.length() >= 2 && line.charAt(1) == ' ' as char) {
                switch (line.charAt(0)) {
                    case '*':
                        missing++
                        break
                }
            }
        }
        if (missing > 0) {
            warnOrThrow(extension.failOnUpdateNeeded.get(), "You are missing ${missing} changes.")
        }

        out = svnExec(['info', attributes.svnUrl as String])
        out.eachLine { line ->
            Matcher matcher = line =~ revPattern
            if (matcher.matches()) {
                attributes.remoteSvnRev = matcher.group(1)
            }
        }
        if (svnRev != attributes.remoteSvnRev) {
            // warn that there's a difference in local revision versus remote
            warnOrThrow(extension.failOnUpdateNeeded.get(), "Local revision (${svnRev}) does not match remote (${attributes.remoteSvnRev}), local revision is used in tag creation.")
        }
    }

    @Override
    void createReleaseTag(String message) {
        String svnUrl = attributes.svnUrl
        String svnRev = attributes.svnRev ?: attributes.initialSvnRev //release set by commit below when needed, no commit => initial
        String svnRoot = attributes.svnRoot
        String svnTag = tagName()

        List<String> commands = ['copy', "${svnUrl}@${svnRev}", "${svnRoot}/tags/${svnTag}", '--parents', '-m', message]
        if (extension.svn.pinExternals.get()) {
            commands += '--pin-externals'
        }
        svnExec(commands)
    }

    @Override
    void commit(String message) {
        String out = svnExec(['commit', '-m', message], errorMessage: 'Error committing new version', errorPatterns: [ERROR])

        // After the first commit we need to find the new revision so the tag is made from the correct revision
        if (!attributes.svnRev) {
            out.eachLine { line ->
                Matcher matcher = line =~ commitPattern
                if (matcher.matches()) {
                    attributes.svnRev = matcher.group(1)
                }
            }
        }
    }

    @Override
    void add(File file) {
        svnExec(['add', file.path], errorMessage: "Error adding file ${file.name}", errorPatterns: ['warning:'])
    }

    /**
     * Adds the executable and optional also username/password
     *
     * @param options
     * @param commands
     * @return
     */
    private String svnExec(
        Map options = [:],
        List<String> commands
    ) {
        if (extension.svn.username.isPresent()) {
            if (extension.svn.password.isPresent()) {
                commands.addAll(0, ['--password', extension.svn.password.get()]);
            }
            commands.addAll(0, ['--non-interactive', '--no-auth-cache', '--username', extension.svn.username.get()]);
        }
        commands.add(0, 'svn');

        options['env'] = environment;

        exec(options, commands)
    }

    private void findSvnUrl() {
        String out = svnExec(['info'])

        out.eachLine { line ->
            Matcher matcher = line =~ urlPattern
            if (matcher.matches()) {
                String svnRoot = matcher.group(1)
                String svnProject = matcher.group(2)
                attributes.svnRoot = svnRoot
                attributes.svnUrl = "$svnRoot$svnProject"
            }
            matcher = line =~ revPattern
            if (matcher.matches()) {
                attributes.initialSvnRev = matcher.group(1)
            }
        }
        if (!attributes.svnUrl || !attributes.initialSvnRev) {
            throw new GradleException('Could not determine root SVN url or revision.')
        }
    }

    static class Cacheable extends BaseScmAdapter.Cacheable {

        Cacheable(CacheablePluginHelper cacheablePluginHelper) {
            super(cacheablePluginHelper)
        }

        @Override
        void revert() {
            svnExec(['revert', propertiesFile.name], errorMessage: 'Error reverting changes made by the release plugin.', errorPatterns: [ERROR])
        }
    }
}
