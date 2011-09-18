package release

import java.util.regex.Matcher
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.GradleBuild

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin extends PluginHelper implements Plugin<Project> {

    private static final String LINE_SEP = System.getProperty( 'line.separator' )
    private static final String PROMPT   = "${LINE_SEP}??>"


    @Requires({ message })
    static String readLine ( String message, String defaultValue = null ) {
        System.console().readLine( "$PROMPT $message " + ( defaultValue ? "[$defaultValue] " : '' )) ?:
        defaultValue
    }


    @Requires({ project && newVersion })
    static void updateVersionProperty( Project project, String newVersion ) {
        File       propsFile   = project.file( 'gradle.properties' )
        assert propsFile.file, "[$propsFile.canonicalPath] wasn't found, can't update it"

        Properties gradleProps = new Properties()
        gradleProps.load(propsFile.newReader())
        gradleProps.version = newVersion
        gradleProps.store( propsFile.newWriter(), "Version updated to '${newVersion}', by Gradle release plugin." )
    }


    @Requires({ project })
    void apply( Project project ) {

        project.convention.plugins.release = new ReleasePluginConvention()

        applyScmPlugin(project)

        project.task( 'release', type: GradleBuild ) {
            // Release task should perform the following tasks.
            tasks = [
                    //  1. Check to see if source needs to be checked in.
                    'checkCommitNeeded',
                    //  2. Check to see if source is out of date
                    'checkUpdateNeeded',
                    //  3. Check for SNAPSHOT dependencies if required.
                    'checkSnapshotDependencies',
                    //  4. Build && run Unit tests
                    'build',
                    //  5. Run any other tasks the user specifies in convention.
                    releaseConvention( project ).requiredTasks,
                    //  6. Update Snapshot version if used
                    'unSnapshotVersion',
                    //  7. Commit Snapshot update (if done)
                    'preTagCommit',
                    //  8. Create tag of release.
                    'createReleaseTag',
                    //  9. Update version to next version.
                    'updateVersion',
                    // 10. Commit version update.
                    'commitNewVersion'
            ].flatten()
        }

        project.task( 'checkSnapshotDependencies' ) << { checkSnapshotDependencies( project ) }
        project.task( 'unSnapshotVersion'         ) << { unSnapshotVersion( project ) }
        project.task( 'updateVersion'             ) << { updateVersion( project ) }
    }


    def checkSnapshotDependencies( Project project ) {
        project.configurations.runtime.allDependencies.each { Dependency dependency ->
            if (dependency?.version?.contains('SNAPSHOT')) {
                def message = "Snapshot dependency detected: ${dependency.group ?: ''}:${dependency.name}:${dependency.version ?: ''}"
                if ( releaseConvention( project ).failOnSnapshotDependencies ) {
                    throw new GradleException(message)
                } else {
                    println "WARNING: $message"
                }
            }
        }
    }


    def unSnapshotVersion( Project project ) {
        def version = "${project.version}"
        if (version.contains('-SNAPSHOT')) {
            project.setProperty('usesSnapshot', true)
            version = version.replace('-SNAPSHOT', '')
            project.version = version
            updateVersionProperty(project, version)
        } else {
            project.setProperty('usesSnapshot', false)
        }
    }


    def updateVersion( Project project ) {
        def version = "${project.version}"
        Map<String, Closure> patterns = releaseConvention( project ).versionPatterns
        for (Map.Entry<String, Closure> entry: patterns) {
            String pattern = entry.key
            Closure handler = entry.value
            Matcher matcher = version =~ pattern
            if (matcher.matches()) {
                String nextVersion = handler.call(project, matcher)
                if (project.hasProperty('usesSnapshot') && project.usesSnapshot) {
                    nextVersion = "${nextVersion}-SNAPSHOT"
                }
                nextVersion = readLine('Enter the next version:', nextVersion)
                updateVersionProperty(project, nextVersion)
                return
            }
        }
    }


    /**
     * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
     * @param project
     */
    private void applyScmPlugin(Project project) {
        // apply scm tasks
        for ( name in project.rootProject.projectDir.list())
        {
            switch (name) {
                case '.svn': project.apply plugin: SvnReleasePlugin
                             return
                case '.bzr': project.apply plugin: BzrReleasePlugin
                             return
                case '.git': project.apply plugin: GitReleasePlugin
                             return
                case '.hg':  project.apply plugin: HgReleasePlugin
                             return
                default:     throw new GradleException(
                    'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
                    "[${ project.rootProject.projectDir.canonicalPath }]" )
            }
        }
    }
}