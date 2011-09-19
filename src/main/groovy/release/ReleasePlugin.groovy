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


    @Requires({ project })
    void apply( Project project ) {

        project.convention.plugins.release = new ReleasePluginConvention()

        checkPropertiesFile( project )
        applyScmPlugin( project )

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

        def snapshotDependencies = project.configurations.runtime.allDependencies.
                                   findAll { Dependency d -> d.version?.contains( 'SNAPSHOT' )}.
                                   collect { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

        if ( snapshotDependencies ) {
            def message = "Snapshot dependencies detected: $snapshotDependencies"
            if ( releaseConvention( project ).failOnSnapshotDependencies ) {
                throw new GradleException( message )
            }
            else {
                println "WARNING: $message"
            }
        }
    }


    def unSnapshotVersion( Project project ) {
        def version = project.version.toString()

        if ( version.contains( '-SNAPSHOT' )) {
            project.setProperty( 'usesSnapshot', true )
            version -= '-SNAPSHOT'
            project.version = version
            updateVersionProperty( project, version )
        }
        else {
            project.setProperty( 'usesSnapshot', false )
        }
    }


    def updateVersion( Project project ) {
        def version = project.version.toString()
        Map<String, Closure> patterns = releaseConvention( project ).versionPatterns

        for ( Map.Entry<String, Closure> entry in patterns ) {

            String pattern  = entry.key
            //noinspection GroovyUnusedAssignment
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if ( matcher.find()) {
                String nextVersion = handler( matcher )
                if ( project.hasProperty( 'usesSnapshot' ) && project.usesSnapshot ) {
                    nextVersion += '-SNAPSHOT'
                }
                nextVersion = readLine( 'Enter the next version:', nextVersion )
                updateVersionProperty( project, nextVersion )
                return
            }
        }

        throw new GradleException( "Failed to increase version [$version] - unknown pattern" )
    }


    def checkPropertiesFile( Project project ) {
        File       propertiesFile   = project.file( 'gradle.properties' )
        assert propertiesFile.file, "[$propertiesFile.canonicalPath] not found, create it and specify version = ..."

        Properties properties = new Properties()
        propertiesFile.withReader { properties.load( it ) }

        assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
        assert releaseConvention( project ).versionPatterns.keySet().any { ( properties.version =~ it ).find() }, \
               "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
               releaseConvention( project ).versionPatterns.keySet()
    }


    /**
     * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
     * @param project
     */
    private void applyScmPlugin(Project project) {
        // apply scm tasks
        for ( name in project.rootProject.projectDir.list()) {
            switch (name) {
                case '.svn': project.apply plugin: SvnReleasePlugin
                             return
                case '.bzr': project.apply plugin: BzrReleasePlugin
                             return
                case '.git': project.apply plugin: GitReleasePlugin
                             return
                case '.hg':  project.apply plugin: HgReleasePlugin
                             return
            }
        }

        throw new GradleException(
            'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
            "[${ project.rootProject.projectDir.canonicalPath }]" )
    }
}