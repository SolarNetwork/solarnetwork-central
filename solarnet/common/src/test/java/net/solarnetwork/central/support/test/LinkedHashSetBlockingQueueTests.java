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
import static org.assertj.core.api.BDDAssertions.thenObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
 * @author matt
 * @version 1.0
 */
public class LinkedHashSetBlockingQueueTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
		// @formatteR:on
	}

}
