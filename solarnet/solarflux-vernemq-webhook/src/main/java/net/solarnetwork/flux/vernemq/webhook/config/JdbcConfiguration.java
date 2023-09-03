/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.config;

import javax.cache.Cache;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;

/**
 * Configuration for JDBC based services.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
public class JdbcConfiguration {

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

  @Autowired(required = false)
  @Qualifier("audit")
  private DataSource auditDataSource;

  @Autowired
  private AuthorizationEvaluator authorizationEvaluator;

  @Autowired(required = false)
  @Qualifier("actor")
  private Cache<String, Actor> actorCache;

  /**
   * The {@link AuthService}.
   * 
   * @return the service
   */
  @Bean
  public JdbcAuthService authService() {
    JdbcAuthService service = new JdbcAuthService(
        new JdbcTemplate(primaryDataSource(dataSourceProperties())), authorizationEvaluator,
        auditService());
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

  @ConfigurationProperties(prefix = "app.audit.jdbc")
  @Bean(destroyMethod = "disableWriting")
  public JdbcAuditService auditService() {
    JdbcAuditService service = new JdbcAuditService(
        auditDataSource != null ? auditDataSource : primaryDataSource(dataSourceProperties()));
    service.enableWriting();
    return service;
  }

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource.tomcat")
  public DataSource primaryDataSource(DataSourceProperties properties) {
    DataSource dataSource = properties.initializeDataSourceBuilder()
        .type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
    return dataSource;
  }

}
