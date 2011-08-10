package release

import org.gradle.api.Project
import org.gradle.api.Plugin

/**
 * @author elberry
 * Created: Tue Aug 09 23:25:18 PDT 2011
 */
class SvnReleasePlugin implements Plugin<Project> {
   void apply (Project project) {
      project.convention.plugins.SvnReleasePlugin = new SvnReleasePluginConvention()
      
      // add your plugin tasks here.
   }
}