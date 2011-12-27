buildscript {
	repositories {
		ivy {
			name = 'gradle_release'
			artifactPattern "${System.properties['user.dir']}/build/libs/[artifact]-[revision].jar"
		}
	}
	dependencies {
		classpath "gradle-release:gradle-release:${project.version}"
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}