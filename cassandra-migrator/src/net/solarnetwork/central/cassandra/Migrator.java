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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.central.cassandra.config.MigratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.datastax.driver.core.Cluster;

/**
 * Application class to migrate data from existing DAO to Cassandra.
 * 
 * @author matt
 * @version 1.0
 */
public class Migrator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Cluster cassandra;
	private final ExecutorService executorService;
	private final Collection<MigrationTask> tasks;

	/**
	 * Construct with deps.
	 * 
	 * @param jdbcOperations
	 *        the JDBC ops
	 * @param cluster
	 *        the Cassandra cluster
	 * @param executorService
	 *        the ExecutorService
	 * @param tasks
	 *        the tasks to execute
	 */
	public Migrator(Cluster cluster, ExecutorService executorService, Collection<MigrationTask> tasks) {
		super();
		this.cassandra = cluster;
		this.executorService = executorService;
		this.tasks = tasks;
	}

	public void go() {
		try {
			log.info("Howdy there, I'm ready to go with Cassandra connection to {}", cassandra
					.getMetadata().getClusterName());
			List<MigrationResult> results = new ArrayList<MigrationResult>(tasks.size());
			List<Future<MigrationResult>> running = new ArrayList<Future<MigrationResult>>(tasks.size());
			for ( MigrationTask t : tasks ) {
				log.info("Submitting task {}", t.getClass().getName());
				running.add(executorService.submit(t));
			}

			// loop until all tasks, and their subtasks, are complete
			while ( running.size() > 0 ) {
				Future<MigrationResult> someResult = running.iterator().next();
				MigrationResult result = null;
				try {
					result = someResult.get();
				} catch ( InterruptedException e ) {
					log.warn("Interrupted waiting for subtask to complete: " + e.getMessage());
				}
				if ( someResult.isDone() ) {
					running.remove(someResult);
					if ( result != null ) {
						results.add(result);
						running.addAll(result.getSubtasks());
					}
				}
			}

			executorService.shutdown();
			executorService.awaitTermination(365, TimeUnit.DAYS);
			StringBuilder buf = new StringBuilder();
			for ( MigrationResult r : results ) { // TODO: sort
				buf.append(r.getStatusMessage()).append("\n");
			}
			log.info(
					"Alrighty then, I'm all done. I hope that went as well as expected! Here are your results: \n{}",
					buf);
		} catch ( InterruptedException e ) {
			log.warn("Interrupted while waiting for tasks to complete: " + e.getMessage());
		} catch ( ExecutionException e ) {
			log.warn("ExecutionException processing results: " + e.getMessage());
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
