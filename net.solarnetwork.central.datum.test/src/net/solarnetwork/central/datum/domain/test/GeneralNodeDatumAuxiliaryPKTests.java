/* ==================================================================
 * GeneralNodeDatumAuxiliaryPKTests.java - 1/02/2019 4:54:16 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;

/**
 * Test cases for the {@link GeneralNodeDatumPK} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumAuxiliaryPKTests {

	private static final DateTime TEST_DATE = new DateTime(2019, 2, 1, 17, 0, 0);
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	@Test
	public void stringValue() {
		GeneralNodeDatumAuxiliaryPK pk = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		assertThat("String value", pk.toString(),
				equalTo("GeneralNodeDatumAuxiliaryPK{nodeId=" + TEST_NODE_ID + ", sourceId="
						+ TEST_SOURCE_ID + ", created=" + TEST_DATE + ", type="
						+ DatumAuxiliaryType.Reset + "}"));
	}

	@Test
	public void idValue() {
		GeneralNodeDatumAuxiliaryPK pk = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		assertThat("ID value", pk.getId(), equalTo(DigestUtils.sha1Hex("n=" + TEST_NODE_ID + ";s="
				+ TEST_SOURCE_ID + ";c=" + TEST_DATE + ";t=" + DatumAuxiliaryType.Reset)));
	}

	@Test
	public void equalsEqual() {
		GeneralNodeDatumAuxiliaryPK pk1 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		GeneralNodeDatumAuxiliaryPK pk2 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
				new DateTime(TEST_DATE), TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		assertThat("Keys equal", pk1, equalTo(pk2));
	}

	@Test
	public void equalsDifferent() {
		GeneralNodeDatumAuxiliaryPK pk1 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		GeneralNodeDatumAuxiliaryPK pk2 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
				new DateTime(TEST_DATE), TEST_SOURCE_ID, null);
		assertThat("Keys not equal", pk1, not(equalTo(pk2)));
	}

	@Test
	public void compareTypesDescendingNull() {
		GeneralNodeDatumAuxiliaryPK pk1 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		GeneralNodeDatumAuxiliaryPK pk2 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
				new DateTime(TEST_DATE), TEST_SOURCE_ID, null);
		assertThat("Comparison", pk1.compareTo(pk2), greaterThanOrEqualTo(1));
	}

	@Test
	public void compareTypesAscendingNull() {
		GeneralNodeDatumAuxiliaryPK pk1 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, null);
		GeneralNodeDatumAuxiliaryPK pk2 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
				new DateTime(TEST_DATE), TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		assertThat("Comparison", pk1.compareTo(pk2), lessThanOrEqualTo(-1));
	}

	@Test
	public void compareKindsEqual() {
		GeneralNodeDatumAuxiliaryPK pk1 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, TEST_DATE,
				TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		GeneralNodeDatumAuxiliaryPK pk2 = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
				new DateTime(TEST_DATE), TEST_SOURCE_ID, DatumAuxiliaryType.Reset);
		assertThat("Comparison", pk1.compareTo(pk2), equalTo(0));
	}
}
