/* ==================================================================
 * SimpleDatumCollectorConfig.java - 30/12/2025 3:27:53 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Configuration for the {@link DatumWriteOnlyDao}, without SQS.
 * 
 * <p>
 * <b>Note</b> this is not intended for production use. See
 * {@link SqsDatumCollectorConfig}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Profile("!datum-collector-sqs")
@Configuration(proxyBeanMethods = false)
public class SimpleDatumCollectorConfig implements SolarInConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SimpleDatumCollectorConfig.class);

	@Qualifier(DATUM_COLLECTOR)
	@Bean
	public DatumWriteOnlyDao simpleDatumCollector(DatumEntityDao dao) {
		log.warn("Using simple datum collector: this is not designed for production use.");
		return new DatumWriteOnlyDao() {

			@Override
			public DatumPK persist(GeneralObjectDatum<? extends GeneralObjectDatumKey> entity) {
				return dao.persist(entity);
			}

			@Override
			public DatumPK store(Datum datum) {
				return dao.store(datum);
			}

			@Override
			public DatumPK store(StreamDatum datum) {
				return dao.store(datum);
			}
		};
	}
}
