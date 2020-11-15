/* ==================================================================
 * MyBatisAuditDatumEntityDaoTests.java - 15/11/2020 12:45:36 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.mybatis.MyBatisAuditDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisAuditDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisAuditDatumEntityDaoTests extends AbstractMyBatisDaoTestSupport {

	protected MyBatisAuditDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new MyBatisAuditDatumEntityDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	@Test
	public void findAuditDatum_forUser_hour_noData() {
		// GIVEN

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatum, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("No data available", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findAuditDatum_acc_forUser_noData() {
		// GIVEN

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatum, DatumPK> results = dao.findAccumulativeAuditDatumFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("No data available", results.getReturnedResultCount(), equalTo(0));
	}
}
