/* ==================================================================
 * AllTests.java - Nov 10, 2014 2:28:10 PM
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

package net.solarnetwork.central.dao.mybatis.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * All unit tests.
 * 
 * @author matt
 * @version 1.0
 */
@RunWith(Suite.class)
@SuiteClasses({ MyBatisNetworkAssociationDaoTests.class, MyBatisPriceLocationDaoTests.class,
		MyBatisPriceSourceDaoTests.class, MyBatisSolarLocationDaoTests.class,
		MyBatisSolarNodeDaoTests.class, MyBatisWeatherLocationDaoTests.class,
		MyBatisWeatherSourceDaoTests.class })
public class AllTests {

}
