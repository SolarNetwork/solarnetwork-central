/* ==================================================================
 * DatumImportController.java - 7/11/2018 6:57:54 AM
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.web.domain.Response.response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;
import net.solarnetwork.web.support.MultipartFileResource;

/**
 * Web service API for datum import management.
 * 
 * @author matt
 * @version 1.0
 * @since 1.33
 */
@RestController("v1DatumExportController")
@RequestMapping(value = { "/sec/import", "/v1/sec/user/import" })
public class DatumImportController extends WebServiceControllerSupport {

	private final OptionalService<DatumImportBiz> importBiz;

	/**
	 * Constructor.
	 * 
	 * @param importBiz
	 *        the import biz to use
	 */
	public DatumImportController(OptionalService<DatumImportBiz> importBiz) {
		super();
		this.importBiz = importBiz;
	}

	/**
	 * Get a list of all available input format services.
	 * 
	 * @param locale
	 *        the locale to use
	 * @return the services
	 */
	@ResponseBody
	@RequestMapping(value = "/services/input", method = RequestMethod.GET)
	public Response<List<LocalizedServiceInfo>> availableInputFormatServices(Locale locale) {
		final DatumImportBiz biz = importBiz.service();
		List<LocalizedServiceInfo> result = null;
		if ( biz != null ) {
			Iterable<DatumImportInputFormatService> services = biz.availableInputFormatServices();
			result = new ArrayList<>();
			for ( DatumImportInputFormatService s : services ) {
				result.add(s.getLocalizedServiceInfo(locale));
			}
		}
		return response(result);
	}

	/**
	 * Upload a datum import configuration with associated data.
	 * 
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 * 
	 * @param config
	 *        the import configuration
	 * @param data
	 *        the data to import
	 * @return a status entity
	 */
	@ResponseBody
	@RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Response<DatumImportStatus> startImport(@RequestPart("config") BasicConfiguration config,
			@RequestPart("data") MultipartFile data) {
		final DatumImportBiz biz = importBiz.service();
		DatumImportStatus result = null;
		if ( biz != null ) {
			Long userId = SecurityUtils.getCurrentActorUserId();
			BasicDatumImportResource resource = new BasicDatumImportResource(
					new MultipartFileResource(data), data.getContentType());
			BasicDatumImportRequest request = new BasicDatumImportRequest(config, userId);
			result = biz.submitDatumImportRequest(request, resource);
		}
		return response(result);
	}
}
