package net.researchgate.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.Specification

/**
 * User: elberry
 * User: evgenyg
 * Date: 9/21/11
 */
@SuppressWarnings( 'JUnitPublicNonTestMethod' )
abstract class AbstractReleasePluginTests extends Specification {

	abstract protected Class<? extends Plugin<Project>> getPluginClass()
	abstract protected void initProject( File root, String projectName )

	def setup() {
		File    workingDir  = new File( System.getProperty( 'user.dir' ))
		Project thisProject = ProjectBuilder.builder().withProjectDir( workingDir ).build()
        File    tempDir     = new File( thisProject.buildDir, 'tests-temp' )
		String  projectName = "${ pluginClass.simpleName }_TestProject_${ new Date().format( 'yyMMdd_HH-mm-ss' )}"
		File    projectDir  = new File( tempDir, projectName )

        assert projectDir.with { directory || mkdirs() }
		initProject( tempDir, projectName )

        new File( tempDir, "$projectName/build.gradle" ).write( '''
      		apply plugin: 'groovy'
      		apply plugin: 'release'
      		'''.stripIndent())

		project = ProjectBuilder.builder().withProjectDir( projectDir ).build()
	}


	@Ignore
	def 'initScmPlugin_CheckPluginInstalled'() {
        given:
        project.plugins.each { plugin -> log.info( plugin.toString())}

        expect:
		project.tasks && project.plugins
		// doesn't currently work. doesn't look like any plugins are installed at all. Might need to use some task
		// verification. Eg. just perform the release, then use SCM plugin to verify it happened.
		//assert project.plugins.findPlugin(pluginClass), "${pluginClass} Doesn't seem to be installed for project: ${project.rootDir}"
	}
}