/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release.cli

import spock.lang.Specification

public class ExecutorTests extends Specification {

    Executor executor

    def setup() {
        executor = new Executor()
    }

    def 'supplied envs are taken'() {
        given:
        String output = executor.exec(['env'], env: ['TEST_RELEASE': 1234])
        expect:
        output.findAll(/(?m)^TEST_RELEASE=1234$/).size() == 1
    }

    def 'system envs are merged'() {
        given:
        String output = executor.exec(['env'])
        expect:
        output.findAll(/(?m)^PATH=/).size() == 1
    }

    def 'supplied envs overwrite system envs'() {
        given:
        String output = executor.exec(['env'], env: ['PATH': 1234])
        expect:
        output.findAll(/(?m)^PATH=1234$/).size() == 1
        output.findAll(/(?m)^PATH=/).size() == 1
    }
}
