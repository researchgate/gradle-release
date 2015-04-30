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

abstract class BaseScmPlugin extends PluginHelper {

    BaseScmPlugin(Project project) {
        this.project = project
        extension = project.extensions['release'] as ReleaseExtension
    }

    abstract void init()

    abstract void checkCommitNeeded()

    abstract void checkUpdateNeeded()

    abstract void createReleaseTag(String message)

    abstract void commit(String message)

    abstract void revert()
}
