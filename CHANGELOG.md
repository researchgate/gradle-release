# Changelog

## 2.2.2
##### Released:  not yet

* GIT: Catch errors correctly when doing commit (#128 thanks grigorigoldman)

## 2.2.1
##### Released:  18. August 2015

* COMMON: Fix incompatibility with maven-publish plugin (#125 thanks raphsoft and alibkord)

## 2.2.0
##### Released:  9. August 2015

### Bugfixes

* COMMON: Refactored writing of version property to property-file. (#120, thanks simonsilvalauinger)
    * Spaces are now preserved in properties file
    * No other properties will be touched
    * DateTime will not be written anymore

### New Features

* COMMON: Possibility to use a regex for the option requireBranch (#118,#119, thanks grigorigoldman)
* COMMON: Possibility to use custom build tasks by setting the option ```buildTasks``` (#117, thanks ntarocco)

## 2.1.2
##### Released: 12. June 2015

### Bugfixes

* SVN: Fix checkUpdateNeeded when externals were used (#106, thanks pstanoev)
* SVN: Fix creating tags when subdirectories are used in tagname (#110, thanks kmoens)

### Changes

* COMMON: Added cli option ```release.useAutomaticVersion``` to normalize with the other options. ```gradle.release.useAutomaticVersion``` will be deprecated in the future.

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
