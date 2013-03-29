buildscript {
	repositories {
		mavenCentral()
		maven { url "https://oss.sonatype.org/content/groups/public"}
	}
	dependencies {
		classpath 'com.github.townsfolk:gradle-release:1.2-SNAPSHOT',
				'org.ajoberstar:gradle-git:0.4.0'
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}