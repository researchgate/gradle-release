package release

import org.gradle.api.GradleException

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends BaseScmPlugin {

    private static final String LINE              = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
    private static final String NOTHING_TO_COMMIT = 'nothing to commit (working directory clean)'


    void init () {
        project.convention.plugins.GitReleasePlugin = new GitReleasePluginConvention()
    }


    void checkCommitNeeded () {

        def status = exec( 'git', 'status' ).readLines()

        if ( ! status.grep( NOTHING_TO_COMMIT )) {
            throw new GradleException( [ 'You have uncommitted or unversioned files:',
                                       LINE, *status, LINE ].join( '\n' ))
        }
    }


    void checkUpdateNeeded () {

        exec( 'git', 'remote', 'update' )
        def status    = exec( 'git', 'status' ).readLines()
        def noUpdates = ( status.size() == 2 ) &&
                        ( status[ 0 ].startsWith( '# On branch ' )) &&
                        ( status[ 1 ] == NOTHING_TO_COMMIT )

        if ( ! noUpdates ) {
            throw new GradleException( [ 'You have remote changes to pull or local changes to push',
                                       LINE, *status, LINE ].join( '\n' ))
        }
    }


    void createReleaseTag () {
        exec([ 'git', 'tag', '-a', project.properties.version, '-m', 'v' + project.properties.version ], 'aaaa', 'aaaaaa' )
    }


    void commit ( String message ) {
        exec([ 'git', 'commit', '-a', '-m', message ], 'aaaa', 'aaaaaa' )
        exec([ 'git', 'push', 'origin' ], 'aaaa', 'aaaaaa' )
    }
}