/* ==================================================================
 * AwsSecretsBiz.java - 27/08/2022 2:13:52 pm
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

package net.solarnetwork.central.cloud.aws.biz;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import javax.cache.Cache;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.biz.SecretsBiz;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;

/**
 * Implementation of {@link SecretsBiz} using AWS Secrets Manager.
 * 
 * @author matt
 * @version 1.0
 */
public class AwsSecretsBiz implements SecretsBiz {

	private final SecretsManagerClient client;
	private Cache<String, String> secretCache;

	/**
	 * Constructor.
	 * 
	 * @param region
	 *        the AWS region name
	 * @param accessKey
	 *        the access key
	 * @param accessKeySecret
	 *        the access key secret
	 */
	public AwsSecretsBiz(String region, String accessKey, String accessKeySecret) {
		this(Region.of(region), StaticCredentialsProvider
				.create(AwsBasicCredentials.create(accessKey, accessKeySecret)));
	}

	/**
	 * Constructor.
	 * 
	 * @param region
	 *        the AWS region
	 * @param credentialsProvider
	 *        the credentials provider
	 */
	public AwsSecretsBiz(Region region, AwsCredentialsProvider credentialsProvider) {
		super();
		// @formatter:off
		client = SecretsManagerClient.builder()
				.region(requireNonNullArgument(region, "region"))
				.credentialsProvider(requireNonNullArgument(credentialsProvider, "credentialsProvider"))
				.build();
		// @formatter:on
	}

	@Override
	public void putSecret(String secretName, String secretValue) {
		final Cache<String, String> cache = this.secretCache;
		try {
			try {
				CreateSecretRequest createReq = CreateSecretRequest.builder().name(secretName)
						.secretString(secretValue).build();
				client.createSecret(createReq);
			} catch ( ResourceExistsException ree ) {
				// update existing secret
				PutSecretValueRequest putReq = PutSecretValueRequest.builder().secretId(secretName)
						.secretString(secretValue).build();
				client.putSecretValue(putReq);
			}
			if ( cache != null ) {
				cache.put(secretName, secretValue);
			}
		} catch ( SdkException e ) {
			throw new RemoteServiceException("Error retrieving secret [%s]".formatted(secretName), e);
		}
	}

	@Override
	public String getSecret(String secretName) {
		final Cache<String, String> cache = this.secretCache;
		if ( cache != null ) {
			String result = cache.get(secretName);
			if ( result != null ) {
				return result;
			}
		}
		try {
			GetSecretValueRequest req = GetSecretValueRequest.builder().secretId(secretName).build();
			GetSecretValueResponse res = client.getSecretValue(req);
			String result = res.secretString();
			if ( cache != null ) {
				cache.put(secretName, result);
			}
			return result;
		} catch ( SdkException e ) {
			throw new RemoteServiceException("Error retrieving secret [%s]".formatted(secretName), e);
		}
	}

	/**
	 * Configure a cache to use for secrets.
	 * 
	 * @param secretCache
	 *        the cache
	 */
	public void setSecretCache(Cache<String, String> secretCache) {
		this.secretCache = secretCache;
	}

}
