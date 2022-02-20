/* ==================================================================
 * EcsContainerMetadataConfig.java - 21/02/2022 9:20:39 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.config;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.cloud.domain.ContainerMetadata;
import net.solarnetwork.codec.JsonUtils;

/**
 * Configuration for ECS container metadata.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class EcsContainerMetadataConfig {

	private static final Logger log = LoggerFactory.getLogger(EcsContainerMetadataConfig.class);

	@ConditionalOnProperty("ECS_CONTAINER_METADATA_URI_V4")
	@Bean
	public ContainerMetadata ecsContainerMetadataV4(@Value("${ECS_CONTAINER_METADATA_URI_V4}") URI uri) {
		ObjectMapper mapper = JsonUtils.newObjectMapper();
		try (JsonParser p = mapper.createParser(uri.toURL())) {
			ObjectNode root = p.readValueAsTree();
			JsonNode cluster = root.get("Cluster");
			JsonNode taskArn = root.get("TaskARN");
			if ( taskArn == null ) {
				log.error("TaskARN not available in ECS container metadata from [{}]: {}", uri, root);
				return null;
			}
			String taskId = taskArn.textValue();
			if ( taskId == null || taskId.isBlank() ) {
				log.error("TaskARN empty in ECS container metadata from [{}]: {}", uri, root);
				return null;
			}
			if ( cluster != null ) {
				String clusterId = cluster.textValue();
				if ( clusterId != null && !clusterId.isBlank() && taskId.startsWith(clusterId) ) {
					// strip cluster ID from taskARN to get final task ID
					taskId = taskId.substring(clusterId.length());
				}
			}
			log.info("Discovered ECS metadata from [{}]: {}");
			return new ContainerMetadata(taskId);
		} catch ( IOException e ) {
			log.error("IO error reading ECS container metadata from [{}]: {}", uri, e.toString());
		}
		return null;
	}

}
