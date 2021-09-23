/* ==================================================================
 * DatumTests.java - 22/10/2020 1:42:45 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumPK;

/**
 * Test cases for the {@link DatumEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumEntityTests {

	@Test
	public void hasId_true() {
		DatumEntity d = new DatumEntity(UUID.randomUUID(), Instant.now(), null, null);
		assertThat("Datum with stream ID and timestamp has an ID", d.hasId(), equalTo(true));
	}

	@Test
	public void hasId_noStreamId() {
		DatumEntity d = new DatumEntity(null, Instant.now(), null, null);
		assertThat("Datum without stream ID does not have an ID", d.hasId(), equalTo(false));
	}

	@Test
	public void hasId_noTimestamp() {
		DatumEntity d = new DatumEntity(UUID.randomUUID(), null, null, null);
		assertThat("Datum without timestamp does not have an ID", d.hasId(), equalTo(false));
	}

	@Test
	public void createdIsAliasForTimestamp() {
		final Instant now = Instant.now();
		DatumEntity d = new DatumEntity(UUID.randomUUID(), now, null, null);
		assertThat("getTimestamp() returns constructor instance", d.getTimestamp(), sameInstance(now));
		assertThat("getCreated() is alias for getTimestamp()", d.getCreated(), sameInstance(now));
	}

	@Test
	public void idCreatedFromConstructorArgumnets() {
		final UUID streamId = UUID.randomUUID();
		final Instant timestamp = Instant.now();
		DatumEntity d = new DatumEntity(streamId, timestamp, null, null);
		assertThat("getId() returns PK with constructor values", d.getId(),
				equalTo(new DatumPK(streamId, timestamp)));
	}

	@Test
	public void streamIdIsAliasForIdTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant timestamp = Instant.now();
		DatumEntity d = new DatumEntity(streamId, timestamp, null, null);
		assertThat("getStreamId() returns ID streamId instance", d.getStreamId(),
				sameInstance(d.getId().getStreamId()));
	}

	@Test
	public void timestampIsAliasForIdTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant timestamp = Instant.now();
		DatumEntity d = new DatumEntity(streamId, timestamp, null, null);
		assertThat("getTimestamp() returns ID timestamp instance", d.getTimestamp(),
				sameInstance(d.getId().getTimestamp()));
	}

}
