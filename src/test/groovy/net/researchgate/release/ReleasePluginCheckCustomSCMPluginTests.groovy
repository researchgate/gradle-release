package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

public class ReleasePluginCheckCustomSCMPluginTests extends Specification {

    def project

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").build()
        project.apply plugin: ReleasePlugin
        project.release {
            customScmPlugin = NoSCMReleasePlugin
        }
        project.findScmPlugin.execute()
    }

    def 'when setting a customScmPlugin it should be accepted'() {
        when:
        project.createReleaseTag.execute()
        then:
        notThrown GradleException
    }
}
