package release

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Base class for all SCM-specific plugins
 * @author evgenyg
 */
abstract class BaseScmPlugin extends PluginHelper implements Plugin<Project> {

    @Requires({ project })
    final void apply( Project project ) {

        this.project = project
        init()

        project.task( 'checkCommitNeeded' ) << { checkCommitNeeded() }
        project.task( 'checkUpdateNeeded' ) << { checkUpdateNeeded() }
        project.task( 'createReleaseTag'  ) << { createReleaseTag() }
    }

    abstract void init ()
    abstract void checkCommitNeeded()
    abstract void checkUpdateNeeded()
    abstract void createReleaseTag ()
    abstract void commit ( String message  )
}
