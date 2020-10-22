/* ==================================================================
 * DatumStreamPKTests.java - 22/10/2020 9:22:03 am
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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumStreamPK;

/**
 * Test cases for the {@link DatumStreamPK} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumStreamPKTests {

	@Test
	public void stringValue() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk = new DatumStreamPK(streamId, ts);
		assertThat("String value", pk.toString(), equalTo(
				"DatumStreamPK{streamId=" + streamId.toString() + ", timestamp=" + ts.toString() + "}"));
	}

	@Test
	public void stringValue_nulls() {
		DatumStreamPK pk = new DatumStreamPK(null, null);
		assertThat("String value (null)", pk.toString(), equalTo("DatumStreamPK{}"));
	}

	@Test
	public void equals_equal() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		DatumStreamPK pk2 = new DatumStreamPK(streamId, ts);
		assertThat("Keys equal", pk1, equalTo(pk2));
	}

	@Test
	public void equals_null() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		assertThat("Keys equal", pk1, not(equalTo(null)));
	}

	@Test
	public void equals_differentStreamIdSameTimestamp() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(UUID.randomUUID(), Instant
				.ofEpochSecond(pk1.getTimestamp().getEpochSecond(), pk1.getTimestamp().getNano()));
		assertThat("Keys not equal because stream IDs differ", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_otherStreamIdNull() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(null, pk1.getTimestamp());
		assertThat("Keys not equal because other stream ID null", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_thisStreamIdNull() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(null, pk1.getTimestamp());
		assertThat("Keys not equal because this stream ID null", pk2, not(equalTo(pk1)));
	}

	@Test
	public void equals_sameStreamIdDifferentTimestamp() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(
				new UUID(pk1.getStreamId().getMostSignificantBits(),
						pk1.getStreamId().getLeastSignificantBits()),
				pk1.getTimestamp().minusSeconds(1));
		assertThat("Keys not equal because timestamps differ", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_otherTimestampNull() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(pk1.getStreamId(), null);
		assertThat("Keys not equal because other stream ID null", pk1, not(equalTo(pk2)));
	}

	@Test
	public void equals_thisTimestampNull() {
		DatumStreamPK pk1 = new DatumStreamPK(UUID.randomUUID(), Instant.now());
		DatumStreamPK pk2 = new DatumStreamPK(pk1.getStreamId(), null);
		assertThat("Keys not equal because other stream ID null", pk2, not(equalTo(pk1)));
	}

	@Test
	public void compareTo_equal() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		DatumStreamPK pk2 = new DatumStreamPK(streamId, ts);
		assertThat("Keys compare equally", pk1.compareTo(pk2), equalTo(0));
	}

	@Test
	public void compareTo_smallerStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(new UUID(0L, 0L), ts);
		DatumStreamPK pk2 = new DatumStreamPK(new UUID(1L, 0L), ts);
		assertThat("Key less from smaller stream ID", pk1.compareTo(pk2), equalTo(-1));
		assertThat("Reversed key less from smaller stream ID", pk2.compareTo(pk1), equalTo(1));
	}

	@Test
	public void compareTo_largerStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(new UUID(1L, 0L), ts);
		DatumStreamPK pk2 = new DatumStreamPK(new UUID(0L, 0L), ts);
		assertThat("Key less from smaller stream ID", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from smaller stream ID", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_nullStreamEqualTimestamp() {
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(null, ts);
		DatumStreamPK pk2 = new DatumStreamPK(new UUID(1L, 0L), ts);
		assertThat("Key greater from null stream ID", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key greater from null stream ID", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_equalStreamSmallerTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		DatumStreamPK pk2 = new DatumStreamPK(streamId, ts.plusSeconds(1));
		assertThat("Key less from smaller timestamp", pk1.compareTo(pk2), equalTo(-1));
		assertThat("Reversed key less from smaller timestamp", pk2.compareTo(pk1), equalTo(1));
	}

	@Test
	public void compareTo_equalStreamLargerTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		DatumStreamPK pk2 = new DatumStreamPK(streamId, ts.minusSeconds(1));
		assertThat("Key less from smaller timestamp", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from smaller timestamp", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_equalStreamNullTimestamp() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, null);
		DatumStreamPK pk2 = new DatumStreamPK(streamId, ts);
		assertThat("Key less from null timestamp", pk1.compareTo(pk2), equalTo(1));
		assertThat("Reversed key less from null timestamp", pk2.compareTo(pk1), equalTo(-1));
	}

	@Test
	public void compareTo_null() {
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now();
		DatumStreamPK pk1 = new DatumStreamPK(streamId, ts);
		assertThat("Key less from null object", pk1.compareTo(null), equalTo(-1));
	}

}
