package release

import org.gradle.api.Project
import org.gradle.api.Plugin

/**
 * @author elberry
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin implements Plugin<Project> {
   void apply (Project project) {
      project.convention.plugins.GitReleasePlugin = new GitReleasePluginConvention()
      
      // add your plugin tasks here.
   }
}