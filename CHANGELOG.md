# Changelog

## 2.1.0
##### Not yet released

### New Features

* COMMON: New flexible configuration ```tagTemplate``` to specify the name of the tag. (#96)
* COMMON: Refactored the build process during release to run in separate process.
* COMMON: Added **beforeReleaseBuild** and **afterReleaseBuild** hook, which both run in the same process as the build itself.

### Deprecated Features

* COMMON: The configuration options ```includeProjectNameInTag``` and ```tagPrefix``` are deprecated and will be remove with 3.0. Please migrate to tagTemplate. (#96)
* COMMON: Depending on any internal release task like **createReleaseTag** is highly discouraged. Use the **beforeReleaseBuild or **afterReleaseBuild** tasks.

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
