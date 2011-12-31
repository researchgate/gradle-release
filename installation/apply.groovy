buildscript {
	repositories {
		ivy {
			name = 'gradle_release'
			artifactPattern 'http://launchpad.net/[organization]/trunk/[revision]/+download/[artifact]-[revision].jar'
		}
	}
	dependencies {
		classpath 'gradle-release:gradle-release:0.8a'
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}