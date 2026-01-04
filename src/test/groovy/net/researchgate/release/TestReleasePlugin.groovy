package net.researchgate.release

abstract class TestReleasePlugin extends ReleasePlugin {

    @Override
    protected BaseScmAdapter findScmAdapter(PluginHelper pluginHelper) {
        return new TestAdapter(pluginHelper)
    }
}
