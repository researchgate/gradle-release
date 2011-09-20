package release

import org.gradle.api.GradleException

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends BaseScmPlugin {

    private static final String LINE              = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
    private static final String NOTHING_TO_COMMIT = 'nothing to commit (working directory clean)'


    private GitReleasePluginConvention gitConvention(){ convention( 'GitReleasePlugin', GitReleasePluginConvention) }
    private List<String>               gitStatus()    { exec( 'git', 'status' ).readLines() }


    void init () {

        setConvention( 'GitReleasePlugin', new GitReleasePluginConvention())

        if ( gitConvention().requireBranch ) {

            def branch = gitStatus()[ 0 ].trim().find( /^# On branch (\S+)$/ ){ it[ 1 ] }

            if ( ! ( branch == gitConvention().requireBranch )) {
                throw new GradleException( "Current Git branch is \"$branch\" and not \"${ gitConvention().requireBranch }\"." )
            }
        }
    }


    void checkCommitNeeded () {

        def status = gitStatus()

        if ( ! status.grep( NOTHING_TO_COMMIT )) {
            throw new GradleException(([ 'You have uncommitted or unversioned files:', LINE, *status, LINE ] as String[] ).
                                      join( '\n' ))
        }
    }


    void checkUpdateNeeded () {

        exec([ 'git', 'remote', 'update' ], 'aaaaaa', 'aaaaaa' )

        def status    = gitStatus()
        def noUpdates = ( status.size() == 2 ) &&
                        ( status[ 0 ].startsWith( '# On branch ' )) &&
                        ( status[ 1 ] == NOTHING_TO_COMMIT )

        if ( ! noUpdates ) {
            throw new GradleException(( [ 'You have remote changes to pull or local changes to push', LINE, *status, LINE ] as String[] ).
                                      join( '\n' ))
        }
    }


    void createReleaseTag () {
        def version = project.version
        exec([ 'git', 'tag', '-a',      version, '-m', 'v' + version ], "Duplicate tag [$version]", 'already exists' )
        exec([ 'git', 'push', 'origin', version ], 'aaaa', 'aaaaaa' )
    }


    void commit ( String message ) {
        exec([ 'git', 'commit', '-a', '-m', message ], 'aaaa', 'aaaaaa' )
        exec([ 'git', 'push', 'origin' ], 'aaaa', 'aaaaaa' )
    }
}