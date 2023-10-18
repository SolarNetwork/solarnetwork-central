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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.dest.s3.S3DatumExportDestinationService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.BasicDestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link S3DatumExportDestinationService} class.
 * 
 * @author matt
 * @version 2.0
 */
public class S3DatumExportDestinationServiceTests extends AbstractCentralTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static Properties TEST_PROPS;

	@BeforeClass
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

	private ListObjectsV2Result listFolder(AmazonS3 client) {
		AmazonS3URI uri = new AmazonS3URI(TEST_PROPS.getProperty("path"));
		return client.listObjectsV2(uri.getBucket(), uri.getKey());
	}

	private void cleanS3Folder(AmazonS3 client) {
		// clear out dir
		AmazonS3URI uri = new AmazonS3URI(TEST_PROPS.getProperty("path"));

		ListObjectsV2Result result = listFolder(client);

		DeleteObjectsRequest req = new DeleteObjectsRequest(uri.getBucket());
		if ( result.getKeyCount() > 0 ) {
			for ( S3ObjectSummary obj : result.getObjectSummaries() ) {
				req.getKeys().add(new KeyVersion(obj.getKey()));
			}
			DeleteObjectsResult deleteResult = client.deleteObjects(req);
			if ( deleteResult.getDeletedObjects() != null ) {
				log.info("Deleted objects from S3: " + deleteResult.getDeletedObjects().stream()
						.map(o -> o.getKey()).collect(Collectors.toList()));
			}
		}
	}

	private String getObjectAsString(AmazonS3 client, String key) {
		AmazonS3URI uri = new AmazonS3URI(TEST_PROPS.getProperty("path"));
		S3Object obj = client.getObject(uri.getBucket(), key);
		try {
			return FileCopyUtils.copyToString(new InputStreamReader(obj.getObjectContent(), "UTF-8"));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void settingSpecifiers() {
		// given
		S3DatumExportDestinationService service = new S3DatumExportDestinationService();

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

	private AmazonS3 getS3Client() {
		AmazonS3URI uri = new AmazonS3URI(TEST_PROPS.getProperty("path"));
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(uri.getRegion());
		String accessKey = TEST_PROPS.getProperty("accessKey");
		String secretKey = TEST_PROPS.getProperty("secretKey");
		builder = builder.withCredentials(
				new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		return builder.build();
	}

	private String getObjectKeyPrefix() {
		AmazonS3URI uri = new AmazonS3URI(TEST_PROPS.getProperty("path"));
		String keyPrefix = uri.getURI().getPath();
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
	public void export() throws IOException {
		// GIVEN
		AmazonS3 client = getS3Client();
		cleanS3Folder(client);

		S3DatumExportDestinationService service = new S3DatumExportDestinationService();

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

		Map<String, Object> runtimeProps = config.createRuntimeProperties(ts, null,
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
		ListObjectsV2Result listing = listFolder(client);
		Set<String> keys = listing.getObjectSummaries().stream().map(S3ObjectSummary::getKey)
				.collect(Collectors.toSet());
		String keyPrefix = getObjectKeyPrefix();
		assertThat(keys, containsInAnyOrder(keyPrefix + "/data-export-2018-04-10.csv"));

		String exportContent = getObjectAsString(client, listing.getObjectSummaries().get(0).getKey());
		assertThat("Exported content", exportContent,
				equalTo(FileCopyUtils.copyToString(new InputStreamReader(
						getClass().getResourceAsStream("test-datum-export-01.txt"), "UTF-8"))));
	}

}
