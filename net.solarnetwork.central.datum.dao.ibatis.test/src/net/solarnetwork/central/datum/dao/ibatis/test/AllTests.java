/* ==================================================================
 * AbstractIbatisDaoTestSupport.java - Jun 3, 2011 8:22:52 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ IbatisConsumptionDatumDaoTest.class, IbatisDayDatumDaoTest.class,
		IbatisGeneralLocationDatumDaoTest.class, IbatisGeneralLocationDatumMetadataDaoTest.class,
		IbatisGeneralNodeDatumDaoTest.class, IbatisGeneralNodeDatumMetadataDaoTest.class,
		IbatisHardwareControlDatumDaoTest.class, IbatisPowerDatumDaoTest.class,
		IbatisPriceDatumDaoTest.class, IbatisWeatherDatumDaoTest.class })
public class AllTests {

}
