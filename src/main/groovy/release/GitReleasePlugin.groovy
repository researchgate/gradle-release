package release

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends PluginHelper implements Plugin<Project> {

    @Requires({ project })
    void apply(Project project) {
        project.convention.plugins.GitReleasePlugin = new GitReleasePluginConvention()

        project.task( 'checkCommitNeeded' ) << { checkCommitNeeded( project ) }
        project.task( 'checkUpdateNeeded' ) << { checkUpdateNeeded( project ) }
        project.task( 'commitNewVersion'  ) << { commitNewVersion( project ) }
        project.task( 'createReleaseTag'  ) << { createReleaseTag( project ) }
        project.task( 'preTagCommit'      ) << { preTagCommit( project ) }
    }


    def checkCommitNeeded( Project project ) {
        println( 'checkCommitNeeded' )
    }

    def checkUpdateNeeded( Project project ) {
        println( 'checkUpdateNeeded' )
    }

    def commitNewVersion( Project project ) {
        println( 'commitNewVersion' )
    }

    def createReleaseTag( Project project ) {
        println( 'createReleaseTag' )
    }

    def preTagCommit( Project project ) {
        println( 'preTagCommit' )
    }
}