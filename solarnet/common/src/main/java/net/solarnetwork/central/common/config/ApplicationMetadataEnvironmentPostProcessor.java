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
import static org.springframework.util.StringUtils.arrayToDelimitedString;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
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
 * @version 1.4
 */
public class ApplicationMetadataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The application metadata properties prefix.
	 */
	public static final String APP_PROP_PREFIX = "app.meta.";

	/** The default container ID maximum length. */
	public static final int DEFAULT_MAX_CONTAINER_ID_LENGTH = 8;

	/**
	 * The system environment variable for the maximum application instance ID length, when derived from
	 * an ECS container ID.
	 *
	 * <p>
	 * The variable value must be an integer; if less than {@literal 1} then no maximum length is
	 * enforced. If not defined then {@link #DEFAULT_MAX_CONTAINER_ID_LENGTH} will be used.
	 * </p>
	 */
	public static final String ENV_APP_ID_CONTAINER_ID_LENGTH = "APP_ID_CONTAINER_ID_LENGTH";

	private final DeferredLog logger = new DeferredLog();

	// Before ConfigFileApplicationListener so values there can use these
	private int order = ConfigDataEnvironmentPostProcessor.ORDER - 1;

	/**
	 * Create a new {@link ApplicationMetadataEnvironmentPostProcessor} instance.
	 */
	public ApplicationMetadataEnvironmentPostProcessor() {
		super();
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
		application.addInitializers(
				ctx -> logger.replayTo(ApplicationMetadataEnvironmentPostProcessor.class));

		Properties appProps = new Properties();

		String appName = environment.getProperty("application.title", "");
		if ( !appName.isBlank() ) {
			addWithPrefix(appProps, "name", appName);
		}

		String appVersion = environment.getProperty("application.version", "");
		if ( !appVersion.isBlank() ) {
			addWithPrefix(appProps, "version", appVersion);
		}

		String appInstanceId = null;
		Map<String, Object> sysEnv = environment.getSystemEnvironment();
		Object ecsMetaUri = sysEnv.get("ECS_CONTAINER_METADATA_URI_V4");
		if ( ecsMetaUri != null ) {
			ContainerMetadata meta = ecsContainerMetadataV4(ecsMetaUri + "/task");
			if ( meta != null ) {
				appInstanceId = meta.getContainerId();
				Object maxLengthProp = sysEnv.get(ENV_APP_ID_CONTAINER_ID_LENGTH);
				int maxLength = (maxLengthProp != null
						? Integer.parseInt(maxLengthProp.toString())
						: DEFAULT_MAX_CONTAINER_ID_LENGTH);
				if ( maxLength > 0 && appInstanceId.length() > maxLength ) {
					appInstanceId = appInstanceId.substring(0, maxLength);
				}
			}
		}
		if ( appInstanceId == null ) {
			// try using hostname, first from environment
			Object hostname = sysEnv.get("HOSTNAME");
			if ( hostname != null && !hostname.toString().isBlank() ) {
				appInstanceId = hostname.toString();
			} else {
				// next from `hostname` command
				hostname = exec(new String[] { "hostname" });
				if ( hostname != null && !hostname.toString().isBlank() ) {
					appInstanceId = hostname.toString();
				}
			}
		}
		addWithPrefix(appProps, "instance-id",
				appInstanceId != null ? appInstanceId : UUID.randomUUID().toString());

		logger.info("Environment app metadata: " + appProps);

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

	private String exec(String[] command) {
		try (InputStream in = Runtime.getRuntime().exec(command).getInputStream()) {
			return StreamUtils.copyToString(in, ByteUtils.UTF8).trim();
		} catch ( Exception e ) {
			logger.warn(format("Error executing [%s]", arrayToDelimitedString(command, " ")), e);
			return null;
		}
	}

	public ContainerMetadata ecsContainerMetadataV4(String uri) {
		ObjectMapper mapper = JsonUtils.newObjectMapper();
		try (JsonParser p = mapper.createParser(new URI(uri).toURL())) {
			ObjectNode root = p.readValueAsTree();
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
			int slashIdx = taskId.lastIndexOf('/');
			if ( slashIdx >= 0 && (slashIdx + 1) < taskId.length() ) {
				// strip cluster ID from taskARN to get final task ID
				taskId = taskId.substring(slashIdx + 1);
			}
			ContainerMetadata meta = new ContainerMetadata(taskId);
			logger.info(format("Discovered ECS metadata from [%s]: %s", uri, root));
			return meta;
		} catch ( URISyntaxException e ) {
			logger.error(format("URL syntax error for ECS container metadata from [%s]: %s", uri,
					e.toString()));
		} catch ( IOException e ) {
			logger.error(
					format("IO error reading ECS container metadata from [%s]: %s", uri, e.toString()));
		}
		return null;
	}

}
