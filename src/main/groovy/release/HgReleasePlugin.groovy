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
        project.logger.info( 'checkCommitNeeded' )
    }

    void checkUpdateNeeded() {
        project.logger.info( 'checkUpdateNeeded' )
    }

    void createReleaseTag() {
        project.logger.info( 'createReleaseTag' )
    }

    void commit ( String message ) {
        project.logger.info( 'commit' )
    }
}