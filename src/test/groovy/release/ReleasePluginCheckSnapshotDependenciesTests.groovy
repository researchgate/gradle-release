package release

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


public class ReleasePluginCheckSnapshotDependenciesTests extends Specification {
    def project

    def setup() {
        project = ProjectBuilder.builder().withName("ReleasePluginTest").build()
        project.apply plugin: 'groovy'
        project.apply plugin: TestReleasePlugin
    }

    def 'when no deps then no exception'() {
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        notThrown GradleException
    }

    def 'when no SNAPSHOT deps then no exception'() {
        given:
        project.configurations {custom}
        project.dependencies {
            compile 'my1:my1:1.1.1'
            custom 'my2:my2:1.1.1'
        }
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        notThrown GradleException
    }

    def 'when SNAPSHOT in plugin provide cfg then exception'() {
        given:
        project.dependencies {compile 'my:my:1.1.1-SNAPSHOT'}
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        GradleException ex = thrown()
        ex.cause.message =~ /my:my:1.1.1-SNAPSHOT/
    }

    def 'when SNAPSHOT in custom deps then exception'() {
        given:
        project.configurations {custom}
        project.dependencies {custom 'my:my:1.1.1-SNAPSHOT'}
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        GradleException ex = thrown()
        ex.cause.message =~ /my:my:1.1.1-SNAPSHOT/
    }

    def 'when SNAPSHOT in subprojects then exception'() {
        given:
        def proj1 = ProjectBuilder.builder().withParent(project).withName("proj1").build()
        proj1.apply plugin: 'java'
        proj1.dependencies {compile 'my1:my1:1.1.1-SNAPSHOT'}

        def proj2 = ProjectBuilder.builder().withParent(project).withName("proj2").build()
        proj2.apply plugin: 'java'
        proj2.dependencies {compile 'my2:my2:1.1.1'}

        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains 'proj1: [my1:my1:1.1.1-SNAPSHOT]'
        !ex.cause.message.contains('my2:my2:1.1.1')
    }

    def 'when same SNAPSHOT in several configurations then show one in exception'() {
        given:
        project.dependencies {
            compile 'my:my:1.1.1-SNAPSHOT'
            runtime 'my:my:1.1.1-SNAPSHOT'
        }
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains '[my:my:1.1.1-SNAPSHOT]'
    }

    def 'when few SNAPSHOT deps in several configurations then show all in exception'() {
        given:
        project.dependencies {
            compile 'my1:my1:1.1.1-SNAPSHOT'
            runtime 'my2:my2:1.1.1-SNAPSHOT'
        }
        when:
        project.tasks['checkSnapshotDependencies'].execute()
        then:
        GradleException ex = thrown()
        ex.cause.message.contains '[my1:my1:1.1.1-SNAPSHOT, my2:my2:1.1.1-SNAPSHOT]'
    }
}
