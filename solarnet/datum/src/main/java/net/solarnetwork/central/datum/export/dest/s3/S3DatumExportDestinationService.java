/* ==================================================================
 * S3DatumExportDestinationService.java - 11/04/2018 6:19:11 AM
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

package net.solarnetwork.central.datum.export.dest.s3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DestinationConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportDestinationService;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.StringUtils;

/**
 * AWS S3 implementation of {@link DatumExportDestinationService}.
 * 
 * @author matt
 * @version 2.0
 */
public class S3DatumExportDestinationService extends BaseDatumExportDestinationService {

	public S3DatumExportDestinationService() {
		super("net.solarnetwork.central.datum.export.dest.s3.S3DatumExportDestinationService");
	}

	@Override
	public String getDisplayName() {
		return "S3 Datum Export Destination Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier("path", ""));
		result.add(new BasicTextFieldSettingSpecifier("filenameTemplate",
				S3DestinationProperties.DEFAULT_FILENAME_TEMPLATE));
		result.add(new BasicTextFieldSettingSpecifier("accessKey", ""));
		result.add(new BasicTextFieldSettingSpecifier("secretKey", "", true));
		result.add(new BasicTextFieldSettingSpecifier("storageClass", ""));
		return result;
	}

	@Override
	public void export(Configuration config, Iterable<DatumExportResource> resources,
			Map<String, ?> runtimeProperties, ProgressListener<DatumExportService> progressListener)
			throws IOException {
		if ( config == null ) {
			throw new IOException("No configuration provided.");
		}
		if ( resources == null ) {
			throw new IOException("No export resource provided.");
		}
		DestinationConfiguration destConfig = config.getDestinationConfiguration();
		if ( destConfig == null ) {
			throw new IOException("No destination configuration provided.");
		}
		S3DestinationProperties props = new S3DestinationProperties();
		ClassUtils.setBeanProperties(props, destConfig.getServiceProperties(), true);
		if ( !props.isValid() ) {
			throw new IOException("Service configuration is not valid.");
		}

		List<DatumExportResource> resourceList = StreamSupport.stream(resources.spliterator(), false)
				.collect(Collectors.toList());
		final int resourceCount = resourceList.size();
		final com.amazonaws.event.ProgressListener s3ProgressListener = new com.amazonaws.event.ProgressListener() {

			private double overallProgress = 0;

			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				ProgressEventType type = progressEvent.getEventType();
				if ( type == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT ) {
					double resourceProgress = (double) progressEvent.getBytesTransferred()
							/ (double) progressEvent.getBytes();
					overallProgress += (resourceProgress / resourceCount);
					progressListener.progressChanged(S3DatumExportDestinationService.this,
							overallProgress);
				}

			}

		};

		AmazonS3 client = getClient(props);
		AmazonS3URI uri = props.getUri();
		for ( ListIterator<DatumExportResource> itr = resourceList.listIterator(); itr.hasNext(); ) {
			// if we have >1 resource to upload, then we'll insert an index suffix in the key name, like -1, -2, etc.
			int idx = (resourceCount > 1 ? itr.nextIndex() + 1 : 0);
			DatumExportResource resource = itr.next();
			String key = getDestinationPath(props, runtimeProperties, idx);
			ObjectMetadata objectMetadata = new ObjectMetadata();
			if ( resource.getContentType() != null ) {
				objectMetadata.setContentType(resource.getContentType());
			}
			objectMetadata.setContentLength(resource.contentLength());
			objectMetadata.setLastModified(new Date(resource.lastModified()));
			try (InputStream in = resource.getInputStream()) {
				PutObjectRequest req = new PutObjectRequest(uri.getBucket(), key, in, objectMetadata);
				if ( props.getStorageClass() != null ) {
					req.setStorageClass(props.getStorageClass());
				}
				if ( progressListener != null ) {
					req.withGeneralProgressListener(s3ProgressListener);
				}
				client.putObject(req);
			} catch ( AmazonServiceException e ) {
				log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}",
						e.getMessage(), e.getStatusCode(), e.getErrorCode(), e.getErrorType(),
						e.getRequestId());
				throw new RemoteServiceException("Error putting S3 object at " + key, e);
			} catch ( AmazonClientException e ) {
				log.debug("Error communicating with AWS: {}", e.getMessage());
				throw new IOException("Error communicating with AWS", e);
			}
		}
	}

	private String getDestinationPath(S3DestinationProperties props, Map<String, ?> runtimeProperties,
			int resourceIndex) {
		AmazonS3URI uri = props.getUri();
		String key = uri.getKey() + "/"
				+ StringUtils.expandTemplateString(props.getFilenameTemplate(), runtimeProperties);
		if ( resourceIndex > 0 ) {
			Object ext = runtimeProperties.get(Configuration.PROP_FILENAME_EXTENSION);
			String suffix = "." + ext;
			if ( ext != null && key.endsWith(suffix) ) {
				key = key.substring(0, key.length() - suffix.length()) + "-" + resourceIndex + suffix;
			} else {
				key += "-" + resourceIndex;
			}
		}
		return key;
	}

	private AmazonS3 getClient(S3DestinationProperties props) {
		AmazonS3URI uri = props.getUri();
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(uri.getRegion());
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder = builder.withCredentials(
					new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		}
		return builder.build();
	}

}
