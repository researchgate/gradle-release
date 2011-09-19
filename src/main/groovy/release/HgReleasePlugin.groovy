package release

import org.gradle.api.Project

/**
 * @author elberry
 * @author evgenyg
 * Created: Wed Aug 10 23:44:15 PDT 2011
 */
class HgReleasePlugin extends BaseScmReleasePlugin {

    @Override
    void init (Project project) {
        project.convention.plugins.HgReleasePlugin = new HgReleasePluginConvention()
    }


    void checkCommitNeeded( Project project ) {
        println( 'checkCommitNeeded' )
    }

    void checkUpdateNeeded( Project project ) {
        println( 'checkUpdateNeeded' )
    }

    void commitNewVersion( Project project ) {
        println( 'commitNewVersion' )
    }

    void createReleaseTag( Project project ) {
        println( 'createReleaseTag' )
    }

    void preTagCommit( Project project ) {
        println( 'preTagCommit' )
    }
}