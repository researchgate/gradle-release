package net.researchgate.release

/**
 * Created with IntelliJ IDEA.
 * User: eugenesh
 * Date: 15.12.12
 * Time: 0:54
 * To change this template use File | Settings | File Templates.
 */
class NoSCMReleasePlugin extends BaseScmPlugin<NoSCMReleasePluginConvention> {
    @Override
    NoSCMReleasePluginConvention buildConventionInstance() {
        return new NoSCMReleasePluginConvention()
    }

    @Override
    void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void checkCommitNeeded() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void checkUpdateNeeded() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void createReleaseTag(String message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void commit(String message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void revert() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
