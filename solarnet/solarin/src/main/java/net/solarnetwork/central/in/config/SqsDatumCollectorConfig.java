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
import net.solarnetwork.central.datum.support.SqsDatumCollector;
import net.solarnetwork.central.datum.support.SqsDatumCollectorSettings;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.util.StatTracker;

/**
 * Configuration for the {@link DatumWriteOnlyDao}, without SQS.
 * 
 * @author matt
 * @version 1.0
 */
@Profile("datum-collector-sqs")
@Configuration(proxyBeanMethods = false)
public class SqsDatumCollectorConfig implements SolarInConfiguration {

	@Autowired
	private DatumEntityDao datumDao;

	@ConfigurationProperties(prefix = "app.solarin.sqs-collector")
	@Bean
	public SqsDatumCollectorSettings sqsDatumCollectorSettings() {
		return new SqsDatumCollectorSettings();
	}

	@Qualifier(DATUM_COLLECTOR)
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public SqsDatumCollector sqsDatumCollector(SqsDatumCollectorSettings settings) {
		StatTracker stats = new StatTracker("SqsDatumCollector", null,
				LoggerFactory.getLogger(SqsDatumCollector.class), settings.getStatFrequency());

		SqsDatumCollector collector = new SqsDatumCollector(settings.newAsyncClient(), settings.getUrl(),
				DatumJsonUtils.newDatumObjectMapper(),
				new ArrayBlockingQueue<>(settings.getWorkQueueSize()), datumDao, stats);
		collector.setReadConcurrency(settings.getReadConcurrency());
		collector.setWriteConcurrency(settings.getWriteConcurrency());
		if ( settings.getWorkItemMaxWait() != null ) {
			collector.setWorkItemMaxWaitMs(settings.getWorkItemMaxWait().toMillis());
		}
		collector.setReadMaxMessageCount(settings.getReadMaxMessageCount());
		if ( settings.getReadMaxWaitTime() != null ) {
			collector.setReadMaxWaitTimeSecs((int) settings.getReadMaxWaitTime().toSeconds());
		}
		if ( settings.getReadSleepMin() != null ) {
			collector.setReadSleepMinMs(settings.getReadSleepMin().toMillis());
		}
		if ( settings.getReadSleepMax() != null ) {
			collector.setReadSleepMaxMs(settings.getReadSleepMax().toMillis());
		}
		if ( settings.getReadSleepThrottleStep() != null ) {
			collector.setReadSleepThrottleStepMs(settings.getReadSleepThrottleStep().toMillis());
		}
		if ( settings.getShutdownWait() != null ) {
			collector.setShutdownWaitSecs((int) settings.getShutdownWait().toSeconds());
		}
		return collector;
	}

}
