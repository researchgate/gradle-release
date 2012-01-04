package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * User: elberry
 * User: evgenyg
 * Date: 9/21/11
 */
@Mixin( PluginHelper )
@SuppressWarnings( 'JUnitPublicNonTestMethod' )
class GitReleasePluginTests extends AbstractReleasePluginTests {

	@Override
    @SuppressWarnings( 'GetterMethodCouldBeProperty' )
	Class<? extends Plugin<Project>> getPluginClass() { BzrReleasePlugin }

	@Override
	void initProject( File root, String projectName ) {

        exec( false, [:], root, *[ 'git', 'init', projectName ] )
	}
}
