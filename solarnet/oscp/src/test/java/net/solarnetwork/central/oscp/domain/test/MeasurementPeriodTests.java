/* ==================================================================
 * MeasurementPeriodTests.java - 8/09/2022 3:30:38 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;

/**
 * Test cases for the {@link MeasurementPeriod} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementPeriodTests {

	private static final Logger log = LoggerFactory.getLogger(MeasurementPeriodTests.class);

	@Test
	public void startingPeriod() {
		for ( MeasurementPeriod p : MeasurementPeriod.values() ) {
			final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
			final Instant end = start.plus(1, ChronoUnit.HOURS);
			Instant now = start;
			Instant expected = start;
			Instant next = start.plusSeconds(p.getCode());
			while ( now.isBefore(end) ) {
				Instant s = p.periodStart(now);
				log.info("{} period start {} -> {}", p, now, s);
				assertThat("%s period start %s".formatted(p, now), s, is(equalTo(expected)));
				assertThat("%s period start %s".formatted(p, now), p.previousPeriodStart(now),
						is(equalTo(expected.minusSeconds(p.getCode()))));
				now = now.plus(1, ChronoUnit.MINUTES);
				if ( !now.isBefore(next) ) {
					expected = now;
					next = now.plusSeconds(p.getCode());
				}
			}
		}
	}

}
