/* ==================================================================
 * CsvDatumExportOutputFormatService.java - 11/04/2018 12:08:19 PM
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

import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;

/**
 * Comma-separated-values implementation of
 * {@link DatumExportOutputFormatService}
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class CsvDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	public CsvDatumExportOutputFormatService() {
		super("net.solarnetwork.central.datum.biz.impl.CsvDatumExportOutputFormatService");
	}

	@Override
	public String getDisplayName() {
		return "CSV Output Format";
	}

	@Override
	public String getExportFilenameExtension() {
		return "csv";
	}

	@Override
	public String getExportContentType() {
		return "text/csv;charset=UTF-8";
	}

}
