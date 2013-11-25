/* ==================================================================
 * MigrationTaskConfig.java - Nov 25, 2013 9:29:07 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.solarnetwork.central.cassandra.MigrateConsumptionDatum;
import net.solarnetwork.central.cassandra.MigrateDatumSupport;
import net.solarnetwork.central.cassandra.MigrateHardwareControlDatum;
import net.solarnetwork.central.cassandra.MigratePowerDatum;
import net.solarnetwork.central.cassandra.MigratePowerDatumAggregateDaily;
import net.solarnetwork.central.cassandra.MigratePowerDatumAggregateHourly;
import net.solarnetwork.central.cassandra.MigratePriceDatum;
import net.solarnetwork.central.cassandra.MigratePriceDatumAggregateDaily;
import net.solarnetwork.central.cassandra.MigratePriceDatumAggregateHourly;
import net.solarnetwork.central.cassandra.MigrateWeatherDatum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Bean configuration for MigrationTask objects.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ EnvironmentConfig.class, JdbcConfig.class, CassandraConfig.class })
public class MigrationTaskConfig {

	@Value("${task.threads}")
	private Integer taskThreads;

	@Autowired
	private JdbcConfig jdbcConfig;

	@Autowired
	private CassandraConfig cassandraConfig;

	@Autowired
	private EnvironmentConfig environmentConfig;

	@Value("${datum.maxProcess}")
	private Integer maxProcess;

	@Bean
	public ExecutorService executorService() {
		return Executors.newFixedThreadPool(taskThreads);
	}

	private void setupMigrateDatumSupport(MigrateDatumSupport t) {
		t.setCluster(cassandraConfig.cassandraCluster());
		t.setJdbcOperations(jdbcConfig.jdbcOperations());
		t.setMaxResults(maxProcess);
		t.setExecutorService(executorService());
	}

	@Bean
	public MigrateHardwareControlDatum migrateHardwareControlDatum() {
		MigrateHardwareControlDatum t = new MigrateHardwareControlDatum();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigrateConsumptionDatum migrateConsumptionDatum() {
		MigrateConsumptionDatum t = new MigrateConsumptionDatum();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePowerDatum migratePowerDatum() {
		MigratePowerDatum t = new MigratePowerDatum();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePriceDatum migratePriceDatum() {
		MigratePriceDatum t = new MigratePriceDatum();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePriceDatumAggregateHourly migratePriceDatumHourly() {
		MigratePriceDatumAggregateHourly t = new MigratePriceDatumAggregateHourly();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePriceDatumAggregateDaily migratePriceDatumDaily() {
		MigratePriceDatumAggregateDaily t = new MigratePriceDatumAggregateDaily();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePowerDatumAggregateDaily migratePowerDatumDaily() {
		MigratePowerDatumAggregateDaily t = new MigratePowerDatumAggregateDaily();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigratePowerDatumAggregateHourly migratePowerDatumHourly() {
		MigratePowerDatumAggregateHourly t = new MigratePowerDatumAggregateHourly();
		setupMigrateDatumSupport(t);
		return t;
	}

	@Bean
	public MigrateWeatherDatum migrateWeatherDatum() {
		MigrateWeatherDatum t = new MigrateWeatherDatum();
		setupMigrateDatumSupport(t);
		t.setSkyConditionMapping(environmentConfig.skyConditionMapping());
		return t;
	}

}
