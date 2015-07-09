package net.researchgate.release.android

class AndroidFlavor {

    private String flavorName

    AndroidFlavor(String flavorName) {
        this.flavorName = flavorName
    }

    String getBuildCommand() {
        return "assemble${flavorName}Release"
    }

    String getTestCommand() {
        return "test${flavorName}Release"
    }

}
