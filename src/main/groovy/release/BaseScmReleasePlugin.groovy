package release

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Base class for all SCM-specific plugins
 * @author evgenyg
 */
abstract class BaseScmReleasePlugin extends PluginHelper implements Plugin<Project> {

    @Requires({ project })
    final void apply( Project project ) {

        init( project )

        project.task( 'checkCommitNeeded' ) << { checkCommitNeeded( project ) }
        project.task( 'checkUpdateNeeded' ) << { checkUpdateNeeded( project ) }
        project.task( 'commitNewVersion'  ) << { commitNewVersion( project ) }
        project.task( 'createReleaseTag'  ) << { createReleaseTag( project ) }
        project.task( 'preTagCommit'      ) << { preTagCommit( project ) }
    }

    abstract void init             ( Project project )
    abstract void checkCommitNeeded( Project project )
    abstract void checkUpdateNeeded( Project project )
    abstract void commitNewVersion ( Project project )
    abstract void createReleaseTag ( Project project )
    abstract void preTagCommit     ( Project project )
}
