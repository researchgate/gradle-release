package release

/**
 * @author elberry
 * @author evgenyg
 * @author szpak
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePluginConvention {
    String requireBranch = 'master'
    boolean pushToCurrentBranch = false
    String scmRootDir
}
