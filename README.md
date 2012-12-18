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

Current Version: 1.0

Current SCM support: [Bazaar](http://bazaar.canonical.com/en/), [Git](http://git-scm.com/), [Mercurial](), and [Subversion]()

## Installation & Usage

The gradle-release plugin will work with Gradle 1.0M3 and beyond
To use the plugin simply add an `apply from` script to your project's `build.gradle` file
It's recommended that you use the `latest` script reference instead of a specific version so that you can automatically get plugin updates:

```groovy
apply from: "https://launchpad.net/gradle-release/trunk/latest/+download/apply.groovy"
```
If you do want to use a specific version, just change the `latest` reference to the specific version:
apply from: "https://launchpad.net/gradle-release/trunk/1.0/+download/apply.groovy"

After you have your 'build.gradle' file configured, simply run: 'gradle release' and follow the on-screen instructions.

### Configuration

As described above, the plugin will check for un-committed files and SNAPSHOT dependencies. By default the plugin will fail when any un-committed, or SNAPSHOT dependencies are found.

Below are some properties of the Release Plugin Convention that can be used to make your release process more lenient.  Name 	 Default 	 Description
failOnCommitNeeded 	 true 	 Fail the release process when there un-committed changes.
failOnPublishNeeded 	 true 	 Fail when there are local commits that haven't been published upstream (DVCS support)
failOnSnapshotDependencies 	 true 	 Fail when the project has dependencies on SNAPSHOT versions
failOnUnversionedFiles 	 true 	 Fail when files are found that are not under version control
failOnUpdateNeeded 	 true 	 Fail when the source needs to be updated, or there are changes available upstream that haven't been pulled.
revertOnFail	 true 	 When a failure occurs should the plugin revert it's changes to gradle.properties?


 Below are some properties of the Release Plugin Convention that can be used to customize the build.  Name 	 Default 	 Description
preCommitText 	 ”” 	 This will be prepended to all commits done by the plugin. A good place for code review, or ticket numbers
preTagCommitMessage 	 ”[Gradle Release Plugin] - pre tag commit: ” 	 The commit message used to commit the non-SNAPSHOT version if SNAPSHOT was used.
tagCommitMessage 	 ”[Gradle Release Plugin] - creating tag: ” 	 The commit message used when creating the tag. Not used with BZR projects.
newVersionCommitMessage 	 ”[Gradle Release Plugin] - new version commit: ” 	 The commit message used when committing the next version.


 To set any of these properties to false, add a 'release' configuration to your project's 'build.gradle' file. Eg. To ignore un-versioned files, you would add the following to your 'build.gradle' file:
release {
   failOnUnversionedFiles = false
}

 Eg. To ignore upstream changes, change 'failOnUpdateNeeded' to false:
release {
   failOnUpdateNeeded = false
}
Custom release steps

 To add a step to the release process is very easy. Gradle provides a very nice mechanism for manipulating existing tasks.

 For example, if we wanted to make sure 'uploadArchives' is called and succeeds before the tag has been created, we would just add the 'uploadArchives' task as a dependency of the 'createReleaseTag' task:
createReleaseTag.dependsOn uploadArchives
Multi-Project Builds

 Support for multi-project builds isn't complete, but will work given some assumptions. The gradle-release plugin assumes and expects the following:

 Only the root|parent project is applying the plugin.

 Only one version is used for root and sub projects.

 Only one version control system is used by both root and sub projects.

 This means the gradle-release plugin does not support sub projects that have different versions from their parent|root project, and it does not support sub projects that have different version control systems from the parent project.
Working in Continuous Integration

 In a continuous integration environment like Jenkins or Hudson, you don't want to have an interactive release process. To avoid having to enter any information manually during the process, you can tell the plugin to automatically set and update the version number.

 You can do this by setting the 'gradle.release.useAutomaticVersion' property on the command line, or in Jenkins when you execute gradle.
-Pgradle.release.useAutomaticVersion=true
## Getting Help

To ask questions or report bugs, please use the GitHub project.

 Project Page: https://launchpad.net/gradle-release
    Asking Questions: https://answers.launchpad.net/gradle-release/+addquestion
    Reporting Bugs: https://bugs.launchpad.net/gradle-release/+filebug
