buildscript {
	repositories {
		ivy {
			def releaseDir = project.hasProperty('gradle.release.dir') ? project.getProperty('gradle.release.dir') :
				                                                         System.properties['user.dir']
			name = 'gradle_release'
			artifactPattern "${releaseDir}/build/libs/[artifact]-[revision].jar"
		}
	}
	dependencies {
		def releaseVersion = project.hasProperty('gradle.release.ver') ? project.getProperty('gradle.release.ver') :
                                                                         project.version
		classpath "gradle-release:gradle-release:${releaseVersion}"
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}