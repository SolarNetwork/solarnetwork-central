/* ==================================================================
 * SolarOcppDaoWarmUpTask.java - 3/07/2024 4:35:45 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.biz.AppWarmUpTask;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;

/**
 * Component to "warm up" the application DAO layer.
 * 
 * @author matt
 * @version 1.0
 */
@Component
@Profile(AppWarmUpTask.WARMUP)
public class SolarOcppDaoWarmUpTask implements AppWarmUpTask {

	private static final Logger log = LoggerFactory.getLogger(SolarOcppDaoWarmUpTask.class);

	private final CentralSystemUserDao systemUserDao;
	private final CentralChargePointDao chargePointDao;

	/**
	 * Constructor.
	 * 
	 * @param systemUserDao
	 *        the system user DAO
	 * @param chargePointDao
	 *        the charge point DAO
	 */
	public SolarOcppDaoWarmUpTask(CentralSystemUserDao systemUserDao,
			CentralChargePointDao chargePointDao) {
		super();
		this.systemUserDao = requireNonNullArgument(systemUserDao, "systemUserDao");
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
	}

	@Override
	public void warmUp() throws Exception {
		log.info("Performing DAO warm-up tasks...");

		try {
			log.debug("Querying for SystemUser...");
			systemUserDao.getForUsernameAndChargePoint(IDENT, IDENT);

			log.debug("Querying for ChargePoint...");
			chargePointDao.getForIdentity(new ChargePointIdentity(IDENT, 0L));
		} catch ( Exception e ) {
			log.error("App warm-up tasks threw exception: {}", e.getMessage(), e);
		}

		log.info("DAO warm-up tasks complete.");
	}

}
