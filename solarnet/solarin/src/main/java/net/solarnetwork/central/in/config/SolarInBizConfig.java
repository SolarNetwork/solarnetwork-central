/* ==================================================================
 * SolarInBizConfig.java - 4/10/2021 4:50:12 PM
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

package net.solarnetwork.central.in.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.in.biz.dao.SimpleNetworkIdentityBiz;

/**
 * Business service configuration for the SolarIn application.
 * 
 * @author matt
 * @version 2.0
 */
@Profile("!sqs-datum-collector")
@Configuration(proxyBeanMethods = false)
public class SolarInBizConfig implements SolarInConfiguration {

	@Autowired
	private DatumStreamMetadataDao metaDao;

	@Autowired
	private NetworkAssociationDao networkAssociationDao;

	@Autowired
	private SolarLocationDao solarLocationDao;

	@Autowired
	private SolarNodeDao solarNodeDao;

	@Autowired
	private SolarNodeMetadataDao solarNodeMetadataDao;

	@Autowired
	private DatumMetadataBiz datumMetadataBiz;

	@Autowired
	private NetworkIdentificationBiz networkIdentificationBiz;

	@Bean
	public SolarNodeMetadataBiz solarNodeMetadataBiz() {
		DaoSolarNodeMetadataBiz biz = new DaoSolarNodeMetadataBiz(solarNodeMetadataDao);
		return biz;
	}

	@Bean
	public DataCollectorBiz dataCollectorBiz(
			@Qualifier(DATUM_COLLECTOR) DatumWriteOnlyDao datumWriteOnlyDao,
			SolarNodeMetadataBiz solarNodeMetadataBiz) {
		DaoDataCollectorBiz biz = new DaoDataCollectorBiz(datumWriteOnlyDao);
		biz.setMetaDao(metaDao);
		biz.setSolarLocationDao(solarLocationDao);
		biz.setSolarNodeDao(solarNodeDao);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setSolarNodeMetadataBiz(solarNodeMetadataBiz);
		return biz;
	}

	@Bean
	public NetworkIdentityBiz networkIdentityBiz() {
		return new SimpleNetworkIdentityBiz(networkIdentificationBiz, networkAssociationDao);
	}

}
