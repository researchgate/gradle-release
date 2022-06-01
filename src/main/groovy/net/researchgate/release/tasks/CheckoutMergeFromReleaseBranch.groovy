package net.researchgate.release.tasks

import org.gradle.api.tasks.TaskAction

class CheckoutMergeFromReleaseBranch extends BaseReleaseTask {

    CheckoutMergeFromReleaseBranch() {
        super()
        description = 'Checkout to the main branch, and merge modifications from the release branch in working tree.'
    }

    @TaskAction
    void checkoutAndMergeToReleaseBranch() {
        if (extension.pushReleaseVersionBranch.get() && !extension.failOnCommitNeeded.get()) {
            log.warn('/!\\Warning/!\\')
            log.warn('It is strongly discouraged to set failOnCommitNeeded to false with pushReleaseVersionBranch is enabled.')
            log.warn('Merging with an uncleaned working directory will lead to unexpected results.')
        }

        scmAdapter.checkoutMergeFromReleaseBranch()
    }
}
