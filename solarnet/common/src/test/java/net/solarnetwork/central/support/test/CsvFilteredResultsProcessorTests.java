/* ==================================================================
 * CsvFilteredResultsProcessorTests.java - 18/11/2022 2:27:49 pm
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

package net.solarnetwork.central.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestStatus;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.support.CsvFilteredResultsProcessor;

/**
 * Test cases for the {@link CsvFilteredResultsProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvFilteredResultsProcessorTests {

	@Test
	public void javaBean_simple() throws IOException {
		// GIVEN
		StringWriter out = new StringWriter();

		SolarNode n1 = new SolarNode(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits());
		n1.setCreated(Instant.now());
		n1.setName("Test Node");

		SolarNode n2 = new SolarNode(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits());
		n2.setCreated(Instant.now().plusSeconds(1));
		n2.setName("Test Node 2");

		// WHEN
		try (CsvFilteredResultsProcessor<SolarNode> processor = new CsvFilteredResultsProcessor<>(out)) {
			processor.start(null, null, null, null);
			processor.handleResultItem(n1);
			processor.handleResultItem(n2);
		}

		// THEN
		assertThat("CSV generated; location property ignored", out.toString().replace("\r", ""),
				is(equalTo("""
						created,id,locationId,name
						%s,%d,%d,%s
						%s,%d,%d,%s
						""".formatted(n1.getCreated(), n1.getId(), n1.getLocationId(), n1.getName(),
						n2.getCreated(), n2.getId(), n2.getLocationId(), n2.getName()))));
	}

	@Test
	public void javaBean_jsonPropertyOrder() throws IOException {
		// GIVEN
		StringWriter out = new StringWriter();

		LocationRequest req1 = new LocationRequest(UUID.randomUUID().getMostSignificantBits(),
				Instant.now());
		req1.setLocationId(UUID.randomUUID().getMostSignificantBits());
		req1.setMessage("Howdy ho!");
		req1.setStatus(LocationRequestStatus.Submitted);
		req1.setUserId(UUID.randomUUID().getMostSignificantBits());

		LocationRequest req2 = new LocationRequest(UUID.randomUUID().getMostSignificantBits(),
				Instant.now().plusSeconds(1));
		req2.setLocationId(UUID.randomUUID().getMostSignificantBits());
		req2.setMessage("How \"about\" this?");
		req2.setStatus(LocationRequestStatus.Rejected);
		req2.setUserId(UUID.randomUUID().getMostSignificantBits());

		// WHEN
		try (CsvFilteredResultsProcessor<LocationRequest> processor = new CsvFilteredResultsProcessor<>(
				out)) {
			processor.start(null, null, null, null);
			processor.handleResultItem(req1);
			processor.handleResultItem(req2);
		}

		// THEN
		assertThat("CSV generated; property order from @JsonPropertyOrder",
				out.toString().replace("\r", ""),
				is(equalTo("""
						id,created,userId,status,locationId,message
						%d,%s,%d,%s,%d,Howdy ho!
						%d,%s,%d,%s,%d,"How ""about"" this?"
						""".formatted(req1.getId(), req1.getCreated(), req1.getUserId(),
						req1.getStatus(), req1.getLocationId(), req2.getId(), req2.getCreated(),
						req2.getUserId(), req2.getStatus(), req2.getLocationId()))));
	}

}
