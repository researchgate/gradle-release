# Upgrade instructions

## 2.0 to 2.1

### Location of git configuration changed

In you build.gradle either replace:

```git.requireBranch = 'branchname'``` with ```release.git.requireBranch = 'branchname'```

or

```
git {
    option = value
}
```

with

```
release {
   git {
       option = value
   }
}
```

### Automatically uploading artifacts on release

Formally it was usually done by adding

```
createReleaseTag.dependsOn uploadArchives
```

This *should not* be done anymore. (Although this will still work, but may have undesired side effects)

Instead use

```
afterReleaseBuild.dependsOn uploadArchives
```

