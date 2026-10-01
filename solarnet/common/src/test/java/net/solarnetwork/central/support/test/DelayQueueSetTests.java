/* ==================================================================
 * DelayQueueSetTests.java - 30/05/2024 10:40:29 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.support.test;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenObject;
import static org.mockito.Mockito.mock;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.DelayQueueSet;

/**
 * Test cases for the {@link DelayQueueSet} class.
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class DelayQueueSetTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final class DelayedInteger implements Delayed {

		private final int i;
		private final long expires;

		private DelayedInteger(int i, long ttl) {
			super();
			this.i = i;
			this.expires = System.currentTimeMillis() + ttl;
		}

		@Override
		public int compareTo(Delayed o) {
			// not bothering to check instanceof for performance
			DelayedInteger other = (DelayedInteger) o;
			int result = Long.compare(expires, other.expires);
			if ( result == 0 ) {
				// fall back to sort by string when expires are equal
				result = Integer.compare(i, other.i);
			}
			return result;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long ms = expires - System.currentTimeMillis();
			return unit.convert(ms, TimeUnit.MILLISECONDS);
		}

		@Override
		public int hashCode() {
			return Objects.hash(i);
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !(obj instanceof DelayedInteger other) ) {
				return false;
			}
			return i == other.i;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(i);
			builder.append("@");
			builder.append(expires);
			return builder.toString();
		}

	}

	private static DelayedInteger i(int i) {
		return i(i, 1000);
	}

	private static DelayedInteger i(int i, long ttl) {
		return new DelayedInteger(i, ttl);
	}

	@Test
	public void offerDuplicate() {
		// GIVEN
		var queue = new DelayQueueSet<DelayedInteger>(10);

		// WHEN
		then(queue.offer(i(123))).as("First offer returns true").isTrue();
		then(queue.offer(i(123))).as("Offer duplicate returns true").isTrue();
	}

	@Test
	public void construct_nonEmptyDelegateSet() {
		// GIVEN
		final var delegateSet = new HashSet<DelayedInteger>(List.of(i(1), i(2)));

		// WHEN
		final Throwable result = catchThrowable(() -> new DelayQueueSet<>(delegateSet));

		// THEN
		// @formatter:off
		then(result)
			.as("Non-empty delegate set rejected, as its elements would never be queued")
			.isInstanceOf(IllegalArgumentException.class)
			;
		// @formatter:on
	}

	@Test
	public void offer_queueThrows() {
		// GIVEN
		final var delegateSet = new HashSet<Delayed>();
		final var queue = new DelayQueueSet<Delayed>(delegateSet);

		// a DelayedInteger cannot be compared to some other Delayed type
		final Delayed other = mock(Delayed.class);
		queue.offer(other);

		// WHEN
		final Throwable result = catchThrowable(() -> queue.offer(i(1)));

		// THEN
		// @formatter:off
		then(result)
			.as("Comparison exception propagated")
			.isInstanceOf(ClassCastException.class)
			;
		then(queue)
			.as("Rejected element not queued")
			.containsExactly(other)
			;
		then(delegateSet)
			.as("Rejected element not left in delegate set")
			.containsExactly(other)
			;
		// @formatter:on

		// equal element can be offered once comparison no longer fails
		final DelayedInteger again = i(1);
		queue.remove(other);
		queue.offer(again);

		// @formatter:off
		then(queue)
			.as("Element equal to rejected element is queued")
			.containsExactly(again)
			;
		// @formatter:on
	}

	@Test
	public void remove_head_concurrentOfferOfEqualElement() throws Exception {
		// GIVEN
		final var offerThreads = new ArrayList<Thread>(2);
		final var queueRef = new AtomicReference<DelayQueueSet<DelayedInteger>>();
		final var delegateSet = new HashSet<DelayedInteger>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean remove(Object o) {
				// each time the delegate set is modified, have another thread
				// try to offer an equal element; that offer can only complete
				// before this method returns if the queue lock is not held
				final Thread t = new Thread(() -> queueRef.get().offer(i(1, 0)));
				offerThreads.add(t);
				t.start();
				try {
					t.join(200);
				} catch ( InterruptedException e ) {
					throw new RuntimeException(e);
				}
				return super.remove(o);
			}

		};
		final var queue = new DelayQueueSet<DelayedInteger>(delegateSet);
		queueRef.set(queue);

		final DelayedInteger first = i(1, 0);
		queue.offer(first);

		// WHEN
		final DelayedInteger result = queue.remove();
		for ( Thread t : offerThreads ) {
			t.join();
		}

		// THEN
		// @formatter:off
		then(result)
			.as("Expired head removed")
			.isSameAs(first)
			;
		then(queue)
			.as("Equal element offered during removal is queued once")
			.hasSize(1)
			;
		then(delegateSet)
			.as("Delegate set in sync with queue")
			.containsExactlyElementsOf(queue)
			;
		// @formatter:on

		queue.offer(i(1, 0));

		// @formatter:off
		then(queue)
			.as("Further equal element is a duplicate")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void clear() throws Exception {
		// GIVEN
		var queue = new DelayQueueSet<DelayedInteger>(10);

		// WHEN
		queue.add(i(123));
		queue.add(i(456));

		// @formatter:off
		thenObject(queue)
			.as("Reported size")
			.returns(2, from(DelayQueueSet::size))
			;

		queue.remove(i(456));

		thenObject(queue)
			.as("Reported size after removal")
			.returns(1, from(DelayQueueSet::size))
			;

		queue.clear();

		thenObject(queue)
			.as("Reported size after clear")
			.returns(0, from(DelayQueueSet::size))
			;
		// @formatter:on
	}

	@Test
	public void threaded() throws Exception {
		// GIVEN
		final RandomGenerator rng = new SecureRandom();
		final int producerCount = 4;
		final int consumerCount = 2;
		final ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);
		final AtomicInteger producerCounter = new AtomicInteger();
		final AtomicInteger consumerCounter = new AtomicInteger();

		final int maxCount = 3_000;
		final int rngMax = 25;
		final long delay = 500L;
		final var delegateSet = new LinkedHashSet<DelayedInteger>(rngMax);

		final var queue = new DelayQueueSet<DelayedInteger>(delegateSet);

		final var accepted = synchronizedList(new ArrayList<Integer>(maxCount));
		final var rejected = synchronizedList(new ArrayList<Integer>(maxCount));
		final var uniqueAccepted = synchronizedSet(new TreeSet<Integer>());

		final long start = System.currentTimeMillis();

		for ( int i = 0; i < producerCount; i++ ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						int count = producerCounter.incrementAndGet();
						if ( count > maxCount ) {
							log.info("Producer: maximum reached: {}/{}/{}", maxCount, accepted.size(),
									rejected.size());
							return;
						}

						int val = rng.nextInt(rngMax);

						if ( queue.offer(i(val, delay)) ) {
							log.debug("ADD: |{}", val);
							accepted.add(val);
							uniqueAccepted.add(val);
						} else {
							log.debug("REJ: |{}", val);
							rejected.add(val);
						}
						long sleep = rng.nextLong(10L, 50L);
						if ( sleep > 0 ) {
							log.debug("Producer: sleep {}", sleep);
							try {
								Thread.sleep(sleep);
							} catch ( InterruptedException e ) {
								// ignore
							}
						}
					}
				}

			});
		}

		final var consumed = synchronizedList(new ArrayList<DelayedInteger>(maxCount));

		for ( int i = 0; i < consumerCount; i++ ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						int count = consumerCounter.incrementAndGet();
						if ( count > maxCount ) {
							log.info("Consumer: maximum reached: {}", maxCount);
							return;
						}

						DelayedInteger val;
						try {
							val = queue.poll(500, TimeUnit.MILLISECONDS);
						} catch ( InterruptedException e ) {
							// stop
							log.info("Consumer: interrupted");
							return;
						}
						if ( val == null ) {
							log.info("Consumer: timeout");
							return;
						}
						consumed.add(val);
						log.debug("GET: |{}", val);
						long sleep = rng.nextLong(10L, 100L);
						if ( sleep > 0 ) {
							log.debug("Consumer: sleep {}", sleep);
							try {
								Thread.sleep(sleep);
							} catch ( InterruptedException e ) {
								// ignore
							}
						}
					}
				}

			});
		}

		Thread.sleep(1_000);

		// let producers go until max reached
		executor.shutdown();
		then(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

		final long end = System.currentTimeMillis();

		// THEN
		// @formatter:off
		then(accepted.size() + rejected.size())
			.as("Handled all")
			.isEqualTo(maxCount)
			;
		
		final var consumedCountsByValue = consumed.stream().collect(groupingBy(e -> e.i, counting()));
		final int expectedMaxCountPerValue = (int)((end - start) / delay);

		log.info("Consumed {} counts over {}ms (expected max count is {}): {}", 
				consumed.size(),
				(end - start),
				expectedMaxCountPerValue,
				consumedCountsByValue);
		
		then(consumedCountsByValue)
			.allSatisfy((_, v) -> {
				then(v)
					.as("Should have consumed each value no more than maximum allowed by delay over run time")
					.isLessThanOrEqualTo(expectedMaxCountPerValue)
					;
			})
			;

		final var uniqueConsumed = consumed.stream().map(e -> e.i).collect(toCollection(TreeSet::new));
		then(uniqueConsumed)
			.as("All unique keys consumed")
			.isEqualTo(uniqueAccepted)
			;
		
		
		then(queue).as("Nothing left in queue").isEmpty();
		then(delegateSet).as("Nothing left in delegate set").isEmpty();
		// @formatter:on
	}

}
