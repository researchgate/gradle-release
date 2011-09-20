package release

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project

/**
 * Helper object extended by plugins.
 * @author evgenyg
 */
class PluginHelper {

    private static final String LINE_SEP = System.getProperty( 'line.separator' )
    private static final String PROMPT   = "${LINE_SEP}>"

    protected Project project

   /**
    * Retrieves plugin convention of the type specified.
    *
    * @param project        current Gradle project
    * @param pluginName     plugin name
    * @param conventionType convention type
    * @return               plugin convention of the type specified
    */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    @Requires({ project && pluginName && conventionType })
    @Ensures({ conventionType.isInstance( result ) })
    public <T> T convention( String pluginName, Class<T> conventionType ) {

        Object convention = project.convention.plugins.get( pluginName )

        assert convention, \
               "Current project contains no \"$pluginName\" plugin convention"

        assert conventionType.isInstance( convention ), \
               "Current project contains \"$pluginName\" plugin convention, " +
               "but it's of type [${ convention.class.name }] rather than expected [${ conventionType.name }]"

        ( T ) convention
    }


    /**
     * Gets current {@link ReleasePluginConvention}.
     *
     * @param project current Gradle project
     * @return        current {@link ReleasePluginConvention}.
     */
    @Ensures({ result })
    ReleasePluginConvention releaseConvention() {
        convention( 'release', ReleasePluginConvention )
    }


    /**
     * Executes command specified and retrieves its "stdout" output.
     *
     * @param failOnStderr whether execution should fail if there's any "stderr" output produced, "true" by default.
     * @param commands     commands to execute
     * @return command "stdout" output
     */
    @Requires({ commands })
    @Ensures({ result != null })
    String exec ( boolean failOnStderr = true, String ... commands ) {
        def out     = new StringBuffer()
        def err     = new StringBuffer()
        def process = ( commands as List ).execute()

        project.logger.info( " >>> Running $commands" )

        process.waitForProcessOutput( out, err )

        project.logger.info( " >>> Running $commands: [$out][$err]" )

        if ( failOnStderr ) {
            assert err.length() < 1, "Running $commands produced an stderr output: [$err]"
        }

        out.toString()
    }


    /**
     * Executes command specified and verifies neither "stdout" or "stderr" contain an error pattern specified.
     *
     * @param commands     commands to execute
     * @param errorMessage error message to throw
     * @param errorPattern error pattern to look for
     */
    @Requires({ commands && errorMessage && errorPattern })
    void exec( List<String> commands, String errorMessage, String ... errorPattern ) {
        def out     = new StringBuffer()
        def err     = new StringBuffer()
        def process = commands.execute()

        project.logger.info( " >>> Running $commands" )

        process.waitForProcessOutput( out, err )

        project.logger.info( " >>> Running $commands: [$out][$err]" )

        assert ! [ out, err ]*.toString().any{ String s -> errorPattern.any { s.contains( it ) }}, \
               "$errorMessage - [$out][$err]"
    }


    /**
     * Capitalizes first letter of the String specified.
     *
     * @param s String to capitalize
     * @return String specified with first letter capitalized
     */
    @Requires({ s })
    @Ensures({ Character.isUpperCase( result[ 0 ] as char ) })
    String capitalize( String s ) {
        s[ 0 ].toUpperCase() + ( s.size() > 1 ? s[ 1 .. -1 ] : '' )
    }


    /**
     * Reads user input from the console.
     *
     * @param message      Message to display
     * @param defaultValue (optional) default value to display
     * @return             User input entered or default value if user enters no data
     */
    @Requires({ message })
    String readLine ( String message, String defaultValue = null ) {
        System.console().readLine( "$PROMPT $message " + ( defaultValue ? "[$defaultValue] " : '' )) ?:
        defaultValue
    }


    /**
     * Updates 'gradle.properties' file with new version specified.
     *
     * @param project    current project
     * @param newVersion new version to store in the file
     */
    @Requires({ newVersion })
    void updateVersionProperty( String newVersion ) {

        project.version             = newVersion
        File       propertiesFile   = project.file( 'gradle.properties' )
        assert propertiesFile.file, "[$propertiesFile.canonicalPath] wasn't found, can't update it"

        Properties gradleProps = new Properties()
        propertiesFile.withReader { gradleProps.load( it ) }

        gradleProps.version = newVersion
        propertiesFile.withWriter {
            gradleProps.store( it, "Version updated to '${newVersion}', by Gradle release plugin (http://code.launchpad.net/~gradle-plugins/gradle-release/)." )
        }
    }
}
