# gradle-release plugin

[![Build Status](https://travis-ci.org/researchgate/gradle-release.svg?branch=master)](https://travis-ci.org/researchgate/gradle-release)
[![Download](https://api.bintray.com/packages/researchgate/gradle-plugins/gradle-release/images/download.svg)](https://bintray.com/researchgate/gradle-plugins/gradle-release/_latestVersion)

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

Current SCM support: [Bazaar](http://bazaar.canonical.com/en/), [Git](http://git-scm.com/), [Mercurial](http://mercurial.selenic.com/), and [Subversion](http://subversion.apache.org/)

## Installation

The gradle-release plugin will work with Gradle 1.0M3 and beyond

### Gradle 1.x and 2.0

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'net.researchgate:gradle-release:2.1.0'
    }
}

apply plugin: 'net.researchgate.release'
```

### Gradle 2.1 and higher

```groovy
plugins {
  id 'net.researchgate.release' version '2.1.0'
}
```

Please refer to the [Gradle DSL PluginDependenciesSpec](http://www.gradle.org/docs/current/dsl/org.gradle.plugin.use.PluginDependenciesSpec.html) to
understand the behavior and limitations when using the new syntax to declare plugin dependencies.

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
		<td>tagTemplate</td>
		<td>$version</td>
		<td>The string template which is used to generate the tag name. Possible variables are $version and $name. Example: "$name-$version" will result in "myproject-1.1.0"</td>
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
For example, if we wanted to make sure `uploadArchives` is called and succeeds after the build with the release version has finished, we would just add the `uploadArchives` task as a dependency of the `afterReleaseBuild` task:

    afterReleaseBuild.dependsOn uploadArchives

### Multi-Project Builds

Support for [multi-project builds](http://gradle.org/docs/current/userguide/multi_project_builds.html) isn't complete, but will work given some assumptions. The gradle-release plugin assumes and expects the following:

1. Only the root|parent project is applying the plugin
2. Only one version is used for root and sub projects
3. Only one version control system is used by both root and sub projects

This means the gradle-release plugin does not support sub projects that have different versions from their parent|root project, and it does not support sub projects that have different version control systems from the parent project.

### Working in Continuous Integration

In a continuous integration environment like Jenkins or Hudson, you don't want to have an interactive release process. To avoid having to enter any information manually during the process, you can tell the plugin to automatically set and update the version number.

You can do this by setting the `gradle.release.useAutomaticVersion` property on the command line, or in Jenkins when you execute gradle. The version to release and the next version can be optionally defined using the properties `releaseVersion` and `nextVersion`.

```bash
$ gradle release -Pgradle.release.useAutomaticVersion=true -PreleaseVersion=1.0.0 -PnewVersion=1.1.0-SNAPSHOT
```


## Getting Help

To ask questions or report bugs, please use the GitHub project.

* Project Page: [https://github.com/researchgate/gradle-release](https://github.com/researchgate/gradle-release)
* Asking Questions: [https://github.com/researchgate/gradle-release/issues](https://github.com/researchgate/gradle-release/issues)
* Reporting Bugs: [https://github.com/researchgate/gradle-release/issues](https://github.com/researchgate/gradle-release/issues)
