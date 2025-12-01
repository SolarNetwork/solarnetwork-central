/* ==================================================================
 * UserNodeInstructionsConfig.java - 28/11/2025 10:37:48 am
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import java.time.Clock;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.common.http.BasicHttpOperations;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.user.biz.InstructionsExpressionService;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.domain.UsersUserEvents;
import net.solarnetwork.domain.Result;

/**
 * Configuration for user node instructions support.
 *
 * @author matt
 * @version 1.0
 */
@Profile(USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserNodeInstructionsConfig implements SolarNetUserConfiguration {

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private InstructorBiz instructorBiz;

	@Autowired
	private UserNodeInstructionTaskDao taskDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RestOperations restOps;

	@Qualifier(USER_INSTRUCTIONS)
	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private InstructionsExpressionService expressionService;

	@Autowired(required = false)
	private QueryAuditor queryAuditor;

	@Autowired(required = false)
	private UserServiceAuditor userServiceAuditor;

	@Autowired(required = false)
	@Qualifier(USER_INSTRUCTIONS_HTTP)
	private Cache<CachableRequestEntity, Result<?>> httpCache;

	@Value("${app.user-instr.allow-http-local-hosts:false}")
	private boolean allowHttpLocalHosts;

	@ConfigurationProperties(prefix = "app.user-instr.service")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public UserNodeInstructionService userNodeInstructionService() {
		var service = new DaoUserNodeInstructionService(Clock.systemUTC(),
				taskExecutor.getThreadPoolExecutor(), objectMapper, userEventAppenderBiz, instructorBiz,
				expressionService, nodeOwnershipDao, taskDao, datumDao, datumStreamMetadataDao);
		service.setQueryAuditor(queryAuditor);

		var http = new BasicHttpOperations(LoggerFactory.getLogger(DaoUserNodeInstructionService.class),
				userEventAppenderBiz, restOps, UsersUserEvents.INSTRUCTION_ERROR_TAGS);
		http.setHttpCache(httpCache);
		http.setAllowLocalHosts(allowHttpLocalHosts);
		http.setUserServiceAuditor(userServiceAuditor);
		http.setUserServiceKey(CONTENT_PROCESSED_AUDIT_SERVICE);
		service.setHttpOperations(http);

		return service;
	}

}
