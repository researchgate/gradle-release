package net.researchgate.release.task

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.PluginHelper
import net.researchgate.release.ReleaseExtension
import net.researchgate.release.adapter.DryRunAdapter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project

class BaseScmTask extends DefaultTask {

    ReleaseExtension extension

    PluginHelper pluginHelper

    /**
     * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
     * @param directory the directory to start from
     */
    protected BaseScmAdapter getScmAdapter() {
        println(pluginHelper.scmAdapter)
        if (pluginHelper.scmAdapter != null) {
            return pluginHelper.scmAdapter
        }
        if (pluginHelper.findProperty('release.dryRun', false)) {
            pluginHelper.scmAdapter = new DryRunAdapter(project)
            return pluginHelper.scmAdapter
        }

        BaseScmAdapter adapter
        File projectPath = project.projectDir.canonicalFile

        extension.scmAdapters.find {
            assert BaseScmAdapter.isAssignableFrom(it)

            BaseScmAdapter instance = it.getConstructor(Project.class).newInstance(project)
            if (instance.isSupported(projectPath)) {
                adapter = instance
                return true
            }

            return false
        }

        if (adapter == null) {
            throw new GradleException(
                    "No supported Adapter could be found. Are [${ projectPath }] or its parents are valid scm directories?")
        }

        pluginHelper.scmAdapter = adapter
        return adapter
    }

}
