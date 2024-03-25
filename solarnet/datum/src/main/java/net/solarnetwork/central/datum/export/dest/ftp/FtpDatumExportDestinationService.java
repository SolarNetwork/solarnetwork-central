/* ==================================================================
 * FtpDatumExportDestinationService.java - 25/03/2024 2:08:35 pm
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

package net.solarnetwork.central.datum.export.dest.ftp;

import static net.solarnetwork.util.StringUtils.expandTemplateString;
import static org.apache.commons.net.ftp.FTPReply.isPositiveCompletion;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.util.FileCopyUtils;
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
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ClassUtils;

/**
 * FTP implementation of {@link DatumExportDestinationService}.
 *
 * @author matt
 * @version 1.0
 */
public class FtpDatumExportDestinationService extends BaseDatumExportDestinationService {

	/**
	 * The FTP {@code PROT} command argument for control and data protection
	 * level.
	 */
	public static final String FTP_CONTROL_AND_DATA_PROTECTION_LEVEL = "P";

	/**
	 * Constructor.
	 */
	public FtpDatumExportDestinationService() {
		super("net.solarnetwork.central.datum.export.dest.ftp.FtpDatumExportDestinationService");
	}

	@Override
	public String getDisplayName() {
		return "FTP Datum Export Destination Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(3);
		result.add(new BasicTextFieldSettingSpecifier("url", ""));
		result.add(new BasicTextFieldSettingSpecifier("username", ""));
		result.add(new BasicTextFieldSettingSpecifier("password", "", true));
		result.add(new BasicToggleSettingSpecifier("implicitTls", Boolean.FALSE));
		result.add(new BasicToggleSettingSpecifier("dataTls", FtpDestinationProperties.DEFAUT_DATA_TLS));
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
		FtpDestinationProperties props = new FtpDestinationProperties();
		ClassUtils.setBeanProperties(props, destConfig.getServiceProperties(), true);
		if ( !props.isValid() ) {
			throw new IOException("Service configuration is not valid.");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final String destUrl = expandTemplateString(props.getUrl(),
				new UrlEncodingOnAccessMap<String>((Map) runtimeProperties));

		final URI uri;
		try {
			uri = new URI(destUrl);
		} catch ( URISyntaxException e ) {
			throw new IOException("Invalid URL [" + destUrl + "]: " + e.toString(), e);
		}

		FTPClient ftp = ("ftps".equalsIgnoreCase(uri.getScheme()) ? new FTPSClient(props.isImplicitTls())
				: new FTPClient());
		int port = (uri.getPort() > 0 ? uri.getPort()
				: "ftps".equalsIgnoreCase(uri.getScheme()) && props.isImplicitTls()
						? FTPSClient.DEFAULT_FTPS_PORT
						: FTPClient.DEFAULT_PORT);
		ftp.connect(uri.getHost(), port);
		ftp.enterLocalPassiveMode();
		if ( props.isDataTls() && ftp instanceof FTPSClient ftps ) {
			ftps.execPBSZ(0);
			ftps.execPROT(FTP_CONTROL_AND_DATA_PROTECTION_LEVEL);
		}
		try {
			log.debug("Export to FTP URL [{}]", destUrl);
			if ( props.hasCredentials() ) {
				int reply = ftp.user(props.getUsername());
				if ( reply == FTPReply.NEED_PASSWORD ) {
					if ( !isPositiveCompletion(ftp.pass(props.getPassword())) ) {
						throw new IOException("Password rejected by server.");
					}
				} else if ( !isPositiveCompletion(reply) ) {
					throw new IOException("Username rejected by server.");
				}
			}
			if ( !ftp.setFileType(FTPClient.BINARY_FILE_TYPE) ) {
				throw new IOException("Unable to switch to binary file type.");
			}
			var inputs = new ArrayList<InputStream>();
			for ( DatumExportResource r : resources ) {
				inputs.add(r.getInputStream());
			}
			String destPath = uri.getPath();
			if ( destPath.startsWith("/") ) {
				destPath = destPath.substring(1);
			}
			try (OutputStream out = ftp.storeFileStream(destPath)) {
				int replyCode = ftp.getReplyCode();
				if ( !(replyCode == FTPReply.FILE_STATUS_OK
						|| FTPReply.isPositiveIntermediate(ftp.getReplyCode())) ) {
					throw new IOException("Unable to upload to [%s]: %s".formatted(destPath,
							ftp.getReplyString().trim()));
				}
				FileCopyUtils.copy(new ConcatenatingInputStream(inputs), out);
				try {
					if ( !ftp.completePendingCommand() ) {
						throw new IOException("Upload to [%s] failed.".formatted(destPath));
					}
				} catch ( FTPConnectionClosedException e ) {
					log.warn("Upload to {} connection closed; ignoring", uri);
				}
			}
		} finally {
			ftp.disconnect();
		}
	}

}
