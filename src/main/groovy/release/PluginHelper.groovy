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
        Object convention = project.convention.plugins.get( pluginName )

        assert convention, \
               "Current Project contains no \"$pluginName\" plugin convention"

        assert conventionType.isInstance( convention ), \
               "Current Project contains \"$pluginName\" plugin convention, " +
               "but it's of type [${ convention.class.name }] instead of expected [${ conventionType.name }]"

        ( T ) convention
    }


    /**
     * Gets current {@link ReleasePluginConvention}.
     *
     * @param project current Gradle project
     * @return        current {@link ReleasePluginConvention}.
     */
    public ReleasePluginConvention releaseConvention( Project project )
    {
        convention( project, 'release', ReleasePluginConvention )
    }
}
