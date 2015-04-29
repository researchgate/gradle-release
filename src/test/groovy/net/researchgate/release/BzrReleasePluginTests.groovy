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

import org.gradle.api.Plugin
import org.gradle.api.Project

@Mixin( PluginHelper )
@SuppressWarnings( 'JUnitPublicNonTestMethod' )
class BzrReleasePluginTests extends AbstractReleasePluginTests {

	@Override
    @SuppressWarnings( 'GetterMethodCouldBeProperty' )
	Class<? extends Plugin<Project>> getPluginClass() { BzrReleasePlugin }

	@Override
	void initProject( File root, String projectName ) {

        exec( false, [:], root, *[ 'bzr', 'init', projectName ] )
	}
}
