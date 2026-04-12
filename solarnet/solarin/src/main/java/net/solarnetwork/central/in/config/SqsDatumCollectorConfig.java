/* ==================================================================
 * SqsDatumCollectorConfig.java - 30/04/2025 5:20:37 pm
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

import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import net.solarnetwork.central.datum.support.DatumJsonEntityCodec;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDaoGenericAdapter;
import net.solarnetwork.central.datum.v2.dao.GenericWriteOnlyDaoDatumAdapter;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.central.support.SqsOverflowQueueSettings;
import net.solarnetwork.util.StatTracker;

/**
 * Configuration for the {@link DatumWriteOnlyDao}, using SQS.
 * 
 * @author matt
 * @version 2.0
 */
@Profile("datum-collector-sqs")
@Configuration(proxyBeanMethods = false)
public class SqsDatumCollectorConfig implements SolarInConfiguration {

	@Autowired
	private DatumEntityDao datumDao;

	@ConfigurationProperties(prefix = "app.solarin.sqs-collector")
	@Qualifier(DATUM_COLLECTOR)
	@Bean
	public SqsOverflowQueueSettings sqsDatumCollectorSettings() {
		return new SqsOverflowQueueSettings();
	}

	@Qualifier(DATUM_COLLECTOR)
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public SqsOverflowQueue<Object, DatumPK> sqsDatumCollector(
			@Qualifier(DATUM_COLLECTOR) SqsOverflowQueueSettings settings) {
		StatTracker stats = new StatTracker("SqsDatumCollector", null,
				LoggerFactory.getLogger(SqsOverflowQueue.class), settings.getStatFrequency());

		var entityCodec = new DatumJsonEntityCodec(stats, DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER);

		var collector = new SqsOverflowQueue<Object, DatumPK>(stats, "DatumQueue-SQS",
				settings.newAsyncClient(), settings.getUrl(),
				new ArrayBlockingQueue<>(settings.getWorkQueueSize()),
				new LinkedHashSetBlockingQueue<>(9), new DatumWriteOnlyDaoGenericAdapter(datumDao),
				entityCodec);
		collector.setPingTestName("SQS Datum Collector");
		settings.configure(collector);
		return collector;
	}

	@Qualifier(DATUM_COLLECTOR)
	@Bean
	public GenericWriteOnlyDaoDatumAdapter sqsDatumWriteOnlyDao(
			@Qualifier(DATUM_COLLECTOR) SqsOverflowQueue<Object, DatumPK> queue) {
		return new GenericWriteOnlyDaoDatumAdapter(queue);
	}

}
