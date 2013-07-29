## Need help, or someone to take over

I Haven't had any time lately to work on this project. I'm very thankful to those in the Gradle community who've submitted patches.
Unfortunately, those patches really have comprised practically all work that's been done on this project over the last few months.

## Introduction

The gradle-release plugin is designed to work similar to the Maven release plugin.
The `gradle release` task defines the following as the default release process:

* The plugin checks for any un-committed files (Added, modified, removed, or un-versioned).
* Checks for any incoming or outgoing changes.
* Removes the SNAPSHOT flag on your projects version (If used)
* Prompts you for the release version.
* Checks if your project is using any SNAPSHOT dependencies
* Will `build` your project.
* Commits the project if SNAPSHOT was being used.
* Creates a release tag with the current version.
* Prompts you for the next version.
* Commits the project with the new version.

Current Version: 1.2

Current SCM support: [Bazaar](http://bazaar.canonical.com/en/), [Git](http://git-scm.com/), [Mercurial](http://mercurial.selenic.com/), and [Subversion](http://subversion.apache.org/)

## Installation

The gradle-release plugin will work with Gradle 1.0M3 and beyond

### Using the "apply from" script

To use the plugin simply add an `apply from` script to your project's `build.gradle` file
It's recommended that you use the `latest` script reference instead of a specific version so that you can automatically get plugin updates:

    apply from: 'http://tellurianring.com/projects/gradle-plugins/gradle-release/apply.groovy'
If you do want to use a specific version, just change the `version` reference to the specific version:

    apply from: 'http://tellurianring.com/projects/gradle-plugins/gradle-release/[version]/apply.groovy'

Eg.

    apply from: 'http://tellurianring.com/projects/gradle-plugins/gradle-release/1.2/apply.groovy'

### Applying directly from the maven repo

The binary files are hosted in Sonatype's Nexus repository.

    https://oss.sonatype.org/content/groups/public

To use it directly or through your own Maven Repository proxy define a `buildscript` closure in your `build.gradle` file.

    buildscript {
       repositories {
          mavenCentral()
          maven { url "https://oss.sonatype.org/content/groups/public"}
       }
       dependencies {
          classpath 'com.github.townsfolk:gradle-release:1.2'
       }
    }
    apply plugin: 'release'

## Usage

After you have your `build.gradle` file configured, simply run: `gradle release` and follow the on-screen instructions.

### Configuration

As described above, the plugin will check for un-committed files and SNAPSHOT dependencies.
By default the plugin will fail when any un-committed, or SNAPSHOT dependencies are found.

Below are some properties of the Release Plugin Convention that can be used to make your release process more lenient

<table border="0">
	<tr>
		<th>Name</th>
		<th>Default value</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>failOnCommitNeeded</td>
		<td>true</td>
		<td>Fail the release process when there un-committed changes</td>
	</tr>
	<tr>
		<td>failOnPublishNeeded</td>
		<td>true</td>
		<td>Fail when there are local commits that haven't been published upstream (DVCS support)</td>
	</tr>
	<tr>
		<td>failOnSnapshotDependencies</td>
		<td>true</td>
		<td>Fail when the project has dependencies on SNAPSHOT versions</td>
	</tr>
	<tr>
		<td>failOnUnversionedFiles</td>
		<td>true</td>
		<td>Fail when files are found that are not under version control</td>
	</tr>
	<tr>
		<td>failOnUpdateNeeded</td>
		<td>true</td>
		<td>Fail when the source needs to be updated, or there are changes available upstream that haven't been pulled</td>
	</tr>
	<tr>
		<td>revertOnFail</td>
		<td>true</td>
		<td>When a failure occurs should the plugin revert it's changes to gradle.properties?</td>
	</tr>
</table>

Below are some properties of the Release Plugin Convention that can be used to customize the build<br>
<table>
	<tr>
		<th>Name</th>
		<th>Default value</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>tagPrefix</td>
		<td></td>
		<td>Prefixes tag name when release tag is created. Useful when you want the tag to include project name or some other prefix.</td>
	</tr>
	<tr>
		<td>preCommitText</td>
		<td></td>
		<td>This will be prepended to all commits done by the plugin. A good place for code review, or ticket numbers</td>
	</tr>
	<tr>
		<td>preTagCommitMessage</td>
		<td>[Gradle Release Plugin] - pre tag commit: </td>
		<td>The commit message used to commit the non-SNAPSHOT version if SNAPSHOT was used</td>
	</tr>
	<tr>
		<td>tagCommitMessage</td>
		<td>[Gradle Release Plugin] - creating tag: </td>
		<td>The commit message used when creating the tag. Not used with BZR projects</td>
	</tr>
	<tr>
		<td>newVersionCommitMessage</td>
		<td>[Gradle Release Plugin] - new version commit:</td>
		<td>The commit message used when committing the next version</td>
	</tr>
</table>

Below are some properties of the Release Plugin Convention that are specific to version control.<br>
<table>
	<tr>
		<th>VCS</th>
		<th>Name</th>
		<th>Default value</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>Git</td>
		<td>requireBranch</td>
		<td>master</td>
		<td>Defines the branch which releases must be done off of. Eg. set to `release` to require releases are done on the `release` branch. Set to '' to ignore.</td>
	</tr>
</table>

To set any of these properties to false, add a "release" configuration to your project's ```build.gradle``` file. Eg. To ignore un-versioned files, you would add the following to your ```build.gradle``` file:

    release {
      failOnUnversionedFiles = false
    }

Eg. To ignore upstream changes, change 'failOnUpdateNeeded' to false:

    release {
      failOnUpdateNeeded = false
    }

### Custom release steps

To add a step to the release process is very easy. Gradle provides a very nice mechanism for [manipulating existing tasks](http://gradle.org/docs/current/userguide/tutorial_using_tasks.html#N102B2)
For example, if we wanted to make sure `uploadArchives` is called and succeeds before the tag has been created, we would just add the `uploadArchives` task as a dependency of the `createReleaseTag` task:

    createReleaseTag.dependsOn uploadArchives

### Multi-Project Builds

Support for [multi-project builds](http://gradle.org/docs/current/userguide/multi_project_builds.html) isn't complete, but will work given some assumptions. The gradle-release plugin assumes and expects the following:

1. Only the root|parent project is applying the plugin
2. Only one version is used for root and sub projects
3. Only one version control system is used by both root and sub projects

This means the gradle-release plugin does not support sub projects that have different versions from their parent|root project, and it does not support sub projects that have different version control systems from the parent project.

### Working in Continuous Integration

In a continuous integration environment like Jenkins or Hudson, you don't want to have an interactive release process. To avoid having to enter any information manually during the process, you can tell the plugin to automatically set and update the version number.

You can do this by setting the 'gradle.release.useAutomaticVersion' property on the command line, or in Jenkins when you execute gradle.

    -Pgradle.release.useAutomaticVersion=true

## Getting Help

To ask questions or report bugs, please use the GitHub project.

* Project Page: [https://github.com/townsfolk/gradle-release](https://github.com/townsfolk/gradle-release)
* Asking Questions: [https://github.com/townsfolk/gradle-release/issues](https://github.com/townsfolk/gradle-release/issues)
* Reporting Bugs: [https://github.com/townsfolk/gradle-release/issues](https://github.com/townsfolk/gradle-release/issues)
