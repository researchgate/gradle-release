package release

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author elberry
 * Created: Tue Aug 09 15:32:00 PDT 2011
 */
class ReleasePlugin implements Plugin<Project> {
   void apply(Project project) {

      applyScmPlugin(project)

      project.convention.plugins.release = new ReleasePluginConvention()

      project.task('validateSource', dependsOn: ['checkUpdateNeeded', 'checkCommitNeeded']) {}

      project.task('release', dependsOn: ['checkUpdateNeeded', 'checkCommitNeeded']) {
         // Release task should perform the following tasks.
         //  1. Check to see if source is out of date
         //  2. Check to see if source needs to be checked in.
         //  3. Check for SNAPSHOT dependencies if required.
         //  4. Build && run Unit tests
         //  5. Run any other tasks the user specifies in convention.
         //  6. Update Snapshot version if used
         //  7. Commit Snapshot update (if done)
         //  8. Create tag of release.
         //  9. Update version to next version.
         // 10. Commit version update.
      }
   }

   /**
    * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
    * @param project
    */
   private void applyScmPlugin(Project project) {
      // apply scm tasks
      project.projectDir.eachDir {
         switch (it.name) {
            case '.svn':
               project.apply plugin: SvnReleasePlugin
               return
            case '.bzr':
               project.apply plugin: BzrReleasePlugin
               return
            case '.git':
               project.apply plugin: GitReleasePlugin
               return
            case '.hg':
               // not supported
               return
         }
      }
   }
}