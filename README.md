# gradle-release plugin

[![Build Status](https://travis-ci.org/researchgate/gradle-release.svg?branch=master)](https://travis-ci.org/researchgate/gradle-release)
[![Download](https://api.bintray.com/packages/researchgate/gradle-plugins/gradle-release/images/download.svg)](https://bintray.com/researchgate/gradle-plugins/gradle-release/_latestVersion)
[![Gitter](https://img.shields.io/badge/chat-online-brightgreen.svg?style=flat)](https://gitter.im/researchgate/gradle-release)



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

Current SCM support: [Bazaar](http://bazaar.canonical.com/en/), [Git](http://git-scm.com/) (1.7.2 or newer), [Mercurial](http://mercurial.selenic.com/), and [Subversion](http://subversion.apache.org/)

## Installation

The gradle-release plugin will work with Gradle 1.0M3 and beyond

### Gradle 1.x and 2.0

```groovy
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'net.researchgate:gradle-release:2.4.0'
  }
}

apply plugin: 'net.researchgate.release'
```

### Gradle 2.1 and higher

```groovy
plugins {
  id 'net.researchgate.release' version '2.4.0'
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
		<td>The string template which is used to generate the tag name. Possible variables are $version and $name. Example: '$name-$version' will result in "myproject-1.1.0". (Always ensure to use single-quotes, otherwise `$` is interpreted already in your build script)</td>
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
		<td>Defines the branch which releases must be done off of. Eg. set to `release` to require releases are done on the `release` branch (or use a regular expression to allow releases from multiple branches, e.g. `/release|master/`). Set to '' to ignore.</td>
	</tr>
	<tr>
		<td>Git</td>
		<td>pushOptions</td>
		<td>{empty}</td>
		<td>Defines an array of options to add to the git adapter during a push.  This could be useful to have the vc hooks skipped during a release. Example `pushOptions = ["--no-verify"]`</td>
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

This are all possible configuration options and its default values:

```
release {
    failOnCommitNeeded = true
    failOnPublishNeeded = true
    failOnSnapshotDependencies = true
    failOnUnversionedFiles = true
    failOnUpdateNeeded = true
    revertOnFail = true
    preCommitText = ''
    preTagCommitMessage = '[Gradle Release Plugin] - pre tag commit: '
    tagCommitMessage = '[Gradle Release Plugin] - creating tag: '
    newVersionCommitMessage = '[Gradle Release Plugin] - new version commit: '
    tagTemplate = '${version}'
    versionPropertyFile = 'gradle.properties'
    versionProperties = []
    buildTasks = ['build']
    versionPatterns = [
        /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") }
    ]
    scmAdapters = [
        net.researchgate.release.GitAdapter,
        net.researchgate.release.SvnAdapter,
        net.researchgate.release.HgAdapter,
        net.researchgate.release.BzrAdapter
    ]

    git {
        requireBranch = 'master'
        pushToRemote = 'origin'
        pushToBranchPrefix = ''
        commitVersionFileOnly = false
    }

    svn {
        username = null
        password = null
        pinExternals = false   // allows to pin the externals when tagging, requires subversion client >= 1.9.0
    }
}
```

### Custom release steps

To add a step to the release process is very easy. Gradle provides a very nice mechanism for [manipulating existing tasks](http://gradle.org/docs/current/userguide/tutorial_using_tasks.html#N102B2)
For example, if we wanted to make sure `uploadArchives` is called and succeeds after the build with the release version has finished, we would just add the `uploadArchives` task as a dependency of the `afterReleaseBuild` task:

    afterReleaseBuild.dependsOn uploadArchives

### Multi-Project Builds

Support for [multi-project builds](http://gradle.org/docs/current/userguide/multi_project_builds.html) isn't complete, but will work given some assumptions. The gradle-release plugin assumes and expects that only one version control system is used by both root and sub projects.

Apply the plugin separately to each subproject that you wish to release. Release using a qualified task name, e.g.:

    ./gradlew :sub:release # release a subproject named "sub"
    ./gradlew :release # release the root project


### Working in Continuous Integration

In a continuous integration environment like Jenkins or Hudson, you don't want to have an interactive release process. To avoid having to enter any information manually during the process, you can tell the plugin to automatically set and update the version number.

You can do this by setting the `release.useAutomaticVersion` property on the command line, or in Jenkins when you execute gradle. The version to release and the next version can be optionally defined using the properties `release.releaseVersion` and `release.newVersion`.

```bash
$ gradle release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=1.0.0 -Prelease.newVersion=1.1.0-SNAPSHOT
```


## Getting Help

To ask questions please use stackoverflow or gitter.

* Chat/Gitter: [https://gitter.im/researchgate/gradle-release](https://gitter.im/researchgate/gradle-release)
* Stack Overflow: [http://stackoverflow.com/questions/ask?tags=gradle-release-plugin](http://stackoverflow.com/questions/ask?tags=gradle-release-plugin)

To report bugs, please use the GitHub project.

* Project Page: [https://github.com/researchgate/gradle-release](https://github.com/researchgate/gradle-release)
* Reporting Bugs: [https://github.com/researchgate/gradle-release/issues](https://github.com/researchgate/gradle-release/issues)
