/* ==================================================================
 * BaseDatumImportInputFormatService.java - 7/11/2018 1:12:14 PM
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

package net.solarnetwork.central.datum.imp.support;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Base class to support implementations of
 * {@link DatumImportInputFormatService}.
 *
 * @author matt
 * @version 2.0
 */
public abstract class BaseDatumImportInputFormatService extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider implements DatumImportInputFormatService {

	private @Nullable String inputFilenameExtension;
	private @Nullable String inputContentType;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the service identifier
	 */
	public BaseDatumImportInputFormatService(String id) {
		super(id);
	}

	@Override
	public final @Nullable String getInputFilenameExtension() {
		return inputFilenameExtension;
	}

	public final void setInputFilenameExtension(@Nullable String inputFilenameExtension) {
		this.inputFilenameExtension = inputFilenameExtension;
	}

	@Override
	public final @Nullable String getInputContentType() {
		return inputContentType;
	}

	public final void setInputContentType(@Nullable String inputContentType) {
		this.inputContentType = inputContentType;
	}

}
