/* ==================================================================
 * DaoSolarNodeMetadataBizTests.java - 11/11/2016 12:38:07 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link DaoSolarNodeMetadataBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoSolarNodeMetadataBizTests {

	private final Long TEST_NODE_ID = -11L;
	private SolarNodeMetadataDao solarNodeMetadataDao;
	private DaoSolarNodeMetadataBiz biz;

	private void replayAll() {
		replay(solarNodeMetadataDao);
	}

	private void verifyAll() {
		verify(solarNodeMetadataDao);
	}

	@BeforeEach
	public void setup() {
		solarNodeMetadataDao = EasyMock.createMock(SolarNodeMetadataDao.class);
		biz = new DaoSolarNodeMetadataBiz(solarNodeMetadataDao);
	}

	@Test
	public void addSolarNodeMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		Capture<SolarNodeMetadata> metaCap = new Capture<>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(null);
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_NODE_ID);

		replayAll();
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta);
		verifyAll();

		SolarNodeMetadata stored = metaCap.getValue();
		then(stored.getNodeId()).as("Node").isEqualTo(TEST_NODE_ID);
		then(stored.getMeta().hasTag("bam")).as("Tag craeted").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Info value").isEqualTo("bar");
	}

	@Test
	public void addSolarNodeMetadataNewWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		Capture<SolarNodeMetadata> metaCap = new Capture<>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(null);
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_NODE_ID);

		replayAll();
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta);
		verifyAll();

		SolarNodeMetadata stored = metaCap.getValue();
		then(stored.getNodeId()).as("Node").isEqualTo(TEST_NODE_ID);
		then(stored.getMeta().hasTag("bam")).as("Tag craeted").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Info value").isEqualTo("bar");
		then(stored.getMeta().getInfoString("watts", "unit")).as("Info prop avlue").isEqualTo("W");
	}

	@Test
	public void findSolarNodeMetadata() {
		FilterSupport criteria = new FilterSupport();
		criteria.setNodeId(TEST_NODE_ID);

		EasyMock.expect(solarNodeMetadataDao.findFiltered(criteria, null, null, null)).andReturn(null);

		replayAll();
		biz.findSolarNodeMetadata(criteria, null, null, null);
		verifyAll();
	}

	@Test
	public void addSolarNodeMetadataMerge() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		final Capture<SolarNodeMetadata> metaCap = new Capture<SolarNodeMetadata>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(null);
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_NODE_ID);

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta.addTag("mab");

		Capture<SolarNodeMetadata> meta2Cap = new Capture<SolarNodeMetadata>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID))
				.andAnswer(new IAnswer<SolarNodeMetadata>() {

					@Override
					public SolarNodeMetadata answer() throws Throwable {
						return metaCap.getValue();
					}
				});
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(meta2Cap))).andReturn(TEST_NODE_ID);

		replayAll();
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta);
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta2);
		verifyAll();

		SolarNodeMetadata stored = meta2Cap.getValue();
		then(stored.getNodeId()).as("Node").isEqualTo(TEST_NODE_ID);
		then(stored.getMeta().hasTag("bam")).as("Has original tag").isTrue();
		then(stored.getMeta().hasTag("mab")).as("Has new tag").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Replaced info value").isEqualTo("bam");
		then(stored.getMeta().getInfoString("oof")).as("New info value").isEqualTo("rab");
	}

	@Test
	public void addSolarNodeMetadataMergeWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		final Capture<SolarNodeMetadata> metaCap = new Capture<SolarNodeMetadata>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(null);
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(metaCap))).andReturn(TEST_NODE_ID);

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta2.putInfoValue("watts", "unitType", "SI");
		meta2.addTag("mab");

		Capture<SolarNodeMetadata> meta2Cap = new Capture<SolarNodeMetadata>();

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID))
				.andAnswer(new IAnswer<SolarNodeMetadata>() {

					@Override
					public SolarNodeMetadata answer() throws Throwable {
						return metaCap.getValue();
					}
				});
		EasyMock.expect(solarNodeMetadataDao.save(EasyMock.capture(meta2Cap))).andReturn(TEST_NODE_ID);

		replayAll();
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta);
		biz.addSolarNodeMetadata(TEST_NODE_ID, meta2);
		verifyAll();

		SolarNodeMetadata stored = metaCap.getValue();

		then(stored.getNodeId()).as("Node").isEqualTo(TEST_NODE_ID);
		then(stored.getMeta().hasTag("bam")).as("Has original tag").isTrue();
		then(stored.getMeta().hasTag("mab")).as("Has new tag").isTrue();
		then(stored.getMeta().getInfoString("foo")).as("Replaced info value").isEqualTo("bam");
		then(stored.getMeta().getInfoString("oof")).as("New info value").isEqualTo("rab");
		then(stored.getMeta().getInfoString("watts", "unit")).as("Replaced info property value")
				.isEqualTo("Wh");
		then(stored.getMeta().getInfoString("watts", "unitType")).as("New info property value")
				.isEqualTo("SI");
	}

	@Test
	public void removeNode() {
		SolarNodeMetadata gndm = new SolarNodeMetadata();
		gndm.setNodeId(TEST_NODE_ID);

		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(gndm);
		solarNodeMetadataDao.delete(gndm);

		replayAll();
		biz.removeSolarNodeMetadata(TEST_NODE_ID);
		verifyAll();
	}

	@Test
	public void removeNodeNonExisting() {
		EasyMock.expect(solarNodeMetadataDao.get(TEST_NODE_ID)).andReturn(null);

		replayAll();
		biz.removeSolarNodeMetadata(TEST_NODE_ID);
		verifyAll();
	}

}
