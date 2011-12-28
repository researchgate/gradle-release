package release

//import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Base class for all SCM-specific plugins
 * @author evgenyg
 */
abstract class BaseScmPlugin extends PluginHelper implements Plugin<Project> {

    //@Requires({ project })
    final void apply( Project project ) {

        this.project = project

        project.task( 'checkCommitNeeded', description: "Checks to see if there are any added, modified, removed, or un-versioned files.") << this.&checkCommitNeeded
        project.task( 'checkUpdateNeeded', description: "Checks to see if there are any incoming, or outgoing changes that haven't been applied locally.") << this.&checkUpdateNeeded
        project.task( 'createReleaseTag', description: "Creates a tag in SCM for the current (un-snapshotted) version.") << this.&createReleaseTag
    }

    abstract void init ()
    abstract void checkCommitNeeded()
    abstract void checkUpdateNeeded()
    abstract void createReleaseTag ()

    //@Requires({ message })
    abstract void commit ( String message  )
}
