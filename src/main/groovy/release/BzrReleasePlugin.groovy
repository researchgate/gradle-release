package release

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.tools.ant.taskdefs.Exec

/**
 * @author elberry
 * Created: Tue Aug 09 23:26:04 PDT 2011
 */
class BzrReleasePlugin implements Plugin<Project> {
   void apply(Project project) {
      checkForXmlOutput()
      project.convention.plugins.BzrReleasePlugin = new BzrReleasePluginConvention()
      project.task('checkUpdateNeeded') << {
         Process process = ['bzr', 'xmlmissing'].execute()
         
      }
   }

   private void checkForXmlOutput() {
      StringBuilder out = new StringBuilder()
      StringBuilder err = new StringBuilder()
      Process process = ['bzr', 'plugins'].execute()
      process.waitForProcessOutput(out, err)
      boolean hasXmlOutput = false
      process.text.eachLine {
         if (it.startsWith('xmloutput')) {
            hasXmlOutput = true
         }
      }
      if (!hasXmlOutput) {
         throw new IllegalStateException("The required xmloutput plugin is not installed in Bazaar, please install it.")
      }
   }
}