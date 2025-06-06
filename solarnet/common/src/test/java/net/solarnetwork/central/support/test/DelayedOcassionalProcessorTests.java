/* ==================================================================
 * DelayedOcassionalProcessorTests.java - 3/07/2024 1:38:18 pm
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.support.DelayQueueSet;
import net.solarnetwork.central.support.DelayedOccasionalProcessor;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link DelayedOccasionalProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DelayedOcassionalProcessorTests {

	private final static Logger log = LoggerFactory.getLogger(DelayedOccasionalProcessor.class);

	@Mock
	private TaskScheduler scheduler;

	@SuppressWarnings("rawtypes")
	@Mock
	private ScheduledFuture future;

	private MutableClock clock;
	private StatTracker stats;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
		stats = new StatTracker("Test", null, log, 10);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void addItems() {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<Integer>(8);
		final var processed = new ArrayList<>(8);
		final var processor = new DelayedOccasionalProcessor<Integer>(clock, stats, scheduler, queue) {

			@Override
			protected void processItemInternal(Integer item) {
				processed.add(item);
			}

		};

		given(scheduler.schedule(same(processor), any(Instant.class))).willReturn(future);
		given(future.isDone()).willReturn(false, false);

		// WHEN
		for ( int i = 1; i <= 3; i++ ) {
			processor.asyncProcessItem(i);
		}

		// THEN
		then(scheduler).should().schedule(same(processor),
				eq(clock.instant().plus(processor.getDelay())));
		then(future).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(stats.allCounts())
			.as("Add count incremented for each call")
			.containsEntry(DelayedOccasionalProcessor.Stats.ItemsAdded.name(), 3L)
			.as("No other counts created")
			.hasSize(1)
			;
		and.then(queue)
			.as("Queue contains added items")
			.containsExactly(1, 2, 3)
			;
		and.then(processed)
			.as("Nothing processed yet")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void processItems_all() {
		// GIVEN
		final var queue = new LinkedHashSetBlockingQueue<Integer>(8);
		queue.add(1);
		queue.add(2);
		queue.add(3);

		final var processed = new ArrayList<>(8);
		final var processor = new DelayedOccasionalProcessor<Integer>(clock, stats, scheduler, queue) {

			@Override
			protected void processItemInternal(Integer item) {
				processed.add(item);
			}

		};

		// WHEN
		// jump ahead in time
		clock.add(processor.getDelay());
		processor.run();

		// THEN
		then(future).shouldHaveNoInteractions();
		then(scheduler).shouldHaveNoInteractions();

		// @formatter:off
		and.then(stats.allCounts())
			.as("Batch count incremented")
			.containsEntry(DelayedOccasionalProcessor.Stats.Batches.name(), 1L)
			.as("Remove count incremted for each item processed")
			.containsEntry(DelayedOccasionalProcessor.Stats.ItemsRemoved.name(), 3L)
			.as("Process count incremted for each item processed")
			.containsEntry(DelayedOccasionalProcessor.Stats.ItemsProcessed.name(), 3L)
			.as("No other counts created")
			.hasSize(3)
			;
		and.then(queue)
			.as("Queue has been emptied")
			.isEmpty()
			;
		and.then(processed)
			.as("All items processed")
			.containsExactly(1, 2, 3)
			;
		// @formatter:on
	}

	public static record DelayedInteger(Clock clock, Instant ready, int i) implements Delayed {

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
		public int compareTo(Delayed o) {
			return Integer.compare(i, ((DelayedInteger) o).i);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return clock.instant().until(ready, unit.toChronoUnit());
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void processItems_some() {
		// GIVEN
		final var i1 = new DelayedInteger(clock, clock.instant().plus(Duration.ofSeconds(1)), 1);
		final var i2 = new DelayedInteger(clock, clock.instant().plus(Duration.ofSeconds(2)), 2);
		final var i3 = new DelayedInteger(clock, clock.instant().plus(Duration.ofSeconds(3)), 3);
		final var queue = new DelayQueueSet<DelayedInteger>();
		queue.addAll(Arrays.asList(i1, i2, i3));

		final var processed = new ArrayList<DelayedInteger>(8);
		final var processor = new DelayedOccasionalProcessor<DelayedInteger>(clock, stats, scheduler,
				queue) {

			@Override
			protected void processItemInternal(DelayedInteger item) {
				processed.add(item);
			}

		};

		given(scheduler.schedule(same(processor), any(Instant.class))).willReturn(future);

		// WHEN
		// jump ahead in time
		clock.add(Duration.ofSeconds(2));
		processor.run();

		// THEN
		// after processing, should re-schedule another run because the queue was not emptied
		then(scheduler).should().schedule(same(processor),
				eq(clock.instant().plus(processor.getDelay())));
		then(future).shouldHaveNoInteractions();

		// @formatter:off
		and.then(stats.allCounts())
			.as("Batch count incremented")
			.containsEntry(DelayedOccasionalProcessor.Stats.Batches.name(), 1L)
			.as("Remove count incremted for each item processed")
			.containsEntry(DelayedOccasionalProcessor.Stats.ItemsRemoved.name(), 2L)
			.as("Process count incremted for each item processed")
			.containsEntry(DelayedOccasionalProcessor.Stats.ItemsProcessed.name(), 2L)
			.as("No other counts created")
			.hasSize(3)
			;
		and.then(queue)
			.as("Queue has not been emptied because item 3 delayed still")
			.containsExactly(i3)
			;
		and.then(processed)
			.as("Items 1, 2 processed because they are ready")
			.containsExactly(i1, i2)
			;
		// @formatter:on
	}

}
