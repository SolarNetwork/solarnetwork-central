/* ==================================================================
 * IbatisGeneralLocationDatumMetadataDaoTest.java - Oct 17, 2014 3:34:15 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.dao.ibatis.IbatisGeneralLocationDatumMetadataDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test cases for the {@link IbatisGeneralLocationDatumMetadataDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisGeneralLocationDatumMetadataDaoTest extends AbstractIbatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID_2 = "test.source.2";

	@Autowired
	private IbatisGeneralLocationDatumMetadataDao dao;

	private GeneralLocationDatumMetadata lastDatum;

	private GeneralLocationDatumMetadata getTestInstance() {
		GeneralLocationDatumMetadata datum = new GeneralLocationDatumMetadata();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		samples.addTag("foo");

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralLocationDatumMetadata datum = getTestInstance();
		LocationSourcePK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(GeneralLocationDatumMetadata src, GeneralLocationDatumMetadata entity) {
		assertNotNull("GeneralLocationDatum should exist", entity);
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getCreated(), entity.getCreated());
		assertEquals(src.getMeta(), entity.getMeta());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralLocationDatumMetadata datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		GeneralLocationDatumMetadata datum = getTestInstance();
		datum.getMeta().getInfo().put("watt_hours", 39309570293789380L);
		datum.getMeta().getInfo().put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getMeta().getInfo().put("watts", 498475890235787897L);
		datum.getMeta().getInfo()
				.put("floating", new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralLocationDatumMetadata entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria,
				null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralLocationDatumMetadata datum2 = new GeneralLocationDatumMetadata();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID_2);
		datum2.setMetaJson("{\"m\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		GeneralLocationDatumMetadata datum3 = new GeneralLocationDatumMetadata();
		datum3.setCreated(lastDatum.getCreated());
		datum3.setLocationId(TEST_LOC_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setMetaJson("{\"m\":{\"watt_hours\":789}}");
		dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<LocationSourcePK> ids = new ArrayList<LocationSourcePK>();
		for ( GeneralLocationDatumMetadataFilterMatch d : results ) {
			ids.add(d.getId());
		}
		// expect d3, d1, d2 because sorted by locationId,created,sourceId
		assertEquals("Result order", Arrays.asList(datum3.getId(), lastDatum.getId(), datum2.getId()),
				ids);
	}

	@Test
	public void findFilteredWithMax() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria,
				null, 0, 1);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralLocationDatumMetadata datum2 = new GeneralLocationDatumMetadata();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID_2);
		datum2.setMetaJson("{\"m\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, 0, 1);
		assertNotNull(results);
		assertEquals("Returned results", 2L, (long) results.getTotalResults());
		assertEquals("Returned result count", 1, (int) results.getReturnedResultCount());
		assertEquals("Datum ID", lastDatum.getId(), results.iterator().next().getId());
	}

	@Test
	public void findFilteredWithLocationSearch() {
		storeNew();

		SolarLocation loc = new SolarLocation();
		loc.setRegion("NZ");
		DatumFilterCommand criteria = new DatumFilterCommand(loc);

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria,
				null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
	}

	@Test
	public void findFilteredWithTagSearch() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setTags(new String[] { "foo", "bar" });

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria,
				null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
	}

}
