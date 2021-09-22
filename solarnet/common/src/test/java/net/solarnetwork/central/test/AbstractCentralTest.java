/* ==================================================================
 * AbstractCentralTest.java - 18/05/2015 9:38:27 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import java.util.logging.Level;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Abstract base class for central non-transactional unit tests.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration(locations = { "classpath:/net/solarnetwork/central/test/test-context.xml" })
public abstract class AbstractCentralTest extends AbstractJUnit4SpringContextTests
		implements CentralTestConstants {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);
	}

}
