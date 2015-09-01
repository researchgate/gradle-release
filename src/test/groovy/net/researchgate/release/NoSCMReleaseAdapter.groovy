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

class NoSCMReleaseAdapter extends BaseScmAdapter {

    NoSCMReleaseAdapter(Project project) {
        super(project)
    }

    @Override
    Object createNewConfig() {
        return null
    }

    @Override
    boolean isSupported(File directory) {
        return true
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
	
	@Override
	String assignReleaseVersionAutomatically(String currentVersion) {
		//
	}
}
