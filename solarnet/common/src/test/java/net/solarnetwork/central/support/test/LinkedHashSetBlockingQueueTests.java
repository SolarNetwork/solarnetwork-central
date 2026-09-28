/* ==================================================================
 * LinkedHashSetBlockingQueueTests.java - 17/04/2024 1:01:30 pm
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
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;

/**
 * Test cases for the {@link LinkedHashSetBlockingQueue} class.
 * 
 * <p>
 * See {@code LinkedHashSetBlockingQueueConcurrencyTests} for the blocking and
 * inter-thread hand-off behavior.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class LinkedHashSetBlockingQueueTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void offerDuplicate() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);

		// WHEN
		then(queue.offer("123")).as("First offer returns true").isTrue();
		then(queue.offer("123")).as("Offer duplicate returns true").isTrue();
	}

	@Test
	public void offerFull() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(2);

		// WHEN
		then(queue.offer("123")).as("First offer returns true").isTrue();
		then(queue.offer("234")).as("Second offer returns true").isTrue();
		then(queue.offer("345")).as("Thrid offer fails (at capacity)").isFalse();
	}

	@Test
	public void clear() throws Exception {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);

		// WHEN
		queue.add("123");
		queue.add("456");

		// @formatter:off
		thenObject(queue)
			.as("Reported size")
			.returns(2, from(LinkedHashSetBlockingQueue::size))
			;

		queue.remove("456");

		thenObject(queue)
			.as("Reported size after removal")
			.returns(1, from(LinkedHashSetBlockingQueue::size))
			;

		queue.clear();

		thenObject(queue)
			.as("Reported size after clear")
			.returns(0, from(LinkedHashSetBlockingQueue::size))
			;
		// @formatter:on
	}

	@Test
	public void offerDuplicateWhenFull() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(2);
		queue.offer("123");
		queue.offer("234");

		// WHEN
		final boolean result = queue.offer("123");

		// THEN
		// @formatter:off
		then(result)
			.as("Offer of an already-queued element is still rejected when at capacity")
			.isFalse()
			;

		then(queue)
			.as("Queue unchanged")
			.containsExactly("123", "234")
			;
		// @formatter:on
	}

	@Test
	public void putDuplicate() throws Exception {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(2);
		queue.offer("123");

		// WHEN
		queue.put("123");

		// THEN
		// @formatter:off
		then(queue)
			.as("Duplicate silently discarded, so queue has not grown")
			.containsExactly("123")
			;

		then(queue.remainingCapacity())
			.as("Capacity not consumed by the discarded duplicate")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void offerDuplicateDoesNotReorder() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");

		// WHEN
		queue.offer("123");

		// THEN
		// @formatter:off
		then(queue.poll())
			.as("Re-offered element keeps its original queue position")
			.isEqualTo("123")
			;

		then(queue.poll())
			.as("Followed by the second element")
			.isEqualTo("234")
			;

		then(queue.poll())
			.as("Nothing else queued")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void constructorNegativeCapacity() {
		// WHEN
		// THEN
		// @formatter:off
		thenExceptionOfType(IllegalArgumentException.class)
			.as("A negative capacity is rejected, as it would otherwise produce an unbounded "
					+ "queue whose offer() always fails")
			.isThrownBy(() -> new LinkedHashSetBlockingQueue<String>(new LinkedHashSet<>(), -1))
			;
		// @formatter:on
	}

	@Test
	public void zeroCapacity() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(0);

		// WHEN
		final boolean result = queue.offer("123");

		// THEN
		// @formatter:off
		then(result)
			.as("A zero-capacity queue never accepts an element")
			.isFalse()
			;

		thenObject(queue)
			.as("Reported size")
			.returns(0, from(LinkedHashSetBlockingQueue::size))
			.as("Reported remaining capacity")
			.returns(0, from(LinkedHashSetBlockingQueue::remainingCapacity))
			;
		// @formatter:on
	}

	@Test
	public void drainToMaxElements() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");
		queue.offer("345");

		// WHEN
		final List<String> dest = new ArrayList<>(3);
		final int result = queue.drainTo(dest, 2);

		// THEN
		// @formatter:off
		then(result)
			.as("Drained count")
			.isEqualTo(2)
			;

		then(dest)
			.as("Drained in queue order")
			.containsExactly("123", "234")
			;

		then(queue)
			.as("Remaining element left queued")
			.containsExactly("345")
			;
		// @formatter:on
	}

	@Test
	public void drainToNonPositiveMaxElements() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");

		// WHEN
		final List<String> dest = new ArrayList<>(2);
		final int result = queue.drainTo(dest, -1);

		// THEN
		// @formatter:off
		then(result)
			.as("Nothing drained for a non-positive maximum")
			.isZero()
			;

		then(dest)
			.as("Destination untouched")
			.isEmpty()
			;

		then(queue.size())
			.as("Reported size unchanged; a negative maximum must not inflate the count")
			.isEqualTo(2)
			;

		then(queue)
			.as("Queue content unchanged")
			.containsExactly("123", "234")
			;
		// @formatter:on
	}

	@Test
	public void drainToSelf() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");

		// WHEN
		// THEN
		// @formatter:off
		thenExceptionOfType(IllegalArgumentException.class)
			.as("Draining into itself is rejected")
			.isThrownBy(() -> queue.drainTo(queue))
			;
		// @formatter:on
	}

	@Test
	public void drainToDestinationThrows() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");

		// a destination that accepts the first element, then fails
		final List<String> dest = new ArrayList<>(2) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean add(String e) {
				if ( size() >= 1 ) {
					throw new IllegalStateException("Destination full.");
				}
				return super.add(e);
			}
		};

		// WHEN
		thenExceptionOfType(IllegalStateException.class).isThrownBy(() -> queue.drainTo(dest));

		// THEN
		// @formatter:off
		then(dest)
			.as("Destination kept the element it accepted")
			.containsExactly("123")
			;

		then(queue.size())
			.as("Reported size accounts only for the element actually drained")
			.isEqualTo(1)
			;

		then(queue.poll())
			.as("Undrained element still available")
			.isEqualTo("234")
			;

		then(queue.poll())
			.as("Queue empty, so poll() reports null rather than failing on an inconsistent count")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void peek() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);

		// WHEN
		// THEN
		// @formatter:off
		then(queue.peek())
			.as("Peek of empty queue returns null")
			.isNull()
			;

		queue.offer("123");
		queue.offer("234");

		then(queue.peek())
			.as("Peek returns the head element")
			.isEqualTo("123")
			;

		then(queue.size())
			.as("Peek does not remove")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	@Test
	public void contains() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");

		// WHEN
		// THEN
		// @formatter:off
		then(queue.contains("123"))
			.as("Queued element found")
			.isTrue()
			;

		then(queue.contains("234"))
			.as("Element never queued not found")
			.isFalse()
			;

		then(queue.contains(null))
			.as("Null never found, rather than throwing")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void iteratorIsSnapshot() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");

		// WHEN
		final Iterator<String> itr = queue.iterator();
		queue.poll();
		queue.offer("345");

		// THEN
		// @formatter:off
		then(itr)
			.as("Iterator sees the elements present when it was created, and so does not throw "
					+ "ConcurrentModificationException after a concurrent change")
			.toIterable()
			.containsExactly("123", "234")
			;
		// @formatter:on
	}

	@Test
	public void iteratorRemove() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");

		// WHEN
		final Iterator<String> itr = queue.iterator();
		itr.next();
		itr.remove();

		// THEN
		// @formatter:off
		then(queue.size())
			.as("Reported size after iterator removal")
			.isEqualTo(1)
			;

		then(queue)
			.as("Removed element gone from the queue")
			.containsExactly("234")
			;

		then(queue.remainingCapacity())
			.as("Capacity released by the iterator removal")
			.isEqualTo(9)
			;
		// @formatter:on
	}

	@Test
	public void iteratorRemoveBeforeNext() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		final Iterator<String> itr = queue.iterator();

		// WHEN
		// THEN
		// @formatter:off
		thenExceptionOfType(IllegalStateException.class)
			.as("Removal before next() is rejected, so the count cannot drift")
			.isThrownBy(itr::remove)
			;

		then(queue.size())
			.as("Reported size unchanged")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void removeIf() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(10);
		queue.offer("123");
		queue.offer("234");
		queue.offer("345");

		// WHEN
		final boolean result = queue.removeIf(e -> e.startsWith("2"));

		// THEN
		// @formatter:off
		then(result)
			.as("Elements removed")
			.isTrue()
			;

		then(queue.size())
			.as("Reported size after bulk removal via the iterator")
			.isEqualTo(2)
			;

		then(queue)
			.as("Matching element removed")
			.containsExactly("123", "345")
			;
		// @formatter:on
	}

	@Test
	public void remainingCapacity() {
		// GIVEN
		var queue = new LinkedHashSetBlockingQueue<String>(2);

		// WHEN
		// THEN
		// @formatter:off
		then(queue.remainingCapacity())
			.as("Empty queue has full capacity available")
			.isEqualTo(2)
			;

		queue.offer("123");

		then(queue.remainingCapacity())
			.as("Capacity consumed by queued element")
			.isEqualTo(1)
			;

		queue.offer("123");

		then(queue.remainingCapacity())
			.as("Discarded duplicate consumes no capacity")
			.isEqualTo(1)
			;

		queue.offer("234");

		then(queue.remainingCapacity())
			.as("Full queue has no capacity available")
			.isZero()
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
		final int queueSize = 20;
		final int rngMax = queueSize * 5;
		final var delegateSet = new LinkedHashSet<Integer>(queueSize);

		final var queue = new LinkedHashSetBlockingQueue<Integer>(delegateSet, queueSize);

		final var accepted = synchronizedList(new ArrayList<Integer>(maxCount));
		final var rejected = synchronizedList(new ArrayList<Integer>(maxCount));

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

						Integer val = rng.nextInt(rngMax);

						if ( queue.offer(val) ) {
							log.debug("ADD: |{}", val);
							accepted.add(val);
						} else {
							log.debug("REJ: |{}", val);
							rejected.add(val);
						}
						long sleep = count > maxCount * 0.75 ? 30
								: Math.max(0, (count - maxCount / 2) / 4);
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

		final var consumed = synchronizedList(new ArrayList<>(maxCount));

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

						Integer val;
						try {
							val = queue.poll(2, TimeUnit.SECONDS);
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
						long sleep = rng.nextLong(10L, 400L);
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

		// THEN
		// @formatter:off
		then(accepted.size() + rejected.size())
			.as("Handled all")
			.isEqualTo(maxCount)
			;
		// @formatter:on
	}

}
