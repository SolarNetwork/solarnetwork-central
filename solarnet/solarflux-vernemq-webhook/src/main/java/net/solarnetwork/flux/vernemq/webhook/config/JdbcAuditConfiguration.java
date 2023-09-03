/* ========================================================================
 * Copyright 2021 SolarNetwork Foundation
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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional data source configuration for auditing.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(value = "app.datasource.audit.url", matchIfMissing = false)
public class JdbcAuditConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "app.datasource.audit")
  public DataSourceProperties auditDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("app.datasource.audit.tomcat")
  @Qualifier("audit")
  public DataSource auditDataSource(
      @Qualifier("auditDataSourceProperties") DataSourceProperties properties) {
    DataSource dataSource = properties.initializeDataSourceBuilder()
        .type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
    return dataSource;
  }

}
