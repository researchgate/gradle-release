buildscript {
	repositories {
		maven { url "http://tellurianring.com/projects/gradle-plugins/repo"}
		mavenCentral()
	}
	dependencies {
		classpath 'gradle-release:gradle-release:1.2-SNAPSHOT',
				'org.ajoberstar:gradle-git:0.4.0'
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}