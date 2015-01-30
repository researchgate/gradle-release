# Changelog


## 2.0.0
##### Released: 8. Feb 2015

### New Features

* Release version and new version as build parameters (#44, #67, thanks thokuest)
* Option to disable pushing to remote (#41, #33, thanks szpak)
* Option to change name of remote to push to

### Changes

* Package of classes had to be changed from ```release```to ```net.researchgate.release```. (This is only relevant for you if you imported some of the classes directly into your code.)
* Better error message if updating of version fails

### Bugfixes

* Fixed calling release plugin in sub-project with git repository in parent (#35, thanks szpak)
* Fixed setting requireBranch to empty string (#40, thanks szpak)
* Fixed problems with setting custom versionPropertyFile (#56, #57, #58, thanks jeffastorey)
* Fixed SVN release on windows (#39, #68, thanks thokuest)
* Only check for incoming and outgoing changes on current branch for Mercurial (#76, thanks litpho)
* Fix creating branch from correct revision (#48)
* Set proper environment for svn client (#52, #86)
