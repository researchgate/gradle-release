package release

class TestReleasePlugin extends ReleasePlugin {
    @Override
    protected Class findScmType(File directory) {
        return NoSCMReleasePlugin
    }
}
