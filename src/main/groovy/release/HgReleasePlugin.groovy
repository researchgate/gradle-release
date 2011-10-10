package release

/**
 * @author elberry
 * @author evgenyg
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends BaseScmPlugin {

    void init () {
        setConvention( 'HgReleasePlugin', new HgReleasePluginConvention())
    }


    void checkCommitNeeded() {
        log.info( 'checkCommitNeeded' )
    }

    void checkUpdateNeeded() {
        log.info( 'checkUpdateNeeded' )
    }

    void createReleaseTag() {
        log.info( 'createReleaseTag' )
    }

    void commit ( String message ) {
        log.info( 'commit' )
    }
}