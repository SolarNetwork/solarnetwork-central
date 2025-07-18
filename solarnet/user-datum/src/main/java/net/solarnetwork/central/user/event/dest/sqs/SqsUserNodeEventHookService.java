/* ==================================================================
 * SqsUserNodeEventHookService.java - 15/06/2020 4:53:08 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dest.sqs;

import static net.solarnetwork.central.user.event.dest.sqs.SqsStats.BasicCount.NodeEventsPublishFailed;
import static net.solarnetwork.central.user.event.dest.sqs.SqsStats.BasicCount.NodeEventsPublished;
import static net.solarnetwork.central.user.event.dest.sqs.SqsStats.BasicCount.NodeEventsReceived;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * SQS implementation of {@link UserNodeEventHookService}.
 *
 * @author matt
 * @version 3.0
 */
public class SqsUserNodeEventHookService extends BaseSettingsSpecifierLocalizedServiceInfoProvider
		implements UserNodeEventHookService {

	private final SqsStats sqsStats;
	private Cache<String, SqsDestination> destinationCache;

	private final ConcurrentMap<String, SqsDestination> cacheLock = new ConcurrentHashMap<>(30, 0.9f, 4);

	/**
	 * Constructor.
	 */
	public SqsUserNodeEventHookService() {
		this(new SqsStats("SqsUserNodeEventHook", 200));
	}

	/**
	 * Constructor.
	 *
	 * @param stats
	 *        the status object to use
	 * @throws IllegalArgumentException
	 *         if {@code stats} is {@literal null}
	 */
	public SqsUserNodeEventHookService(SqsStats stats) {
		super("net.solarnetwork.central.user.event.dest.sqs.SqsUserNodeEventHookService");
		if ( stats == null ) {
			throw new IllegalArgumentException("The stats argument must not be null.");
		}
		this.sqsStats = stats;
	}

	@Override
	public String getDisplayName() {
		return "AWS SQS Node Event Hook Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier("region", ""));
		result.add(new BasicTextFieldSettingSpecifier("queueName", ""));
		result.add(new BasicTextFieldSettingSpecifier("accessKey", ""));
		result.add(new BasicTextFieldSettingSpecifier("secretKey", "", true));
		return result;
	}

	@Override
	public boolean processUserNodeEventHook(UserNodeEventHookConfiguration config,
			UserNodeEventTask event) throws RepeatableTaskException {
		log.debug("Got user node event task {} for user {} hook {} with props {}", event.getId(),
				event.getUserId(), event.getHookId(), event.getTaskProperties());

		sqsStats.incrementAndGet(NodeEventsReceived);
		try {
			SqsDestinationProperties props = SqsDestinationProperties
					.ofServiceProperties(config.getServiceProperties());
			if ( !props.isValid() ) {
				throw new IllegalArgumentException("Service configuration is not valid.");
			}

			SqsDestination dest = getDestination(props);
			Map<String, Object> msg = event.asMessageData(config.getTopic());
			dest.sendJsonMessage(msg);

			sqsStats.incrementAndGet(NodeEventsPublished);

			return true;
		} catch ( RuntimeException e ) {
			sqsStats.incrementAndGet(NodeEventsPublishFailed);
			throw e;
		}
	}

	private SqsDestination getDestination(SqsDestinationProperties props) {
		final String key = keyForDestination(props);
		final Cache<String, SqsDestination> cache = getDestinationCache();

		// we want to serialize access to the SqsDestination objects, but Cache does not provide
		// computeIfAbsent so we go through a ConcurrentMap instead
		SqsDestination dest = cacheLock.computeIfAbsent(key, k -> {
			SqsDestination d = null;
			if ( cache != null ) {
				d = cache.get(key);
			}
			if ( d == null ) {
				log.debug("Creating SQS destination for {}@{}/{}", props.getAccessKey(),
						props.getRegion(), props.getQueueName());
				SqsClient client = createClient(props);
				String queueUrl;
				try {
					GetQueueUrlResponse urlRes = client
							.getQueueUrl((b) -> b.queueName(props.getQueueName()));
					queueUrl = urlRes.queueUrl();
				} catch ( QueueDoesNotExistException e ) {
					throw new IllegalArgumentException(
							String.format("Queue [%s] does not exist (using region %s).",
									props.getQueueName(), props.getRegion()));
				}
				d = new SqsDestination(client, queueUrl);
			}
			if ( cache != null ) {
				cache.putIfAbsent(key, d);
			}
			return d;
		});
		cacheLock.remove(key, dest);
		return dest;
	}

	private String keyForDestination(SqsDestinationProperties props) {
		StringBuilder buf = new StringBuilder();
		buf.append(props.getRegion());
		buf.append(props.getQueueName());
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty() ) {
			buf.append(accessKey);
			buf.append(secretKey);
		}
		return Base64.getEncoder().encodeToString(DigestUtils.sha1(buf.toString()));
	}

	private SqsClient createClient(SqsDestinationProperties props) {
		SqsClientBuilder builder = SqsClient.builder().region(Region.of(props.getRegion()));
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty() ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	/**
	 * Get the optional destination cache.
	 *
	 * @return the cache, or {@literal null}
	 */
	public Cache<String, SqsDestination> getDestinationCache() {
		return destinationCache;
	}

	/**
	 * Set an optional cache to use for destinations.
	 *
	 * @param destinationCache
	 *        the cache to set
	 */
	public void setDestinationCache(Cache<String, SqsDestination> destinationCache) {
		this.destinationCache = destinationCache;
	}

}
