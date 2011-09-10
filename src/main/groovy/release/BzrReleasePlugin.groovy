package release

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 23:26:04 PDT 2011
 */
class BzrReleasePlugin extends PluginHelper implements Plugin<Project> {

	private static final String ERROR = 'ERROR'
	private static final String DELIM = '\n  * '

	void apply(Project project) {
		checkForXmlOutput()
		project.convention.plugins.BzrReleasePlugin = new BzrReleasePluginConvention()
        project.task('checkCommitNeeded') << {

            String out   = exec( 'bzr', 'xmlstatus' )
            def xml      = new XmlSlurper().parseText( out )
            def added    = xml.added?.size()    ?: 0
            def modified = xml.modified?.size() ?: 0
            def removed  = xml.removed?.size()  ?: 0
            def unknown  = xml.unknown?.size()  ?: 0

            if ( added || modified || removed || unknown )
            {
                def c = { String name -> [ "${ capitalize( name )}:",
                                           xml."$name".file.collect{ it.text().trim() },
                                           xml."$name".directory.collect{ it.text().trim() } ].
                                         flatten().
                                         join( DELIM ) + '\n'
                }

                throw new GradleException(
                    'You have un-committed or un-known files:\n' +
                    ( added    ? c( 'added' )    : '' ) +
                    ( modified ? c( 'modified' ) : '' ) +
                    ( removed  ? c( 'removed' )  : '' ) +
                    ( unknown  ? c( 'unknown' )  : '' ))

            }
        }

		project.task('checkUpdateNeeded') << {
			String out  = exec( 'bzr', 'xmlmissing' )
			def xml     = new XmlSlurper().parseText( out )
			def extra   = "${xml.extra_revisions?.@size}"   ?: 0
			def missing = "${xml.missing_revisions?.@size}" ?: 0

			if (extra)
            {
				throw new GradleException(
                    [ "You have $extra unpublished change${extra > 1 ? 's' : ''}:",
                      xml.extra_revisions.logs.log.collect{
                          int cutPosition = 40
                          String message  = it.message.text()
                          message         = message.readLines()[0].substring( 0, Math.min( cutPosition, message.size())) +
                                            ( message.size() > cutPosition ? ' ..' : '' )
                          "[$it.revno]: [$it.timestamp][$it.committer][$message]"
                      } ].
                    flatten().
                    join( DELIM )
                )
			}

			if (missing)
            {
				throw new GradleException("You are missing $missing changes.")
			}
		}

		project.task('commitNewVersion') << {
			String newVersionCommitMessage = releaseConvention( project ).newVersionCommitMessage

			commit(newVersionCommitMessage)
		}

		project.task('createReleaseTag') << {
			String tag = project.properties.version

            exec( ['bzr', 'tag', tag], 'Error creating tag', ERROR )
		}

		project.task('preTagCommit') << {
			String preTagCommitMessage = releaseConvention( project ).preTagCommitMessage
			def props = project.properties
			if (props['usesSnapshot']) {
				// should only be changes if the project was using a snapshot version.
				commit(preTagCommitMessage)
			}
		}
	}

	private void checkForXmlOutput() {
		assert exec( 'bzr', 'plugins' ).readLines().any{ it.startsWith( 'xmloutput' ) } , \
		       'The required xmloutput plugin is not installed in Bazaar, please install it.'
	}


    private void commit(String message) {
        exec( ['bzr', 'ci', '-m', message], 'Error committing new version', ERROR )
        exec( ['bzr', 'push', ':parent'],   'Error committing new version', ERROR )
    }
}