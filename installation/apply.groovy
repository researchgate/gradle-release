buildscript {
	repositories {
		ivy {
			name = 'gradle_release'
			artifactPattern 'https://launchpad.net/[organization]/trunk/[revision]/+download/[artifact]-[revision].jar'
			//artifactPattern 'http://www.tellurianring.com/projects/gradle-plugins/release/[artifact]-[revision].jar'
		}
	}
	dependencies {
		classpath 'gradle-release:gradle-release:1.0'
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}