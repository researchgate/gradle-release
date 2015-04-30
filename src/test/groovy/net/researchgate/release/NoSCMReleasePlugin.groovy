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

class NoSCMReleasePlugin extends BaseScmPlugin {

    NoSCMReleasePlugin(Project project) {
        super(project)
    }

    @Override
    void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void checkCommitNeeded() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void checkUpdateNeeded() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void createReleaseTag(String message = "") {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void commit(String message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void revert() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
