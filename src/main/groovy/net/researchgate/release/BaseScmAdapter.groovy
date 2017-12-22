/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import org.gradle.api.Project

abstract class BaseScmAdapter extends PluginHelper {

    BaseScmAdapter(Project project) {
        super(project.getExtensions().getByName('release'), project)
    }

    abstract Object createNewConfig()

    abstract boolean isSupported(File directory)

    abstract void init()

    abstract void checkCommitNeeded()

    abstract void checkUpdateNeeded()

    abstract void createReleaseTag(String message)

    abstract void add(File file)

    abstract void commit(String message)

    abstract void revert()
}
