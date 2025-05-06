/* ==================================================================
 * S3DatumExportDestinationServiceTests.java - 11/04/2018 10:55:17 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.dest.s3.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.dest.s3.S3DatumExportDestinationService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Test cases for the {@link S3DatumExportDestinationService} class.
 *
 * <p>
 * Note this test requires a {@code s3} system property be defined with some
 * value, and a {@code s3-export-dest.properties} classpath resource be created
 * with properties that define the S3 connection details to use:
 * </p>
 *
 * <pre>{@code
 * path = https://s3.us-west-2.amazonaws.com/solarnetwork-dev-testing/data-exports/unittest
 * accessKey = AWS_ACCESS_TOKEN_HERE
 * secretKey = AWS_TOKEN_SECRET_HERE
 * storageClass = REDUCED_REDUNDANCY
 * }</pre>
 *
 * @author matt
 * @version 2.0
 */
@EnabledIfSystemProperty(named = "test.s3", matches = ".*")
public class S3DatumExportDestinationServiceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static Properties TEST_PROPS;

	private ExecutorService executorService;
	private S3Uri s3Uri;

	@BeforeAll
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = S3DatumExportDestinationServiceTests.class.getClassLoader()
					.getResourceAsStream("s3-export-dest.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		TEST_PROPS = p;
	}

	private String region() {
		String region = TEST_PROPS.getProperty("region", null);
		if ( region == null ) {
			String path = TEST_PROPS.getProperty("path");
			Matcher m = Pattern.compile("://s3\\.([^.]+)\\.").matcher(path);
			if ( m.find() ) {
				region = m.group(1);
			} else {
				throw new IllegalStateException("region property not defined");
			}
		}
		return region;
	}

	@BeforeEach
	public void setup() {
		executorService = Executors.newCachedThreadPool();

		s3Uri = S3Utilities.builder().region(Region.of(region())).build()
				.parseUri(URI.create(TEST_PROPS.getProperty("path")));

	}

	@AfterEach
	public void teardown() {
		if ( executorService != null ) {
			executorService.shutdownNow();
		}
	}

	private ListObjectsV2Response listS3Folder(S3Client client) {
		return client.listObjectsV2(r -> r.bucket(s3Uri.bucket().get()).prefix(s3Uri.key().get()));
	}

	private void cleanS3Folder(S3Client client) {
		ListObjectsV2Response result = listS3Folder(client);
		if ( result.keyCount() > 0 ) {
			List<ObjectIdentifier> keys = new ArrayList<>();
			for ( S3Object obj : result.contents() ) {
				if ( s3Uri.key() != null && s3Uri.key().isPresent()
						&& s3Uri.key().get().equals(obj.key()) ) {
					continue;
				}
				keys.add(ObjectIdentifier.builder().key(obj.key()).build());
			}
			if ( !keys.isEmpty() ) {
				DeleteObjectsResponse deleteResult = client
						.deleteObjects(r -> r.bucket(s3Uri.bucket().get()).delete(d -> d.objects(keys)));
				if ( deleteResult.deleted() != null ) {
					log.info("Deleted objects from S3: " + deleteResult.deleted().stream()
							.map(o -> o.key()).collect(Collectors.toList()));
				}
			}
		}
	}

	private String objectAsString(S3Client client, String key) {
		try (ResponseInputStream<GetObjectResponse> in = client
				.getObject(r -> r.bucket(s3Uri.bucket().get()).key(key))) {
			return FileCopyUtils.copyToString(new InputStreamReader(in, UTF_8));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private S3Client s3Client() {
		S3ClientBuilder builder = S3Client.builder()
				.httpClient(new ApacheSdkHttpService().createHttpClientBuilder().build())
				.region(s3Uri.region().get());
		String accessKey = TEST_PROPS.getProperty("accessKey");
		String secretKey = TEST_PROPS.getProperty("secretKey");
		builder = builder.credentialsProvider(
				StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		return builder.build();
	}

	private String getObjectKeyPrefix() {
		String keyPrefix = s3Uri.uri().getPath();
		if ( keyPrefix.startsWith("/") ) {
			keyPrefix = keyPrefix.substring(1);
		}
		int bucketIdx = keyPrefix.indexOf('/');
		if ( bucketIdx > 0 ) {
			keyPrefix = keyPrefix.substring(bucketIdx + 1);
		}
		return keyPrefix;
	}

	@Test
	public void settingSpecifiers() {
		// given
		S3DatumExportDestinationService service = new S3DatumExportDestinationService(executorService);

		// when
		List<SettingSpecifier> specs = service.getSettingSpecifiers();

		// then
		assertThat("Setting specs provided", specs, hasSize(5));

		Set<String> keys = specs.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toSet());
		assertThat("Setting keys", keys, containsInAnyOrder("accessKey", "secretKey", "path",
				"filenameTemplate", "storageClass"));
	}

	private DatumExportResource getTestResource() {
		Resource r = new ClassPathResource("test-datum-export-01.txt", getClass());
		return new BasicDatumExportResource(r, "text/plain;charset=UTF-8");
	}

	@Test
	public void export() throws IOException {
		// GIVEN
		S3Client client = s3Client();
		cleanS3Folder(client);

		S3DatumExportDestinationService service = new S3DatumExportDestinationService(executorService);

		Instant ts = LocalDateTime.of(2018, 4, 11, 11, 50).atZone(ZoneId.of("Pacific/Auckland"))
				.toInstant();

		BasicConfiguration config = new BasicConfiguration();
		config.setName(UUID.randomUUID().toString());

		BasicDestinationConfiguration destConfig = new BasicDestinationConfiguration();
		destConfig.setServiceIdentifier(service.getId());
		Map<String, Object> destProps = new HashMap<>();
		TEST_PROPS.forEach((k, v) -> destProps.put(k.toString(), v));
		destConfig.setServiceProps(destProps);
		config.setDestinationConfiguration(destConfig);

		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setId(UUID.randomUUID());
		taskInfo.setExportDate(ts);
		Map<String, Object> runtimeProps = config.createRuntimeProperties(taskInfo, null,
				new CsvDatumExportOutputFormatService());

		DatumExportResource rsrc = getTestResource();

		// WHEN
		List<Double> progress = new ArrayList<>(8);
		service.export(config, Collections.singleton(rsrc), runtimeProps,
				new ProgressListener<DatumExportService>() {

					@Override
					public void progressChanged(DatumExportService context, double amountComplete) {
						assertThat("Context is service", context, sameInstance(service));
						progress.add(amountComplete);
					}
				});

		// THEN
		assertThat("Progress was made", progress, not(hasSize(0)));
		assertThat("Progress complete", progress.get(progress.size() - 1), equalTo((Double) 1.0));

		// now list our folder to verify expected result
		ListObjectsV2Response listing = listS3Folder(client);
		Set<String> keys = listing.contents().stream().map(S3Object::key).collect(toSet());

		String keyPrefix = getObjectKeyPrefix();
		assertThat(keys, containsInAnyOrder(keyPrefix + "/data-export-2018-04-10.csv"));

		String exportContent = objectAsString(client, listing.contents().get(0).key());
		assertThat("Exported content", exportContent,
				equalTo(FileCopyUtils.copyToString(new InputStreamReader(
						getClass().getResourceAsStream("test-datum-export-01.txt"), UTF_8))));
	}

}
