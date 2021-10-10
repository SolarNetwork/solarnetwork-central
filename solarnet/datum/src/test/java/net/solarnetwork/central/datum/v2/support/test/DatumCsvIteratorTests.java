/* ==================================================================
 * DatumCsvIteratorTests.java - 11/10/2021 9:51:51 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.support.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.ByteUtils.UTF8;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.then;
import static org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.supercsv.io.CsvListReader;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumCsvIterator;
import net.solarnetwork.util.CloseableIterator;

/**
 * Test cases for the {@link DatumCsvIterator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumCsvIteratorTests {

	@Test
	public void customDateFormat() throws IOException {
		// GIVEN
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ssX");
		final Instant startDate = formatter.parse("2019-07-25 05:17:00+12", Instant::from);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "watts" }, new String[] { "watthours" },
				null);

		// WHEN
		List<Datum> datum = new ArrayList<>();
		try (CloseableIterator<Datum> itr = new DatumCsvIterator(new CsvListReader(
				new InputStreamReader(getClass().getResourceAsStream("raw-data-01.csv"), UTF8),
				STANDARD_PREFERENCE), staticProvider(singleton(meta)), formatter)) {
			while ( itr.hasNext() ) {
				Datum d = itr.next();
				datum.add(d);
			}
		}

		// THEN
		then(datum).as("CSV rows processed into Datum").hasSize(43).allSatisfy(d -> {
			then(d.getStreamId()).isEqualTo(meta.getStreamId());
			then(d.getTimestamp()).isNotNull();
			then(d.getProperties().getInstantaneousLength()).isEqualTo(1);
			then(d.getProperties().getAccumulatingLength()).isEqualTo(1);
			then(d.getProperties().getStatusLength()).isEqualTo(0);
		});
		// 2019-07-25 05:43:00+12,1,A,413700,138000
		int row = 26;
		then(datum).element(row).describedAs("row %d ID", row).extracting(Datum::getId)
				.isEqualTo(new DatumPK(meta.getStreamId(), startDate.plus(row, ChronoUnit.MINUTES)));
		then(datum).element(row).describedAs("row %d properties", row).extracting(Datum::getProperties)
				.isEqualTo(propertiesOf(decimalArray("413700"), decimalArray("138000"), null, null));
	}

}
