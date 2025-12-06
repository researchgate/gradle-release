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

import net.researchgate.release.tasks.UnSnapshotVersion
import net.researchgate.release.tasks.UpdateVersion
import org.gradle.api.plugins.BasePlugin

import java.util.regex.Matcher

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ReleasePluginTests extends Specification {

    Project project

    File testDir = new File("build/tmp/test/${getClass().simpleName}")

    def setup() {
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        project = ProjectBuilder.builder().withName('ReleasePluginTest').withProjectDir(testDir).build()
        def testVersionPropertyFile = project.file('version.properties')
        testVersionPropertyFile.withWriter { w ->
            w.write 'version=1.2\n'
        }
        project.plugins.apply(BasePlugin.class)
        ReleasePlugin releasePlugin = project.plugins.apply(ReleasePlugin.class)
        project.extensions.release.scmAdapters = [TestAdapter]

        releasePlugin.createScmAdapter()
    }

    def 'plugin is successfully applied'() {
        expect:
        assert project.tasks.release

    }

    def 'when a custom properties file is used to specify the version'() {
        given:
        (project.extensions.release as ReleaseExtension).with {
            versionPropertyFile.set('version.properties')
        }
        (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        expect:
        project.version == '1.2'
        project.file('version.properties').text == 'version=1.2\n'
    }

    def 'version is properly unsnapshot when using default snapshot suffix'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.3-SNAPSHOT'
        }
        when:
        (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        then:
        project.version == '1.3'
    }

    def 'version is properly unsnapshot when using custom snapshot suffix'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.4-dev'
        }
        project.version = '1.4-dev'
        (project.extensions.release as ReleaseExtension).with {
            snapshotSuffix.set('-dev')
        }
        when:
        (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        then:
        project.version == '1.4'
    }

    def 'version cannot be unsnapshot when using invalid snapshot suffix'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.4-dev'
        }
        project.version = '1.4-dev'
        (project.extensions.release as ReleaseExtension).with {
            snapshotSuffix.set('-SNAPSHOT')
        }
        when:
        (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        then:
        project.version == '1.4-dev'
    }

    def 'snapshot version should be updated to new snapshot version with default snapshot suffix'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.4-SNAPSHOT'
        }
        project.version = '1.4-SNAPSHOT'
        project.properties.put('release.useAutomaticVersion', true)
        when:
        (project.tasks.updateVersion as UpdateVersion).updateVersion()
        then:
        project.version == '1.5-SNAPSHOT'
    }

    def 'snapshot version should be updated to new snapshot version with custom snapshot suffix'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.4-dev'
        }
        project.version = '1.4-dev'
        project.properties.put('release.useAutomaticVersion', true)
        (project.extensions.release as ReleaseExtension).with {
            snapshotSuffix.set('-dev')
        }
        when:
        (project.tasks.updateVersion as UpdateVersion).updateVersion()
        then:
        project.version == '1.5-dev'
    }

    def 'snapshot version should be updated to new release version'() {
        given:
        def testVersionPropertyFile = project.file('gradle.properties')
        testVersionPropertyFile.withWriter { w ->
            w.writeLine 'version=1.4-dev'
        }
        project.version = '1.4-dev'
        project.properties.put('release.useAutomaticVersion', true)
        (project.extensions.release as ReleaseExtension).with {
            snapshotSuffix.set('-dev')
        }
        when:
        (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        (project.tasks.updateVersion as UpdateVersion).updateVersion()
        then:
        project.version == '1.5-dev'
    }

    def 'version should be unsnapshot to release version'() {
        given:
            def testVersionPropertyFile = project.file('gradle.properties')
            testVersionPropertyFile.withWriter { w ->
                w.writeLine 'version=' + currentVersion
            }
            project.version = currentVersion
        when:
            (project.tasks.unSnapshotVersion as UnSnapshotVersion).unSnapshotVersion()
        then:
            project.version == expectedNewVersion
        where:
            currentVersion              | expectedNewVersion
            "1.4-SNAPSHOT"              | "1.4"
            "1.4-SNAPSHOT+meta"         | "1.4+meta"
            "1.4-SNAPSHOT+3.2.1"        | "1.4+3.2.1"
            "1.4-SNAPSHOT+rel-201908"   | "1.4+rel-201908"
    }

    def 'version should be updated to new version with default versionPatterns'() {
        given:
            def testVersionPropertyFile = project.file('gradle.properties')
            testVersionPropertyFile.withWriter { w ->
                w.writeLine 'version=' + currentVersion
            }
            project.version = currentVersion
            project.properties.put('release.useAutomaticVersion', true)
        when:
            (project.tasks.updateVersion as UpdateVersion).updateVersion()
        then:
            project.version == expectedNewVersion
        where:
            currentVersion              | expectedNewVersion
            "1.4-SNAPSHOT"              | "1.5-SNAPSHOT"
            "1.4-SNAPSHOT+meta"         | "1.5-SNAPSHOT+meta"
            "1.4+meta"                  | "1.5+meta"
            "1.4"                       | "1.5"
            "1.4.2"                     | "1.4.3"
    }

    def 'version should be updated to new version with semver based versionPatterns'() {
        given:
            def testVersionPropertyFile = project.file('gradle.properties')
            testVersionPropertyFile.withWriter { w ->
                w.writeLine 'version=' + currentVersion
            }
            project.version = currentVersion
            project.properties.put('release.useAutomaticVersion', true)
            (project.extensions.release as ReleaseExtension).with {
                versionPatterns = [
                    /(\d+)([^\d]*|[-\+].*)$/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
                ]
            }
        when:
            (project.tasks.updateVersion as UpdateVersion).updateVersion()
        then:
            project.version == expectedNewVersion
        where:
            currentVersion              | expectedNewVersion
            "1.4-SNAPSHOT"              | "1.5-SNAPSHOT"
            "1.4-SNAPSHOT+meta"         | "1.5-SNAPSHOT+meta"
            "1.4-SNAPSHOT+3.2.1"        | "1.5-SNAPSHOT+3.2.1"
            "1.4-SNAPSHOT+rel-201908"   | "1.5-SNAPSHOT+rel-201908"
            "1.4+meta"                  | "1.5+meta"
            "1.4"                       | "1.5"
            "1.4.2"                     | "1.4.3"
            "1.4.2+4.5.6"               | "1.4.3+4.5.6"
            "1.4+rel-201908"            | "1.5+rel-201908"
    }
}
