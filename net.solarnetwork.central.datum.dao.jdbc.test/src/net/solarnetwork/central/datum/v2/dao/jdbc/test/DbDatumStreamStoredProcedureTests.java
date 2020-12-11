/* ==================================================================
 * DbDatumStreamStoredProcedureTests.java - 6/11/2020 3:22:25 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.streamMetadata;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for datum steam database stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumStreamStoredProcedureTests extends BaseDatumJdbcTestSupport {

	@Test
	public void findStreamMetadata_none() {
		ObjectDatumStreamMetadata result = streamMetadata(jdbcTemplate, UUID.randomUUID());
		assertThat("Metadata is null when does not exist in DB", result, nullValue());
	}

	@Test
	public void findStreamMetadata_node() {
		// GIVEN
		ObjectDatumStreamMetadata m = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "b", "c", "d" }, new String[] { "e", "f" },
				new String[] { "g" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(m));

		// WHEN
		ObjectDatumStreamMetadata result = streamMetadata(jdbcTemplate, m.getStreamId());

		// THEN
		assertThat("Node metadata found", result, notNullValue());
		assertThat("Node metadata instance returned", result.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Stream ID matches", result.getStreamId(), equalTo(m.getStreamId()));
		assertThat("Time zone matches", result.getTimeZoneId(), equalTo(m.getTimeZoneId()));

		assertThat("Node ID matches", result.getObjectId(), equalTo(m.getObjectId()));
		assertThat("Source ID matches", result.getSourceId(), equalTo(m.getSourceId()));
		assertThat("Instantaneous names match",
				result.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous)));
		assertThat("Accumulating names match",
				result.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Accumulating)));
		assertThat("Status names match", result.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Status)));
	}

	@Test
	public void findStreamMetadata_location() {
		// GIVEN
		ObjectDatumStreamMetadata m = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Location, 1L, "a", new String[] { "b", "c", "d" },
				new String[] { "e", "f" }, new String[] { "g" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(m));

		// WHEN
		ObjectDatumStreamMetadata result = streamMetadata(jdbcTemplate, m.getStreamId());

		// THEN
		assertThat("Location metadata found", result, notNullValue());
		assertThat("Location metadata instance returned", result.getKind(),
				equalTo(ObjectDatumKind.Location));
		assertThat("Stream ID matches", result.getStreamId(), equalTo(m.getStreamId()));
		assertThat("Time zone matches", result.getTimeZoneId(), equalTo(m.getTimeZoneId()));

		assertThat("Object ID matches", result.getObjectId(), equalTo(m.getObjectId()));
		assertThat("Source ID matches", result.getSourceId(), equalTo(m.getSourceId()));
		assertThat("Instantaneous names match",
				result.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous)));
		assertThat("Accumulating names match",
				result.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Accumulating)));
		assertThat("Status names match", result.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Status)));
	}

	@Test
	public void findStreamMetadata_nodeOverLocation() {
		// GIVEN
		ObjectDatumStreamMetadata m = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "b", "c", "d" }, new String[] { "e", "f" },
				new String[] { "g" });
		ObjectDatumStreamMetadata m2 = new BasicObjectDatumStreamMetadata(m.getStreamId(), "UTC",
				ObjectDatumKind.Location, 2L, "h", new String[] { "i", "j", "k" },
				new String[] { "l", "m" }, new String[] { "n" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, Arrays.asList(m, m2));

		// WHEN
		ObjectDatumStreamMetadata result = streamMetadata(jdbcTemplate, m.getStreamId());

		// THEN
		assertThat("Node metadata found", result, notNullValue());
		assertThat("Node metadata instance returned, when same stream ID exists for location",
				result.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Stream ID matches", result.getStreamId(), equalTo(m.getStreamId()));
		assertThat("Time zone matches", result.getTimeZoneId(), equalTo(m.getTimeZoneId()));

		ObjectDatumStreamMetadata nodeMeta = result;
		assertThat("Node ID matches", nodeMeta.getObjectId(), equalTo(m.getObjectId()));
		assertThat("Source ID matches", nodeMeta.getSourceId(), equalTo(m.getSourceId()));
		assertThat("Instantaneous names match",
				nodeMeta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous)));
		assertThat("Accumulating names match",
				nodeMeta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Accumulating)));
		assertThat("Status names match", nodeMeta.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining(m.propertyNamesForType(GeneralDatumSamplesType.Status)));
	}
}
