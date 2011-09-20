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

    private Class<? extends BaseScmPlugin> scmPluginClass


    @Requires({ project })
    void apply( Project project ) {

        this.project = project
        project.convention.plugins.release = new ReleasePluginConvention()

        checkPropertiesFile()
        this.scmPluginClass = applyScmPlugin()

        project.task( 'release', type: GradleBuild ) {
            // Release task should perform the following tasks.
            tasks = [
                    //  1. (SCM Plugin) Check to see if source needs to be checked in.
                    'checkCommitNeeded',
                    //  2. (SCM Plugin) Check to see if source is out of date
                    'checkUpdateNeeded',
                    //  3. (This Plugin) Check for SNAPSHOT dependencies if required.
                    'checkSnapshotDependencies',
                    //  4. (This Plugin) Build && run Unit tests
                    'build',
                    //  5. (This Plugin) Run any other tasks the user specifies in convention.
                    releaseConvention().requiredTasks,
                    //  6. (This Plugin) Update Snapshot version if used
                    'unSnapshotVersion',
                    //  7. (This Plugin) Commit Snapshot update (if done)
                    'preTagCommit',
                    //  8. (SCM Plugin) Create tag of release.
                    'createReleaseTag',
                    //  9. (This Plugin) Update version to next version.
                    'updateVersion',
                    // 10. (This Plugin) Commit version update.
                    'commitNewVersion'
            ].flatten()
        }

        project.task( 'checkSnapshotDependencies' ) << { checkSnapshotDependencies() }
        project.task( 'unSnapshotVersion'         ) << { unSnapshotVersion() }
        project.task( 'preTagCommit'              ) << { preTagCommit() }
        project.task( 'updateVersion'             ) << { updateVersion() }
        project.task( 'commitNewVersion'          ) << { commitNewVersion() }
    }


    void checkSnapshotDependencies() {

        def snapshotDependencies = project.configurations.getByName( 'runtime' ).allDependencies.
                                   findAll { Dependency d -> d.version?.contains( 'SNAPSHOT' )}.
                                   collect { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

        if ( snapshotDependencies ) {
            def message = "Snapshot dependencies detected: $snapshotDependencies"
            if ( releaseConvention().failOnSnapshotDependencies ) {
                throw new GradleException( message )
            }
            else {
                project.logger.warn( "WARNING: $message" )
            }
        }
    }


    void unSnapshotVersion() {
        def version = project.version.toString()

        if ( version.contains( '-SNAPSHOT' )) {
            project.setProperty( 'usesSnapshot', true )
            version -= '-SNAPSHOT'
            updateVersionProperty( version )
        }
        else {
            project.setProperty( 'usesSnapshot', false )
        }
    }


    void preTagCommit() {
        if ( project.properties[ 'usesSnapshot' ] ) {
            // should only be committed if the project was using a snapshot version.
            commit( releaseConvention().preTagCommitMessage + " \"${ project.version }\"." )
        }
    }


    void updateVersion() {
        def version = project.version.toString()
        Map<String, Closure> patterns = releaseConvention().versionPatterns

        for ( Map.Entry<String, Closure> entry in patterns ) {

            String pattern  = entry.key
            //noinspection GroovyUnusedAssignment
            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if ( matcher.find()) {
                String nextVersion = handler( matcher )
                if ( project.properties[ 'usesSnapshot' ] ) {
                    nextVersion += '-SNAPSHOT'
                }
                updateVersionProperty( readLine( "Enter the next version (current one released as [$version]):", nextVersion ))
                return
            }
        }

        throw new GradleException( "Failed to increase version [$version] - unknown pattern" )
    }


    def commitNewVersion() {
        commit( releaseConvention().newVersionCommitMessage + " \"${ project.version }\"." )
    }


    def checkPropertiesFile() {

        File       propertiesFile   = project.file( 'gradle.properties' )
        assert propertiesFile.file, "[$propertiesFile.canonicalPath] not found, create it and specify version = ..."

        Properties properties = new Properties()
        propertiesFile.withReader { properties.load( it ) }

        assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
        assert releaseConvention().versionPatterns.keySet().any { ( properties.version =~ it ).find() }, \
               "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
               releaseConvention().versionPatterns.keySet()
    }


    @Requires({ scmPluginClass && message })
    def commit ( String message ) {
        (( BaseScmPlugin ) project.plugins.findPlugin( scmPluginClass )).commit( message )
    }


    /**
     * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
     * @param project
     */
    private Class<? extends BaseScmPlugin> applyScmPlugin() {

        Class c = ( Class ) project.rootProject.projectDir.list().with {
            delegate.grep( '.svn' ) ? SvnReleasePlugin :
            delegate.grep( '.bzr' ) ? BzrReleasePlugin :
            delegate.grep( '.git' ) ? GitReleasePlugin :
            delegate.grep( '.hg'  ) ? HgReleasePlugin  :
                                      null
        }

        if ( ! c ) {
            throw new GradleException(
                'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
                "[${ project.rootProject.projectDir.canonicalPath }]" )
        }

        assert BaseScmPlugin.isAssignableFrom( c )
        project.apply plugin: c
        c
    }
}