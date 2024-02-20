package net.researchgate.release

import org.gradle.build.event.BuildEventsListenerRegistry

import javax.inject.Inject

interface BuildEventsListenerRegistryProvider {
   @Inject
   BuildEventsListenerRegistry getBuildEventsListenerRegistry();
}
