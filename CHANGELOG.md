# Changelog


## 2.0.0
##### Released: 8. Feb 2015

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
