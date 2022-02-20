/* ==================================================================
 * ApplicationMetadataEnvironmentPostProcessor.java - 21/02/2022 10:47:26 AM
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

import static java.lang.String.format;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StreamUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.cloud.domain.ContainerMetadata;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.ByteUtils;

/**
 * Load up application metadata into the environment.
 * 
 * @author matt
 * @version 1.0
 */
public class ApplicationMetadataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The application metadata properties prefix.
	 */
	public static final String APP_PROP_PREFIX = "app.meta.";

	private final Log logger;

	// Before ConfigFileApplicationListener so values there can use these ones
	private int order = ConfigDataEnvironmentPostProcessor.ORDER - 1;

	/**
	 * Create a new {@link ApplicationMetadataEnvironmentPostProcessor}
	 * instance.
	 * 
	 * @param logger
	 *        the logger to use
	 */
	public ApplicationMetadataEnvironmentPostProcessor(Log logger) {
		this.logger = logger;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		Properties appProps = new Properties();

		String appName = environment.getProperty("application.title", "");
		addWithPrefix(appProps, "name", appName);

		String appVersion = environment.getProperty("application.version", "");
		addWithPrefix(appProps, "version", appVersion);

		String appInstanceId = null;
		Map<String, Object> sysEnv = environment.getSystemEnvironment();
		Object ecsMetaUri = sysEnv.get("ECS_CONTAINER_METADATA_URI_V4");
		if ( ecsMetaUri != null ) {
			ContainerMetadata meta = ecsContainerMetadataV4(ecsMetaUri.toString());
			if ( meta != null ) {
				appInstanceId = meta.getContainerId();
			}
		}
		if ( appInstanceId == null ) {
			// try using hostname, first from environment
			Object hostname = sysEnv.get("HOSTNAME");
			if ( hostname != null && !hostname.toString().isBlank() ) {
				appInstanceId = hostname.toString();
			} else {
				// next from `hostname` commmand
				hostname = exec("hostname");
				if ( hostname != null && !hostname.toString().isBlank() ) {
					appInstanceId = hostname.toString();
				}
			}
		}
		addWithPrefix(appProps, "instance-id",
				appInstanceId != null ? appInstanceId : UUID.randomUUID().toString());

		logger.info("App metadata: " + appProps);

		MutablePropertySources propertySources = environment.getPropertySources();
		if ( propertySources.contains(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME) ) {
			propertySources.addAfter(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
					new PropertiesPropertySource("solarnet", appProps));
		} else {
			propertySources.addFirst(new PropertiesPropertySource("solarnet", appProps));
		}
	}

	private void addWithPrefix(Properties props, String key, Object value) {
		props.put(APP_PROP_PREFIX + key, value);
	}

	private String exec(String command) {
		try (InputStream in = Runtime.getRuntime().exec(command).getInputStream()) {
			return StreamUtils.copyToString(in, ByteUtils.UTF8).trim();
		} catch ( Exception e ) {
			logger.warn(format("Error executing [%s]", command), e);
			return null;
		}
	}

	public ContainerMetadata ecsContainerMetadataV4(String uri) {
		ObjectMapper mapper = JsonUtils.newObjectMapper();
		try (JsonParser p = mapper.createParser(new URL(uri))) {
			ObjectNode root = p.readValueAsTree();
			JsonNode cluster = root.get("Cluster");
			JsonNode taskArn = root.get("TaskARN");
			if ( taskArn == null ) {
				logger.error(format("TaskARN not available in ECS container metadata from [%s]: %s", uri,
						root));
				return null;
			}
			String taskId = taskArn.textValue();
			if ( taskId == null || taskId.isBlank() ) {
				logger.error(format("TaskARN empty in ECS container metadata from [%s]: %s", uri, root));
				return null;
			}
			if ( cluster != null ) {
				String clusterId = cluster.textValue();
				if ( clusterId != null && !clusterId.isBlank() && taskId.startsWith(clusterId) ) {
					// strip cluster ID from taskARN to get final task ID
					taskId = taskId.substring(clusterId.length());
				}
			}
			ContainerMetadata meta = new ContainerMetadata(taskId);
			logger.info(format("Discovered ECS metadata from [%s]: %s", uri, root));
			return meta;
		} catch ( IOException e ) {
			logger.error(
					format("IO error reading ECS container metadata from [%s]: %s", uri, e.toString()));
		}
		return null;
	}

}
