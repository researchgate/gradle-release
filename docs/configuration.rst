Configuration
=============

Overview
--------

.. code-block:: groovy
    :caption: build.gradle

    release {
        failOnCommitNeeded = true
        failOnPublishNeeded = true
        failOnSnapshotDependencies = true
        failOnUnversionedFiles = true
        failOnUpdateNeeded = true
        revertOnFail = true
        preCommitText = ""
        preTagCommitMessage = "[Gradle Release Plugin] - pre tag commit: "
        tagCommitMessage = "[Gradle Release Plugin] - creating tag: "
        newVersionCommitMessage = "[Gradle Release Plugin] - new version commit: "
        includeProjectNameInTag = false

        requiredTasks = []
        versionPatterns = [
          /(\d+)([^\d]*$)/: { Matcher m, Project p -> m.replaceAll("${ (m[0][1] as int) + 1 }${ m[0][2] }") }
        ]

        git {

        }

        versionPropertyFile = 'gradle.properties'
        versionProperties = []
        tagPrefix = null
    }


Common
------

.. confval:: failOnCommitNeeded

    *Default: true*

    Fail the release process when there un-committed changes. Be aware that when set to false the release plugin
    might also commit your changes when commiting its own changes.

.. confval:: failOnPublishNeeded

    *Default: true*

    Fail when there are local commits that haven't been published upstream (has no effect for Subversion)

.. confval:: failOnSnapshotDependencies

    *Default: true*

    Fail when the project has dependencies on SNAPSHOT versions.

.. confval:: failOnUnversionedFiles

    *Default: true*

    Fail when files are found that are not under version control and not ignored in the VCS.

.. confval:: failOnUpdateNeeded

    *Default: true*

    Fail when the local checkout needs to be updated, or there are changes available upstream that haven't been pulled.
    Fail when files are found that are not under version control and not ignored in the VCS.

.. confval:: revertOnFail

    *Default: true*

    When a failure occurs the release plugin will try to revert as much as possible.

    Depending on the stage where the error occurred, it might not be possible to revert commits and changes.

.. confval:: preCommitText

    *Default: ''*

    Text that will be prepended to all commit messages done by the release plugin.

    This configuration can also be set from the commandline:

    ``$ gradle release -Prelease.preCommitText="my commit text"``

.. confval:: preTagCommitMessage

    *Default: '[Gradle Release Plugin] - pre tag commit: '*

    The commit message for the commit that happens after the version has been "unsnapshoted" (if needed) and before the
    tag is being created.

    .. note::
        If you are not using SNAPSHOT versions the preTag-commit is not going to happen and as a consequence this
        setting is not being used at all.

.. confval:: tagCommitMessage

    *Default: '[Gradle Release Plugin] - creating tag: '*

    The commit message for the commit that needs to happen in some VCS when creating a tag. Currently this setting is
    only used for subversion.

.. confval:: newVersionCommitMessage

    *Default: '[Gradle Release Plugin] - new version commit: '*

    The commit message for the commit that happens after the tag has been created and the next version is set in the properties file.

.. confval:: versionPropertyFile

    *Default: 'gradle.properties'*

    The name of the file that holds the version.

.. confval:: versionProperties

    *Default: []*

    The name of the properties that will be written to the versionPropertyFile (TBD link). 'version' is always added to this list.


Bazaar
------

no configuration available

Git
---

no configuration available

Mercurial
---------

no configuration available

Subversion
----------

no configuration available
