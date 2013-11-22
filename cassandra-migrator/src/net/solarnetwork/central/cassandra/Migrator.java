/* ==================================================================
 * Migrator.java - Nov 22, 2013 9:53:14 AM
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

package net.solarnetwork.central.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import com.datastax.driver.core.Cluster;

/**
 * Application class to migrate data from existing DAO to Cassandra.
 * 
 * @author matt
 * @version 1.0
 */
public class Migrator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public JdbcOperations jdbcOperations;
	private final Cluster cassandra;

	/**
	 * Construct with deps.
	 * 
	 * @param jdbcOperations
	 *        the JDBC ops
	 * @param cluster
	 *        the Cassandra cluster
	 */
	public Migrator(JdbcOperations jdbcOperations, Cluster cluster) {
		super();
		this.jdbcOperations = jdbcOperations;
		this.cassandra = cluster;
	}

	public void go() {
		try {
			log.info(
					"Howdy there, I'm ready to go with JDBC connection to {} and Cassandra connection to {}",
					jdbcOperations, cassandra.getMetadata().getClusterName());
			MigrateConsumptionDatum mc = new MigrateConsumptionDatum();
			mc.migrate(jdbcOperations, cassandra);
			log.info("Alrighty then, I'm all done. I hope that went as well as expected!",
					jdbcOperations);
		} finally {
			cassandra.shutdown();
		}
	}

	/**
	 * Execute the migrator.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(MigratorConfig.class);
		Migrator m = ctx.getBean(Migrator.class);
		m.go();
	}

}
