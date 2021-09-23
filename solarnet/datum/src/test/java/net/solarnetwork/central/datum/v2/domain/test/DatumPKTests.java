/* ==================================================================
 * DatumPKTests.java - 22/10/2020 9:22:03 am
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

package net.solarnetwork.central.datum.v2.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.DatumPK;

/**
 * Test cases for the {@link DatumPK} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumPKTests {

	@Test
	public void stringValue() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk = new DatumPK(streamId, ts);
		assertThat("String value", pk.toString(), equalTo(
				"DatumPK{streamId=" + streamId.toString() + ", timestamp=" + ts.toString() + "}"));
	}

	@Test
	public void stringValue_nulls() {
		DatumPK pk = new DatumPK(null, null);
		assertThat("String value (null)", pk.toString(), equalTo("DatumPK{}"));
	}

	@Test
	public void equals_equal() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		DatumPK pk2 = new DatumPK(streamId, ts);
		assertThat("Keys equal", pk1, equalTo(pk2));
	}

	@Test
	public void equals_null() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		assertThat("Keys equal", pk1, not(equalTo(null)));
	}

	@Test
	public void equals_differentStreamIdSameTimestamp() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(UUID.randomUUID(), Instant
				.ofEpochSecond(pk1.getTimestamp().getEpochSecond(), pk1.getTimestamp().getNano()));
		assertThat("Keys not equal because stream IDs differ", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_otherStreamIdNull() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(null, pk1.getTimestamp());
		assertThat("Keys not equal because other stream ID null", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_thisStreamIdNull() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(null, pk1.getTimestamp());
		assertThat("Keys not equal because this stream ID null", pk2, not(equalTo(pk1)));
	}

	@Test
	public void equals_sameStreamIdDifferentTimestamp() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(
				new UUID(pk1.getStreamId().getMostSignificantBits(),
						pk1.getStreamId().getLeastSignificantBits()),
				pk1.getTimestamp().minusSeconds(1));
		assertThat("Keys not equal because timestamps differ", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_otherTimestampNull() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(pk1.getStreamId(), null);
		assertThat("Keys not equal because other stream ID null", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_thisTimestampNull() {
		DatumPK pk1 = new DatumPK(UUID.randomUUID(), Instant.now());
		DatumPK pk2 = new DatumPK(pk1.getStreamId(), null);
		assertThat("Keys not equal because other stream ID null", pk2, not(equalTo(pk1)));
	}

	@Test
	public void compareTo_equal() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		DatumPK pk2 = new DatumPK(streamId, ts);
		assertThat("Keys compare equally", pk1.compareTo(pk2), equalTo(0));
	}

	@Test
	public void compareTo_smallerStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(new UUID(0L, 0L), ts);
		DatumPK pk2 = new DatumPK(new UUID(1L, 0L), ts);
		assertThat("Key less from smaller stream ID", pk1.compareTo(pk2), equalTo(-1));
		assertThat("Reversed key less from smaller stream ID", pk2.compareTo(pk1), equalTo(1));
	}

	@Test
	public void compareTo_largerStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(new UUID(1L, 0L), ts);
		DatumPK pk2 = new DatumPK(new UUID(0L, 0L), ts);
		assertThat("Key less from smaller stream ID", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from smaller stream ID", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_nullStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(null, ts);
		DatumPK pk2 = new DatumPK(new UUID(1L, 0L), ts);
		assertThat("Key greater from null stream ID", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key greater from null stream ID", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_equalStreamSmallerTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		DatumPK pk2 = new DatumPK(streamId, ts.plusSeconds(1));
		assertThat("Key less from smaller timestamp", pk1.compareTo(pk2), equalTo(-1));
		assertThat("Reversed key less from smaller timestamp", pk2.compareTo(pk1), equalTo(1));
	}

	@Test
	public void compareTo_equalStreamLargerTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		DatumPK pk2 = new DatumPK(streamId, ts.minusSeconds(1));
		assertThat("Key less from smaller timestamp", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from smaller timestamp", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_equalStreamNullTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, null);
		DatumPK pk2 = new DatumPK(streamId, ts);
		assertThat("Key less from null timestamp", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from null timestamp", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_null() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumPK pk1 = new DatumPK(streamId, ts);
		assertThat("Key less from null object", pk1.compareTo(null), equalTo(-1));
	}

}
