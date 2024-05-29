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
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @version 1.0
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
			if ( !(obj instanceof DelayedInteger) ) {
				return false;
			}
			DelayedInteger other = (DelayedInteger) obj;
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
			.allSatisfy((k, v) -> {
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
