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

        project.task( 'checkCommitNeeded' ) << this.&checkCommitNeeded
        project.task( 'checkUpdateNeeded' ) << this.&checkUpdateNeeded
        project.task( 'createReleaseTag'  ) << this.&createReleaseTag
    }

    abstract void init ()
    abstract void checkCommitNeeded()
    abstract void checkUpdateNeeded()
    abstract void createReleaseTag ()

    @Requires({ message })
    abstract void commit ( String message  )
}
