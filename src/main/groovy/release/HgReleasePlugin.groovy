package release

/**
 * @author elberry
 * @author evgenyg
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends BaseScmPlugin {

    void init () {
        project.convention.plugins.HgReleasePlugin = new HgReleasePluginConvention()
    }


    void checkCommitNeeded() {
        println( 'checkCommitNeeded' )
    }

    void checkUpdateNeeded() {
        println( 'checkUpdateNeeded' )
    }

    void createReleaseTag() {
        println( 'createReleaseTag' )
    }

    void commit ( String message ) {
        println( 'commit' )
    }
}