/* ==================================================================
 * CloudDatumStreamPollTaskResetAbandoned.java - 13/10/2024 2:43:38 pm
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

package net.solarnetwork.central.c2c.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to process cloud datum stream poll tasks whose state is executing but
 * their execute date is really old.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamPollTaskResetAbandoned extends JobSupport {

	/** The {@code minimumAge} property default value (1 hour). */
	public static final Duration DEFAULT_MINIMUM_AGE = Duration.ofHours(1);

	private final Clock clock;
	private final CloudDatumStreamPollService service;
	private Duration minimumAge = DEFAULT_MINIMUM_AGE;

	/**
	 * Constructor.
	 *
	 * @param service
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamPollTaskResetAbandoned(CloudDatumStreamPollService service) {
		this(Clock.systemUTC(), service);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param service
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamPollTaskResetAbandoned(Clock clock, CloudDatumStreamPollService service) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.service = requireNonNullArgument(service, "service");
		setGroupId("CloudIntegrations");
		setId("CloudDatumStreamPollTaskResetAbandoned");
	}

	@Override
	public void run() {
		Instant date = clock.instant().truncatedTo(ChronoUnit.MINUTES).minus(minimumAge);
		int count = service.resetAbandondedExecutingTasks(date);
		if ( count > 0 ) {
			log.warn("Reset {} abandoned datum stream poll tasks", count);
		}
	}

	/**
	 * Get the minimum age before tasks are eligible for being reset.
	 *
	 * @return the minimum age, never {@literal null}; defaults to
	 *         {@link #DEFAULT_MINIMUM_AGE}
	 */
	public final Duration getMinimumAge() {
		return minimumAge;
	}

	/**
	 * Set the minimum age before tasks are eligible for being reset.
	 *
	 * @param minimumAge
	 *        the minimum age to set; if {@literal null} then set as
	 *        {@link #DEFAULT_MINIMUM_AGE}
	 */
	public final void setMinimumAge(Duration minimumAge) {
		this.minimumAge = (minimumAge != null ? minimumAge : DEFAULT_MINIMUM_AGE);
	}

}
