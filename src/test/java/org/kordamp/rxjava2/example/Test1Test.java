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
import io.reactivex.schedulers.TestScheduler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

public class Test1Test {
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

    @Test
    public void testFailure() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Exception exception = new RuntimeException("boom!");

        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string))
            .concatWith(Observable.error(exception));

        // when:
        observable.subscribe(observer);

        // then:
        observer.assertError(exception);
        observer.assertNotComplete();
    }

    @Test
    public void testInSameThread() {
        // given:
        List<String> results = new ArrayList<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribe(results::add);

        // then:
        assertThat(results, notNullValue());
        assertThat(results, hasSize(9));
        assertThat(results, hasItem(" 4. fox"));
    }

    @Test
    public void testUsingTestObserver() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribe(observer);

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);
        assertThat(observer.values(), hasItem(" 4. fox"));
    }

    @Test
    public void testUsingBlockingCall() {
        // given:
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        Iterable<String> results = observable
            .subscribeOn(Schedulers.computation())
            .blockingIterable();

        // then:
        assertThat(results, notNullValue());
        assertThat(results, iterableWithSize(9));
        assertThat(results, hasItem(" 4. fox"));
    }

    @Test
    public void testUsingComputationScheduler() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribeOn(Schedulers.computation())
            .subscribe(observer);

        observer.awaitTerminalEvent(2, SECONDS);

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem(" 4. fox"));
    }

    @Test
    public void testUsingComputationScheduler_awatility() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribeOn(Schedulers.computation())
            .subscribe(observer);

        await().timeout(2, SECONDS)
            .until(observer::valueCount, equalTo(9));

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem(" 4. fox"));
    }

    @Test
    public void testUsingRxJavaHooksWithImmediateScheduler() {
        // given:
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> String.format("%2d. %s", index, string));

        try {
            // when:
            observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);

            // then:
            observer.assertComplete();
            observer.assertNoErrors();
            observer.assertValueCount(9);
            assertThat(observer.values(), hasItem(" 4. fox"));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void testUsingTestScheduler() {
        // given:
        TestScheduler scheduler = new TestScheduler();
        TestObserver<String> observer = new TestObserver<>();
        Observable<Long> tick = Observable.interval(1, SECONDS, scheduler);

        Observable<String> observable = Observable.fromIterable(WORDS)
            .zipWith(tick,
                (string, index) -> String.format("%2d. %s", index, string));

        observable.subscribeOn(scheduler)
            .subscribe(observer);

        // expect:
        observer.assertNoValues();
        observer.assertNotComplete();

        // when:
        scheduler.advanceTimeBy(1, SECONDS);

        // then:
        observer.assertNoErrors();
        observer.assertValueCount(1);
        observer.assertValues(" 0. the");

        // when:
        scheduler.advanceTimeTo(9, SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);
    }
}
