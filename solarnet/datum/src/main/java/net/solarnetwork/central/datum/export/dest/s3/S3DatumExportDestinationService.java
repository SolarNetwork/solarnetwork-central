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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static software.amazon.awssdk.core.async.AsyncRequestBody.fromInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener.Context.BytesTransferred;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

/**
 * AWS S3 implementation of {@link DatumExportDestinationService}.
 *
 * @author matt
 * @version 3.0
 */
public class S3DatumExportDestinationService extends BaseDatumExportDestinationService {

	private final ExecutorService executorService;

	/**
	 * Constructor.
	 *
	 * @param executorService
	 *        the executor service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public S3DatumExportDestinationService(ExecutorService executorService) {
		super("net.solarnetwork.central.datum.export.dest.s3.S3DatumExportDestinationService");
		this.executorService = requireNonNullArgument(executorService, "executorService");
	}

	@Override
	public String getDisplayName() {
		return "S3 Datum Export Destination Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(5);
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
		final TransferListener s3ProgressListener = new TransferListener() {

			private double overallProgress = 0;

			@Override
			public void bytesTransferred(BytesTransferred xfer) {
				TransferProgressSnapshot tps = xfer.progressSnapshot();
				double resourceProgress = tps.ratioTransferred().orElse(0.0);
				overallProgress += (resourceProgress / resourceCount);
				progressListener.progressChanged(S3DatumExportDestinationService.this, overallProgress);
			}

		};

		S3TransferManager mgr = getTransferManager(props);
		S3Uri uri = props.getUri();
		for ( ListIterator<DatumExportResource> itr = resourceList.listIterator(); itr.hasNext(); ) {
			// if we have >1 resource to upload, then we'll insert an index suffix in the key name, like -1, -2, etc.
			int idx = (resourceCount > 1 ? itr.nextIndex() + 1 : 0);
			DatumExportResource resource = itr.next();
			String key = getDestinationPath(props, runtimeProperties, idx);
			// TODO: how set in v2 SDK? objectMetadata.setLastModified(new Date(resource.lastModified()));
			try (InputStream in = resource.getInputStream()) {
				PutObjectRequest.Builder req = PutObjectRequest.builder().key(key)
						.contentLength(resource.contentLength());
				if ( uri.bucket().isPresent() ) {
					req.bucket(uri.bucket().get());
				}
				if ( resource.getContentType() != null ) {
					req.contentType(resource.getContentType());
				}
				if ( props.getStorageClass() != null ) {
					req.storageClass(props.getStorageClass());
				}
				UploadRequest.Builder up = UploadRequest.builder().putObjectRequest(req.build());
				if ( progressListener != null ) {
					up.addTransferListener(s3ProgressListener);
				}
				up.requestBody(fromInputStream(in, resource.contentLength(), executorService));

				Upload upload = mgr.upload(up.build());
				upload.completionFuture().join();
			} catch ( CompletionException ce ) {
				if ( ce.getCause() instanceof AwsServiceException e ) {
					log.warn("AWS error: {}; HTTP code {}; AWS code {}; request ID {}", e.getMessage(),
							e.statusCode(), e.awsErrorDetails().errorCode(), e.requestId());
					throw new RemoteServiceException("Error putting S3 object at " + key, e);
				} else if ( ce.getCause() instanceof SdkClientException e ) {
					log.debug("Error communicating with AWS: {}", e.getMessage());
					throw new IOException("Error communicating with AWS", e);
				} else {
					throw new RemoteServiceException("Error putting S3 object at " + key, ce);
				}
			}
		}
	}

	private String getDestinationPath(S3DestinationProperties props, Map<String, ?> runtimeProperties,
			int resourceIndex) {
		S3Uri uri = props.getUri();
		String key = uri.key().orElse("") + "/"
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

	private S3TransferManager getTransferManager(S3DestinationProperties props) {
		S3AsyncClient client = getClient(props);
		return S3TransferManager.builder().s3Client(client).build();
	}

	private S3AsyncClient getClient(S3DestinationProperties props) {
		S3Uri uri = props.getUri();
		S3AsyncClientBuilder builder = S3AsyncClient.builder();
		if ( uri.region().isPresent() ) {
			builder.region(uri.region().get());
		}
		String accessKey = props.getAccessKey();
		String secretKey = props.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder = builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

}
