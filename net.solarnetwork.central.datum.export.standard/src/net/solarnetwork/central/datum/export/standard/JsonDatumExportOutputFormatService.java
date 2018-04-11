/* ==================================================================
 * JsonDatumExportOutputFormatService.java - 12/04/2018 6:53:40 AM
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

package net.solarnetwork.central.datum.export.standard;

import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.support.BaseDatumExportOutputFormatService;

/**
 * JSON implementation of {@link DatumExportOutputFormatService}
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class JsonDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	public JsonDatumExportOutputFormatService() {
		super("net.solarnetwork.central.datum.support.JsonDatumExportOutputFormatService");
	}

	@Override
	public String getDisplayName() {
		return "JSON Output Format";
	}

	@Override
	public String getExportFilenameExtension() {
		return "json";
	}

	@Override
	public String getExportContentType() {
		return "application/json;charset=UTF-8";
	}

}
