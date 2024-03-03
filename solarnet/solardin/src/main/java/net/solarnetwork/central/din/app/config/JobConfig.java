/* ==================================================================
 * JobConfig.java - 24/02/2024 11:49:04 am
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

package net.solarnetwork.central.din.app.config;

import static net.solarnetwork.central.din.config.DatumInputTransformServiceConfig.XSLT_TEMPLATES_QUALIFIER;
import javax.xml.transform.Templates;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.common.job.SharedValueCacheCleaner;
import net.solarnetwork.central.din.app.jobs.SolarDinJobs;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.support.SharedValueCache;

/**
 * Configuration for jobs.
 *
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(SolarDinJobs.JOBS_PROFILE)
public class JobConfig {

	/**
	 * A job to prune expired cached XSLT templates.
	 *
	 * @param cache
	 *        the cache to clean
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.din.xslt-templates-cache-prune")
	@Bean
	public ManagedJob xsltTemplatesCacheCleanerJob(
			@Qualifier(XSLT_TEMPLATES_QUALIFIER) SharedValueCache<String, Templates, String> cache) {
		return new SharedValueCacheCleaner(cache, "XSLT-Templates", SolarDinJobs.JOBS_GROUP);
	}

}
