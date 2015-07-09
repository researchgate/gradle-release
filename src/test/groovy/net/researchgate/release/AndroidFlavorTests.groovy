package net.researchgate.release

import net.researchgate.release.android.AndroidFlavor
import spock.lang.Specification

class AndroidFlavorTests extends Specification {

    def 'build command is correct for a flavor'() {
        when:
        def androidFlavor = new AndroidFlavor('prodEnv1')
        then:
        assert androidFlavor.getBuildCommand() == 'assembleprodEnv1Release'
    }

    def 'test command is correct for a flavor'() {
        when:
        def androidFlavor = new AndroidFlavor('prodEnv1')
        then:
        assert androidFlavor.getTestCommand() == 'testprodEnv1Release'
    }
}
