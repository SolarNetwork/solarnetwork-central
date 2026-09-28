/* ==================================================================
 * LinkedHashSetBlockingQueueConcurrencyTests.java - 28/09/2026 10:34:17 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;

/**
 * Concurrency test cases for the {@link LinkedHashSetBlockingQueue} class.
 *
 * <p>
 * {@link java.util.concurrent.BlockingQueue BlockingQueue} implementations must
 * be thread safe, and every deployment shares a single instance between the
 * threads producing work and the threads consuming it. These tests exercise the
 * inter-thread hand-off: that a thread waiting for an element or for capacity
 * is always signalled once one becomes available, that the reported size never
 * drifts from the actual content, and that no element is lost or delivered
 * twice.
 * </p>
 *
 * <p>
 * Because the queue de-duplicates, an add can leave the queue unchanged; the
 * hand-off must still reach any other waiting thread, which
 * {@link #putDuplicate_handsCapacitySignalOn()} covers specifically.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class LinkedHashSetBlockingQueueConcurrencyTests {

	/** Bound on any latch/future wait, to fail rather than hang. */
	private static final long TIMEOUT_SECS = 20L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final List<Thread> threads = new ArrayList<>(8);

	private ExecutorService executor;

	@AfterEach
	public void teardown() throws Exception {
		for ( Thread t : threads ) {
			t.interrupt();
		}
		for ( Thread t : threads ) {
			t.join(SECONDS.toMillis(TIMEOUT_SECS));
		}
		if ( executor != null ) {
			executor.shutdownNow();
		}
	}

	/**
	 * Start a thread that blocks putting an element onto a queue.
	 *
	 * @param queue
	 *        the queue to put onto
	 * @param value
	 *        the value to put
	 * @param completed
	 *        counted down once the put returns normally
	 * @return the started thread, blocked within the queue
	 */
	private Thread startBlockedPutter(LinkedHashSetBlockingQueue<String> queue, String value,
			CountDownLatch completed) {
		final Thread t = new Thread(() -> {
			try {
				queue.put(value);
				completed.countDown();
			} catch ( InterruptedException e ) {
				log.debug("Putter of [{}] interrupted while still blocked.", value);
			}
		}, "put-" + value);
		threads.add(t);
		t.start();
		awaitBlockedInQueue(t);
		return t;
	}

	/**
	 * Wait for a thread to be blocked within the queue implementation.
	 *
	 * @param t
	 *        the thread to wait for
	 * @throws AssertionError
	 *         if the thread does not block within the timeout
	 */
	private static void awaitBlockedInQueue(Thread t) {
		final String queueClassName = LinkedHashSetBlockingQueue.class.getName();
		final long expire = System.nanoTime() + SECONDS.toNanos(TIMEOUT_SECS);
		while ( System.nanoTime() < expire ) {
			final Thread.State state = t.getState();
			if ( (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) && Arrays
					.stream(t.getStackTrace()).anyMatch(e -> queueClassName.equals(e.getClassName())) ) {
				return;
			}
			if ( state == Thread.State.TERMINATED ) {
				throw new AssertionError(
						"Thread %s finished without blocking in the queue.".formatted(t.getName()));
			}
			Thread.yield();
		}
		throw new AssertionError("Thread %s never blocked in the queue; state is %s."
				.formatted(t.getName(), t.getState()));
	}

	@Test
	public void take_blocksUntilElementOffered() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(2);
		final var taken = new ConcurrentLinkedQueue<String>();
		final var completed = new CountDownLatch(1);

		final Thread taker = new Thread(() -> {
			try {
				taken.add(queue.take());
				completed.countDown();
			} catch ( InterruptedException e ) {
				log.debug("Taker interrupted while still blocked.");
			}
		}, "take");
		threads.add(taker);
		taker.start();
		awaitBlockedInQueue(taker);

		// WHEN
		queue.offer("123");

		// THEN
		// @formatter:off
		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("Blocked take() was signalled by the offer()")
			.isTrue()
			;

		then(taken)
			.as("Offered element taken")
			.containsExactly("123")
			;

		then(queue.size())
			.as("Queue empty after the element was taken")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void put_blocksUntilCapacityFreed() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(1);
		queue.offer("123");
		final var completed = new CountDownLatch(1);
		startBlockedPutter(queue, "234", completed);

		// WHEN
		queue.poll();

		// THEN
		// @formatter:off
		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("Blocked put() was signalled by the poll()")
			.isTrue()
			;

		then(queue)
			.as("Blocked element queued once capacity was available")
			.containsExactly("234")
			;
		// @formatter:on
	}

	@Test
	public void putInterrupted_doesNotConsumeCapacitySignal() throws Exception {
		// GIVEN
		// a full queue with two threads blocked putting; interrupting one
		// must not consume the hand-off the other is waiting for
		final var queue = new LinkedHashSetBlockingQueue<String>(1);
		queue.offer("123");
		final var firstCompleted = new CountDownLatch(1);
		final var secondCompleted = new CountDownLatch(1);
		final Thread first = startBlockedPutter(queue, "234", firstCompleted);
		startBlockedPutter(queue, "345", secondCompleted);

		// WHEN
		first.interrupt();
		first.join(SECONDS.toMillis(TIMEOUT_SECS));
		queue.poll();

		// THEN
		// @formatter:off
		then(first.isAlive())
			.as("Interrupted put() abandoned its wait")
			.isFalse()
			;

		then(firstCompleted.getCount())
			.as("Interrupted put() did not queue its element")
			.isEqualTo(1L)
			;

		then(secondCompleted.await(TIMEOUT_SECS, SECONDS))
			.as("Freed slot still reached the remaining blocked put()")
			.isTrue()
			;

		then(queue)
			.as("Only the surviving thread's element queued")
			.containsExactly("345")
			;

		then(queue.size())
			.as("Reported size matches the content")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void clear_wakesBlockedPut() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(1);
		queue.offer("123");
		final var completed = new CountDownLatch(1);
		startBlockedPutter(queue, "234", completed);

		// WHEN
		queue.clear();

		// THEN
		// @formatter:off
		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("clear() frees capacity, so it must signal the blocked put()")
			.isTrue()
			;

		then(queue)
			.as("Blocked element queued once capacity was available")
			.containsExactly("234")
			;
		// @formatter:on
	}

	@Test
	public void remove_wakesBlockedPut() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(1);
		queue.offer("123");
		final var completed = new CountDownLatch(1);
		startBlockedPutter(queue, "234", completed);

		// WHEN
		queue.remove("123");

		// THEN
		// @formatter:off
		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("remove() frees capacity, so it must signal the blocked put()")
			.isTrue()
			;

		then(queue)
			.as("Blocked element queued once capacity was available")
			.containsExactly("234")
			;
		// @formatter:on
	}

	@Test
	public void removeIf_wakesBlockedPut() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(1);
		queue.offer("123");
		final var completed = new CountDownLatch(1);
		startBlockedPutter(queue, "234", completed);

		// WHEN
		final boolean removed = queue.removeIf(_ -> true);

		// THEN
		// @formatter:off
		then(removed)
			.as("Element removed through the iterator")
			.isTrue()
			;

		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("Iterator removal frees capacity, so it must signal the blocked put()")
			.isTrue()
			;

		then(queue)
			.as("Blocked element queued once capacity was available")
			.containsExactly("234")
			;
		// @formatter:on
	}

	@Test
	public void drainTo_wakesEveryBlockedPut() throws Exception {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<String>(2);
		queue.offer("123");
		queue.offer("234");
		final var completed = new CountDownLatch(2);
		startBlockedPutter(queue, "345", completed);
		startBlockedPutter(queue, "456", completed);

		// WHEN
		final List<String> dest = new ArrayList<>(2);
		final int drained = queue.drainTo(dest);

		// THEN
		// @formatter:off
		then(drained)
			.as("Both queued elements drained")
			.isEqualTo(2)
			;

		then(dest)
			.as("Drained in queue order")
			.containsExactly("123", "234")
			;

		then(completed.await(TIMEOUT_SECS, SECONDS))
			.as("drainTo() freed two slots, so both blocked put() calls must be signalled")
			.isTrue()
			;

		then(queue)
			.as("Both blocked elements queued once capacity was available")
			.containsExactlyInAnyOrder("345", "456")
			;
		// @formatter:on
	}

	@Test
	public void putDuplicate_handsCapacitySignalOn() throws Exception {
		// GIVEN
		// a full queue, with one thread blocked putting an element already
		// queued and a second blocked putting a new element; the duplicate
		// put consumes no capacity, so freeing one slot must satisfy both
		final var queue = new LinkedHashSetBlockingQueue<String>(3);
		queue.offer("123");
		queue.offer("234");
		queue.offer("345");

		final var duplicateCompleted = new CountDownLatch(1);
		final var newElementCompleted = new CountDownLatch(1);
		startBlockedPutter(queue, "345", duplicateCompleted);
		startBlockedPutter(queue, "456", newElementCompleted);

		// WHEN
		queue.remove("123");

		// THEN
		// @formatter:off
		then(duplicateCompleted.await(TIMEOUT_SECS, SECONDS))
			.as("Blocked put() of an already-queued element completed")
			.isTrue()
			;

		then(newElementCompleted.await(TIMEOUT_SECS, SECONDS))
			.as("Discarded duplicate consumed no capacity, so the freed slot must still reach "
					+ "the put() waiting with a new element")
			.isTrue()
			;

		then(queue)
			.as("New element queued, and the duplicate kept its original position")
			.containsExactly("234", "345", "456")
			;

		then(queue.size())
			.as("Reported size matches the content")
			.isEqualTo(3)
			;
		// @formatter:on
	}

	@Test
	public void inspection_duringConcurrentMutation_neverFails() throws Exception {
		// GIVEN
		final int inspectorCount = 4;
		final int mutatorCount = 4;
		final int inspectionCount = 20_000;
		final var queue = new LinkedHashSetBlockingQueue<Integer>(64);
		for ( int i = 0; i < 32; i++ ) {
			queue.offer(i);
		}

		final var stop = new AtomicBoolean(false);
		final var barrier = new CyclicBarrier(inspectorCount + mutatorCount);
		executor = Executors.newFixedThreadPool(inspectorCount + mutatorCount);

		// WHEN
		final List<Future<?>> futures = new ArrayList<>(inspectorCount + mutatorCount);
		for ( int i = 0; i < mutatorCount; i++ ) {
			final int offset = i * inspectionCount;
			futures.add(executor.submit(() -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				int val = offset;
				while ( !stop.get() ) {
					queue.offer(val++);
					queue.poll();
				}
				return null;
			}));
		}
		for ( int i = 0; i < inspectorCount; i++ ) {
			futures.add(executor.submit(() -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				try {
					for ( int j = 0; j < inspectionCount; j++ ) {
						// each of these traverses the queue, or must
						// avoid doing so
						queue.contains(-1);
						queue.peek();
						queue.iterator().forEachRemaining(_ -> {
							// consume, forcing the whole traversal
						});
						queue.toString();
					}
				} finally {
					stop.set(true);
				}
				return null;
			}));
		}

		// THEN
		// a ConcurrentModificationException from an inspection surfaces here
		for ( Future<?> f : futures ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}

		// @formatter:off
		then(queue.size())
			.as("Reported size still matches the content after concurrent inspection")
			.isEqualTo(queue.drainTo(new ArrayList<>()))
			;
		// @formatter:on
	}

	@Test
	public void putTake_noElementLostOrDeliveredTwice() throws Exception {
		// GIVEN
		// unique elements, so nothing is de-duplicated and every put must be
		// delivered exactly once; the capacity is far smaller than the
		// element count, so both sides block often
		final int producerCount = 4;
		final int consumerCount = 4;
		final int countPerProducer = 500;
		final int totalCount = producerCount * countPerProducer;
		final var queue = new LinkedHashSetBlockingQueue<Integer>(8);
		final var consumed = new ConcurrentLinkedQueue<Integer>();
		final var barrier = new CyclicBarrier(producerCount + consumerCount);
		executor = Executors.newFixedThreadPool(producerCount + consumerCount);

		// WHEN
		final List<Future<?>> futures = new ArrayList<>(producerCount + consumerCount);
		for ( int i = 0; i < producerCount; i++ ) {
			final int base = i * countPerProducer;
			futures.add(executor.submit((Callable<Void>) () -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				for ( int j = 0; j < countPerProducer; j++ ) {
					queue.put(base + j);
				}
				return null;
			}));
		}
		for ( int i = 0; i < consumerCount; i++ ) {
			futures.add(executor.submit((Callable<Void>) () -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				for ( int j = 0; j < totalCount / consumerCount; j++ ) {
					consumed.add(queue.take());
				}
				return null;
			}));
		}

		// a lost signal leaves a producer or consumer blocked, failing here
		for ( Future<?> f : futures ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}

		// THEN
		// @formatter:off
		then(consumed)
			.as("Every element put was taken exactly once")
			.hasSize(totalCount)
			.doesNotHaveDuplicates()
			.containsExactlyInAnyOrderElementsOf(IntStream.range(0, totalCount).boxed().toList())
			;

		then(queue.size())
			.as("Queue empty once every element was taken")
			.isZero()
			;

		then(queue.peek())
			.as("Nothing left queued")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void mixedMutation_reportedSizeMatchesContent() throws Exception {
		// GIVEN
		final int threadCount = 8;
		final int iterations = 5_000;
		final int capacity = 16;
		final var queue = new LinkedHashSetBlockingQueue<Integer>(capacity);
		final var barrier = new CyclicBarrier(threadCount);
		final var sizeViolations = new AtomicInteger();
		executor = Executors.newFixedThreadPool(threadCount);

		// WHEN
		// hammer every mutator with overlapping element values, so adds are
		// often de-duplicated
		final List<Future<?>> futures = new ArrayList<>(threadCount);
		for ( int i = 0; i < threadCount; i++ ) {
			futures.add(executor.submit((Callable<Void>) () -> {
				final RandomGenerator rng = RandomGenerator.getDefault();
				barrier.await(TIMEOUT_SECS, SECONDS);
				for ( int j = 0; j < iterations; j++ ) {
					final int val = rng.nextInt(capacity * 2);
					switch (j % 6) {
						case 0 -> queue.offer(val);
						case 1 -> queue.offer(val, 1, TimeUnit.MILLISECONDS);
						case 2 -> queue.poll();
						case 3 -> queue.remove(val);
						case 4 -> queue.drainTo(new ArrayList<>(), 4);
						default -> queue.removeIf(e -> e.equals(val));
					}
					final int size = queue.size();
					if ( size < 0 || size > capacity ) {
						sizeViolations.incrementAndGet();
					}
				}
				return null;
			}));
		}

		for ( Future<?> f : futures ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}

		// THEN
		final int reportedSize = queue.size();
		final List<Integer> remaining = new ArrayList<>(capacity);
		final int drained = queue.drainTo(remaining);

		// @formatter:off
		then(sizeViolations)
			.as("Reported size stayed within [0, capacity] throughout")
			.hasValue(0)
			;

		then(drained)
			.as("Reported size matched the actual content, so no element was double-counted "
					+ "or lost from the count")
			.isEqualTo(reportedSize)
			;

		then(remaining)
			.as("Drained content has no duplicates")
			.doesNotHaveDuplicates()
			;

		then(queue.size())
			.as("Queue empty after being drained")
			.isZero()
			;

		then(queue.poll())
			.as("Drained queue polls null rather than failing on an inconsistent count")
			.isNull()
			;

		then(queue.remainingCapacity())
			.as("Full capacity available again")
			.isEqualTo(capacity)
			;
		// @formatter:on
	}

}
