/* ==================================================================
 * HttpDatumExportDestinationService.java - 20/03/2024 7:41:08 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.dest.http;

import static net.solarnetwork.util.StringUtils.expandTemplateString;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DestinationConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportDestinationService;
import net.solarnetwork.central.web.support.UrlEncodingOnAccessMap;
import net.solarnetwork.io.ConcatenatingInputStream;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * HTTP implementation of {@link DatumExportDestinationService}.
 *
 * @author matt
 * @version 1.0
 */
public class HttpDatumExportDestinationService extends BaseDatumExportDestinationService {

	private final HttpClient client;

	/**
	 * Constructor.
	 *
	 * @param client
	 *        the HTTP client to use
	 */
	public HttpDatumExportDestinationService(HttpClient client) {
		super("net.solarnetwork.central.datum.export.dest.http.HttpDatumExportDestinationService");
		this.client = ObjectUtils.requireNonNullArgument(client, "client");
	}

	@Override
	public String getDisplayName() {
		return "HTTP Datum Export Destination Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(6);
		result.add(new BasicTextFieldSettingSpecifier("method",
				HttpDestinationProperties.DEFAULT_METHOD.toString()));
		result.add(new BasicTextFieldSettingSpecifier("url", ""));
		result.add(new BasicTextFieldSettingSpecifier("username", ""));
		result.add(new BasicTextFieldSettingSpecifier("password", "", true));
		result.add(new BasicTextFieldSettingSpecifier("multipartFilenameTemplate", ""));
		result.add(new BasicTextAreaSettingSpecifier("headersValue", "", false));
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
		HttpDestinationProperties props = new HttpDestinationProperties();
		ClassUtils.setBeanProperties(props, destConfig.getServiceProperties(), true);
		if ( !props.isValid() ) {
			throw new IOException("Service configuration is not valid.");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final String destUrl = expandTemplateString(props.getUrl(),
				new UrlEncodingOnAccessMap<String>((Map) runtimeProperties));

		ClassicRequestBuilder req = ClassicRequestBuilder.create(props.method().toString())
				.setUri(destUrl);

		if ( props.hasCredentials() ) {
			// assuming HTTP Basic here
			req.setHeader(HttpHeaders.AUTHORIZATION,
					StandardAuthScheme.BASIC + " "
							+ Base64.getEncoder()
									.encodeToString((props.getUsername() + ":" + props.getPassword())
											.getBytes(StandardCharsets.UTF_8)));
		}

		if ( props.getHeaders() != null ) {
			for ( Entry<String, String> header : props.getHeaders().entrySet() ) {
				req.addHeader(header.getKey(),
						expandTemplateString(header.getValue(), runtimeProperties));
			}
		}

		if ( props.isMultipart() ) {
			MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
			int idx = 0;
			for ( DatumExportResource r : resources ) {
				String fileName = expandTemplateString(props.getMultipartFilenameTemplate(),
						runtimeProperties);
				multipart.addBinaryBody("%s-%d".formatted(fileName, ++idx), r.getInputStream(),
						ContentType.parse(r.getContentType()), fileName);
			}
			req.setEntity(multipart.build());
		} else {
			var inputs = new ArrayList<InputStream>(4);
			String contentType = null;
			long contentLength = 0;
			for ( DatumExportResource r : resources ) {
				contentType = r.getContentType();
				try {
					contentLength += r.contentLength();
				} catch ( IOException e ) {
					// ignore
				}
				inputs.add(r.getInputStream());
			}
			ConcatenatingInputStream cat = new ConcatenatingInputStream(inputs);
			req.setEntity(new InputStreamEntity(cat, contentLength, ContentType.parse(contentType)));
		}

		try {
			log.debug("Export to HTTP {} URL [{}] as {}", req.getMethod(), destUrl,
					req.getEntity().getContentType());
			client.execute(req.build(), (res) -> {
				if ( !(res.getCode() >= HttpStatus.SC_OK
						&& res.getCode() < HttpStatus.SC_REDIRECTION) ) {
					throw new HttpResponseException(res.getCode(), res.getReasonPhrase());
				}
				return null;
			});
		} catch ( HttpResponseException e ) {
			throw new RemoteServiceException("Error exporting to HTTP %s URL [%s]: %s"
					.formatted(req.getMethod(), destUrl, e.getMessage()));
		}
	}

}
