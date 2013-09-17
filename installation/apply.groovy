buildscript {
	repositories {
		mavenCentral()
		maven { url "https://oss.sonatype.org/content/groups/public"}
	}
	dependencies {
		//classpath 'com.github.townsfolk:gradle-release:1.2-SNAPSHOT'
        classpath files('/home/HQ/jstorey/workspace/gradle-release/build/classes/main')
        classpath files('/home/HQ/jstorey/workspace/gradle-release/build/resources/main')
	}
}
// Check to make sure release.ReleasePlugin isn't already added.
if (!project.plugins.findPlugin(release.ReleasePlugin)) {
	project.apply(plugin: release.ReleasePlugin)
}