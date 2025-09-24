package net.researchgate.release

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult

/**
 * Listener to revert SCM changes on release task failure.
 *
 * <p>Requires Gradle 6.1+
 */
abstract class RevertOnFailedReleaseTaskCompletionListener
        implements BuildService<Params>, OperationCompletionListener {
    interface Params extends BuildServiceParameters {
        Property<BaseScmAdapter.Cacheable> getScmAdapter();
    }

    @Override
    void onFinish(FinishEvent finishEvent) {
        if ((finishEvent.result instanceof TaskFailureResult) && finishEvent.descriptor.taskPath.endsWith(':release')) {
            parameters.scmAdapter.get().revert()
        }
    }
}
