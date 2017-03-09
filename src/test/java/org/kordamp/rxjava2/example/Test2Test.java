/*
 * Copyright 2016-2017 Andres Almiray
 *
 * This file is part of Testing RxJava2
 *
 * Testing RxJava2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Testing RxJava2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Testing RxJava2. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kordamp.rxjava2.example;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class Test2Test {
    private static class ImmediateSchedulersRule implements TestRule {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
                    RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
                    RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> Schedulers.trampoline());

                    try {
                        base.evaluate();
                    } finally {
                        RxJavaPlugins.reset();
                    }
                }
            };
        }
    }

    private static final List<String> WORDS = Arrays.asList(
        "the",
        "quick",
        "brown",
        "fox",
        "jumped",
        "over",
        "the",
        "lazy",
        "dog"
    );

    @Rule
    public final ImmediateSchedulersRule schedulers = new ImmediateSchedulersRule();

    @Test
    public void testUsingImmediateSchedulersRule() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribeOn(Schedulers.computation())
            .subscribe(observer);

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);
        assertThat(observer.values(), hasItem(" 4. fox"));
    }
}
