package net.researchgate.release.adapter

import net.researchgate.release.BaseScmAdapter
import org.gradle.api.Project

class DryRunAdapter extends BaseScmAdapter {

    DryRunAdapter(Project project) {
        super(project)
    }

    @Override
    Object createNewConfig() {
        return null
    }

    @Override
    boolean isSupported(File directory) {
        return false
    }

    @Override
    void init() {

    }

    @Override
    void checkCommitNeeded() {

    }

    @Override
    void checkUpdateNeeded() {

    }

    @Override
    void createReleaseTag(String message) {

    }

    @Override
    void add(File file) {

    }

    @Override
    void commit(String message) {

    }

    @Override
    void revert() {

    }
}
