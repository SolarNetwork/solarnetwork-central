/* ==================================================================
 * ServiceConfiguration.java - 16/01/2026 3:10:45 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.flux.vernemq.webhook.config;

import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;

/**
 * Service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class ServiceConfiguration {

	@Value("${solarnetwork.api.host:data.solarnetwork.net}")
	private String snHost = "data.solarnetwork.net";

	@Value("${solarnewtork.api.authPath:/solarflux/auth}")
	private String snPath = "/solarflux/auth";

	@Value("${solarnetwork.api.maxDateSkew:900000}")
	private long authMaxDateSkew = JdbcAuthService.DEFAULT_MAX_DATE_SKEW;

	@Value("${mqtt.forceCleanSession:true}")
	private boolean forceCleanSession = true;

	@Value("${auth.nodeIpMask:#{null}}")
	private String nodeIpMask = null;

	@Value("${auth.requireTokenClientIdPrefix:true}")
	private boolean requireTokenClientIdPrefix = true;

	@Value("${auth.allowDirectTokenAuthentication:true}")
	private boolean allowDirectTokenAuthentication = true;

	@Autowired
	private AuthorizationEvaluator authorizationEvaluator;

	@Autowired(required = false)
	@Qualifier("actor")
	private Cache<String, Actor> actorCache;

	@Autowired
	private javax.sql.DataSource dataSource;

	@Autowired(required = false)
	@Qualifier("audit")
	private javax.sql.DataSource auditDataSource;

	/**
	 * The {@link AuthService}.
	 * 
	 * @return the service
	 */
	@Bean
	public JdbcAuthService authService() {
		JdbcAuthService service = new JdbcAuthService(new JdbcTemplate(dataSource),
				authorizationEvaluator, auditService());
		service.setSnHost(snHost);
		service.setSnPath(snPath);
		service.setMaxDateSkew(authMaxDateSkew);
		service.setForceCleanSession(forceCleanSession);
		service.setActorCache(actorCache);
		service.setIpMask(nodeIpMask);
		service.setRequireTokenClientIdPrefix(requireTokenClientIdPrefix);
		service.setAllowDirectTokenAuthentication(allowDirectTokenAuthentication);
		return service;
	}

	/**
	 * The audit service.
	 * 
	 * @return the service
	 */
	@ConfigurationProperties(prefix = "app.audit.jdbc")
	@Bean(destroyMethod = "disableWriting")
	public JdbcAuditService auditService() {
		JdbcAuditService service = new JdbcAuditService(
				auditDataSource != null ? auditDataSource : dataSource);
		service.enableWriting();
		return service;
	}

}
