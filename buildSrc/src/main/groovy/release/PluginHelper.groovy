package release

import org.gradle.api.Project

/**
 * Helper object extended by plugins.
 * @author evgenyg
 */
class PluginHelper
{
   /**
    * Retrieves plugin convention of the type specified.
    *
    * @param project        current Gradle project
    * @param pluginName     plugin name
    * @param conventionType convention type
    * @return               plugin convention of the type specified
    */
    public <T> T convention( Project project, String pluginName, Class<T> conventionType )
    {
        assert project && pluginName && conventionType

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
    ReleasePluginConvention releaseConvention( Project project )
    {
        convention( project, 'release', ReleasePluginConvention )
    }


    /**
     * Executes command specified and retrieves its "stdout" output.
     *
     * @param failOnStderr whether execution should fail if there's any "stderr" output produced, "true" by default.
     * @param command      command to execute
     * @return command "stdout" output
     */
    String exec ( boolean failOnStderr = true, String ... command )
    {
        assert command

        def out     = new StringBuffer()
        def err     = new StringBuffer()
        def process = ( command as List ).execute()

        process.waitForProcessOutput( out, err )

        if ( failOnStderr )
        {
            assert err.length() < 1, "Running $command produced an stderr output: [$err]"
        }

        out.toString()
    }


    /**
     * Executes command specified and verifies neither "stdout" or "stderr" contain an error pattern specified.
     *
     * @param command      command to execute
     * @param errorMessage error message to throw
     * @param errorPattern error pattern to look for
     */
    void exec( List<String> command, String errorMessage, String ... errorPattern )
    {
        assert command && errorMessage && errorPattern

        def out     = new StringBuffer()
        def err     = new StringBuffer()
        def process = command.execute()

        process.waitForProcessOutput( out, err )

        assert ! [ out, err ]*.toString().any{ String s -> errorPattern.any { s.contains( it ) }}, \
               "$errorMessage - [$out][$err]"
    }


    /**
     * Capitalizes first letter of the String specified.
     *
     * @param s String to capitalize
     * @return String specified with first letter capitalized
     */
    String capitalize( String s )
    {
        assert s
        s[ 0 ].toUpperCase() + ( s.size() > 1 ? s[ 1 .. -1 ] : '' )
    }
}
