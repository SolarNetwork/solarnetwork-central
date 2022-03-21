/* ==================================================================
 * BaseDatumExportOutputFormatService.java - 11/04/2018 12:23:27 PM
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

package net.solarnetwork.central.datum.export.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Base class to support implementations of
 * {@link DatumExportOutputFormatService}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public abstract class BaseDatumExportOutputFormatService
		extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements DatumExportOutputFormatService {

	private File temporaryDir;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the {@link Identity#getId()} to use
	 */
	public BaseDatumExportOutputFormatService(String id) {
		super(id);
		setTemporaryPath(null);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	/**
	 * Create a temporary file.
	 * 
	 * <p>
	 * This will create a temporary file within the configured
	 * {@link #getTemporaryDir()} directory, named after this instance's class
	 * name and file extension.
	 * </p>
	 * 
	 * @param config
	 *        the output configuration
	 * @return the file
	 * @throws IOException
	 *         if an IO error occurs
	 */
	protected File createTemporaryResource(OutputConfiguration config) throws IOException {
		String ext = getExportFilenameExtension();
		String compExt = (config.getCompressionType() != null
				? config.getCompressionType().getFilenameExtension()
				: "");
		if ( !compExt.isEmpty() ) {
			ext += "." + compExt;
		}
		File result = File.createTempFile(getClass().getSimpleName() + "-", "." + ext,
				getTemporaryDir());
		log.info("Created temporary file [{}] for config {}", result, config);
		return result;
	}

	/**
	 * Get the appropriate content type to use based on the output
	 * configuration.
	 * 
	 * @param config
	 *        the output configuration
	 * @return the content type
	 */
	protected String getContentType(OutputConfiguration config) {
		if ( config != null && config.getCompressionType() != null
				&& config.getCompressionType() != OutputCompressionType.None ) {
			return config.getCompressionType().getContentType();
		}
		return getExportContentType();
	}

	/**
	 * Get the configured temporary directory.
	 * 
	 * @return the temporary directory; defaults to the system property
	 *         {@literal java.io.tmpdir}
	 */
	public File getTemporaryDir() {
		return temporaryDir;
	}

	/**
	 * Set the temporary directory.
	 * 
	 * @param temporaryDir
	 *        the temporary directory to set
	 * @throws IllegalArgumentException
	 *         if {@code temporaryDir} is {@literal null}
	 */
	public void setTemporaryDir(File temporaryDir) {
		this.temporaryDir = requireNonNullArgument(temporaryDir, "temporaryDir");
	}

	/**
	 * Set the temporary directory as a path string.
	 * 
	 * @param path
	 *        the path to use, or {@literal null} or an empty string to use the
	 *        system property {@literal java.io.tmpdir}
	 */
	public void setTemporaryPath(String path) {
		if ( path == null || path.isEmpty() ) {
			path = System.getProperty("java.io.tmpdir");
		}
		setTemporaryDir(new File(path));
	}

}
