package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CheckoutAndMergeToReleaseBranch extends BaseReleaseTask {

    @TaskAction
    void checkoutAndMergeToReleaseBranch() {
        if (extension.pushReleaseVersionBranch.isPresent() != null && !extension.failOnCommitNeeded.get()) {
            log.warn('/!\\Warning/!\\')
            log.warn('It is strongly discouraged to set failOnCommitNeeded to false with pushReleaseVersionBranch is enabled.')
            log.warn('Merging with an uncleaned working directory will lead to unexpected results.')
        }

        getScmAdapter().checkoutMergeToReleaseBranch()
    }
}
