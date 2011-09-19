package release

import org.gradle.api.Project
import org.gradle.api.GradleException

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends BaseScmReleasePlugin {

    @Override
    void init ( Project project ) {
        project.convention.plugins.GitReleasePlugin = new GitReleasePluginConvention()
    }

    @Override
    void checkCommitNeeded (Project project) {
        def    status = exec( 'git', 'status' ).readLines()

        if ( ! status.grep( 'nothing to commit (working directory clean)' )) {
            throw new GradleException( [ 'You have uncommitted or unversioned files:',
                                         '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~',
                                         *status,
                                         '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~' ].join( '\n' ))
        }
    }

    @Override
    void checkUpdateNeeded (Project project) {
        println( '' )

    }

    @Override
    void commitNewVersion (Project project) {
        println( '' )

    }

    @Override
    void createReleaseTag (Project project) {
        println( '' )

    }

    @Override
    void preTagCommit (Project project) {
        println( '' )
    }
}