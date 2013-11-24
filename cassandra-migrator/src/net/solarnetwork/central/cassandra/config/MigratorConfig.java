/* ==================================================================
 * MigratorConfig.java - Nov 22, 2013 2:19:16 PM
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.solarnetwork.central.cassandra.MigrationTask;
import net.solarnetwork.central.cassandra.Migrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for {@link Migrator} app.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ JdbcConfig.class, CassandraConfig.class, MigrationTaskConfig.class })
public class MigratorConfig {

	@Autowired
	private JdbcConfig jdbcConfig;

	@Autowired
	private CassandraConfig cassandraConfig;

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${task.threads}")
	private Integer taskThreads;

	@Value("${task.names}")
	private String[] taskNames = new String[] { "all" };

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Bean
	public Migrator migrator() throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		List<MigrationTask> tasks = new ArrayList<MigrationTask>();
		if ( taskNames != null && taskNames.length > 0 ) {
			if ( taskNames.length == 1 && taskNames[0].equalsIgnoreCase("all") ) {
				Map<String, MigrationTask> beans = applicationContext
						.getBeansOfType(MigrationTask.class);
				if ( beans != null && beans.size() > 0 ) {
					tasks.addAll(beans.values());
				}
			} else {
				try {
					for ( String beanName : taskNames ) {
						tasks.add(applicationContext.getBean(beanName, MigrationTask.class));
					}
				} catch ( NoSuchBeanDefinitionException e ) {
					String msg = "Error in task.names in env.properties: " + e.getMessage();
					log.error(msg);
					throw new BeanCreationException(msg);
				}
			}
		}
		Migrator m = new Migrator(cassandraConfig.cassandraCluster(), executorService(), tasks);
		return m;
	}

	@Bean
	public ExecutorService executorService() {
		return Executors.newFixedThreadPool(taskThreads);
	}

	public String[] getTaskNames() {
		return taskNames;
	}

	public void setTaskNames(String[] taskNames) {
		this.taskNames = taskNames;
	}

}
