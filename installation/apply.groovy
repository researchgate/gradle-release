buildscript {
	repositories {
		ivy {
			name = 'gradle_release'
			artifactPattern 'http://tellurianring.com/projects/gradle-plugins/[module]/[revision]/[artifact]-[revision].[ext]'
			ivyPattern 'http://tellurianring.com/projects/gradle-plugins/[module]/[revision]/[artifact]-[revision].[ext]'
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