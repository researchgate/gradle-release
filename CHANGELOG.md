# Changelog

## 2.5.0
##### Released: xx. July 2016

### New Features

* GIT: Option `git.pushOptions` can now be set to add additional git options to the git push.

### Bugfixes

* COMMON: Detect errors of executed commands with their exit value

## 2.4.0
##### Released: 25. May 2016

### New Features

* GIT: Option `git.commitVersionFileOnly` can now be set to make sure we only commit the versionFile instead of all modified file.
    * This will be useful in some case when the repository is modified by the build job on some files to work around build server limitation.
* SVN: Option `svn.pinExternals` can now be set to pin the version of externals

### Bugfixes

* COMMON: Fix support for properties in gradle.properties separated by space or colon.

## 2.3.5
##### Released: 16. January 2016

### Bugfixes

* COMMON: Avoid exception when buildtasks is an empy list (#156 thanks pepijnve)
* GIT: Disable coloring for git-branch call (#157 thanks diegotori)

## 2.3.4
##### Released: 5. October 2015

### Bugfixes

* COMMON: Writing version property could override other properties (#149 thanks markalanturner)

## 2.3.3
##### Released: 5. October 2015

### Bugfixes

* COMMON: Calling custom buildTasks was broken (#144, thanks madhead)
* COMMON: Order of tasks was not completely correct for --parallel runs

## 2.3.2
##### Released: 4. October 2015

### Bugfixes

* SVN: Fix logic in checkUpdateNeeded (#146, thanks Myllyenko)

## 2.3.1
##### Released: 30. November 2015

### Bugfixes

* SVN: Fix NPE happening in checkUpdateNeeded (#145, thanks IVU-chl)

## 2.3.0
##### Released: 13. October 2015

### New Features

* COMMON: Possibility to use the release plugin in multiprojects where each project has its own version (#116, thanks christierney)
    * see [the example](https://github.com/researchgate/gradle-release-examples/tree/master/multi-project-multiple-versions)
* GIT: Option ```pushToBranchPrefix``` can now be set to specify a remote branch prefix when committing next version (#140, #113, thanks muryoh)

### Changes

* COMMON: The plugin now emits warnings when setting deprecated configuration or cli options

### Bugfixes

* COMMON: Fixed internal bug in Executor not overwriting environment variables as expected (#135, thanks ddimtirov)
* COMMON: Fix bug with projects that do not yet have a property file created (#123, thanks dodgex)
* COMMON: Fix bug with release failing when using --parallel option for gradle (#60, thanks tschulte)
* GIT: The option ```pushToCurrentBranch``` is deprecated, as it was simply unnecessary and can be safely removed

## 2.2.3
##### Released: 6. November 2015

### Bugfixes

* COMMON: Writing version property could override other properties (#149 thanks markalanturner)

## 2.2.2
##### Released: 8. September 2015

### Bugfixes

* GIT: Respect option ```pushToRemote``` when pushing tag to remote
* GIT: Catch errors correctly when doing commit (#128, thanks grigorigoldman)
* SVN: Detect trunk/tag/branch directories case-insensitive (#130, thanks naugler)

## 2.2.1
##### Released: 18. August 2015

### Bugfixes

* COMMON: Fix incompatibility with maven-publish plugin (#125, thanks raphsoft and alibkord)

## 2.2.0
##### Released: 9. August 2015

### New Features

* COMMON: Possibility to use a regex for the option requireBranch (#118,#119, thanks grigorigoldman)
* COMMON: Possibility to use custom build tasks by setting the option ```buildTasks``` (#117, thanks ntarocco)

### Bugfixes

* COMMON: Refactored writing of version property to property-file. (#120, thanks simonsilvalauinger)
    * Spaces are now preserved in properties file
    * No other properties will be touched
    * DateTime will not be written anymore

## 2.1.2
##### Released: 12. June 2015

### Changes

* COMMON: Added cli option ```release.useAutomaticVersion``` to normalize with the other options. ```gradle.release.useAutomaticVersion``` will be deprecated in the future.

### Bugfixes

* SVN: Fix checkUpdateNeeded when externals were used (#106, thanks pstanoev)
* SVN: Fix creating tags when subdirectories are used in tagname (#110, thanks kmoens)

## 2.1.1
##### Released: 2. June 2015

### Bugfixes

* SVN: Fix exception when externals were used (#106, thanks pstanoev)
* SVN: Fix externals not to be detected as uncommited changes

## 2.1.0
##### Released: 29. May 2015

This release contains new features but was also the target of some internal refactorings.
Due to the refactoring one minor breaking change had to be done. See the list below for details and the [upgrade instructions](UPGRADE.md#20-to-21).

### **Breaking Changes**

* GIT: The configuration options are now all contained in the release closure. If you were using one of the options ```requireBranch```, ```pushToRemote``` or ```pushToCurrentBranch``` you need to adjust your config. See [upgrade instructions](UPGRADE.md#20-to-21).

### New Features

* COMMON: New flexible configuration ```tagTemplate``` to specify the name of the tag. (#96)
* COMMON: Refactored the build process during release to run in separate process. This will hopefully resolve all the issues with certain other plugins like the new maven-publish or the bintray plugin.
* COMMON: Added **beforeReleaseBuild** and **afterReleaseBuild** hook, which both run in the same process as the build itself.
* COMMON: Added a convenient way for external adapters to be used instead of the supplied ones. Documentation and example to follow.
* SVN: Allow credentials to be specified
    * Either with commandline parameters ```gradle release -Prelease.svn.username=eric -Prelease.svn.password=secret```
    * Or directly inside your build.gradle: (This is a silly example, don't put your credentials under version control! For security reasons you might want to put variables inside your [users properties](https://gradle.org/docs/current/userguide/build_environment.html) file and reference them in the projects gradle script)

```
release {
    svn {
        username = eric
        password = secret
    }
}
```

### Changes

* COMMON: The SCM adapter is now lazy loaded and does not require an scm to be present during plugin initialization. New task `createScmAdapter` added at the beginning of release, which handles the creation of the correct scm adapter.

### Deprecated Features

* COMMON: The configuration options ```includeProjectNameInTag``` and ```tagPrefix``` are deprecated and will be remove with 3.0. Please migrate to ```tagTemplate```.  See [upgrade instructions](UPGRADE.md#20-to-21). (#96)
* COMMON: Depending on any internal release task like **createReleaseTag** is highly discouraged. Use the **beforeReleaseBuild** or **afterReleaseBuild** tasks. See [upgrade instructions](UPGRADE.md#20-to-21).

## 2.0.2
##### Released: 17. Feb 2015

### Bugfixes

* SVN: Fixed running release plugin without using snapshot versions (#91 #92, thanks mwhipple)


## 2.0.1
##### Released: 11. Feb 2015

### Bugfixes

* GIT: Fixed running release plugin on windows (#90, thanks SanderDN)


## 2.0.0
##### Released: 4. Feb 2015

### New Features

* COMMON: The release version and new version can be supplied as cli parameters (#44, #67, thanks thokuest)
* GIT: Option to disable pushing to remote (#41, #33, thanks szpak)
* GIT: Option to change name of remote to push to

### Changes

* Package of classes had to be changed from ```release```to ```net.researchgate.release```. (This is only relevant if you imported some of the classes directly in your code.)
* COMMON: Better error message if updating of version fails

### Bugfixes

* COMMON: Fixed problems with setting custom versionPropertyFile (#56, #57, #58, thanks jeffastorey)
* GIT: Fixed calling release plugin in sub-project with git repository in parent (#35, thanks szpak)
* GIT: Fixed setting requireBranch to empty string (#40, thanks szpak)
* HG: Only check for incoming and outgoing changes on current branch for Mercurial (#76, thanks litpho)
* SVN: Fixed SVN release on windows (#39, #68, thanks thokuest)
* SVN: Fix creating branch from correct revision (#48)
* SVN: Set proper environment for svn client (#52, #86)
