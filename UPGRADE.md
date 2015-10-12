# Upgrade instructions

## 2.0 to 2.1

### Location of git configuration changed

**MANDATORY**

If you are using one or more of the git options (```requireBranch```, ```pushToRemote``` or ```pushToCurrentBranch```) you need to adapt your configuration.

In you build.gradle either replace:

```git.<option> = '<value>'``` with ```release.git.<option> = '<value>'```

or

```
git {
    <option> = <value>
}
```

with

```
release {
   git {
       <option> = <value>
   }
}
```

### Automatically uploading artifacts on release

**RECOMMENDED**

Formally it was usually done by adding

```
createReleaseTag.dependsOn uploadArchives
```

This **should not** be done anymore. (Although this will still continue to work, but may have undesired side effects in the future)

Instead use

```
afterReleaseBuild.dependsOn uploadArchives
```

### Migrate to new ``tagTemplate`` configuration

**RECOMMENDED**

If you are using either ``includeProjectNameInTag`` or ``tagPrefix`` you should consider moving to ``tagTemplate``. The two options have been deprecated and will be removed in the future.

These two scenarios should make it easy to get the same result as before:

##### Scenario #1

* ``includeProjectNameInTag`` not set or set to false
* ``tagPrefix`` set to "something"

Remove ``includeProjectNameInTag`` or ``tagPrefix`` and add
```
release {
    tagTemplate = 'something-$version'
}
```

##### Scenario #2

* ``includeProjectNameInTag`` set to true

Remove ``includeProjectNameInTag`` or ``tagPrefix`` and add

```
release {
    tagTemplate = '$name-$version'
}
```


