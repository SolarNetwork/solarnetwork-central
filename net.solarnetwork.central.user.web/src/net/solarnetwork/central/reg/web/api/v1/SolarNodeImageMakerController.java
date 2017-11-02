/* ==================================================================
 * SolarNodeImageMakerController.java - 31/10/2017 9:40:37 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.nim.biz.SolarNodeImageMakerBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;

/**
 * REST API for the SolarNode Image Maker app.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
@RequestMapping(value = { "/sec/nim", "/v1/sec/user/nim" })
public class SolarNodeImageMakerController extends WebServiceControllerSupport {

	private final OptionalService<SolarNodeImageMakerBiz> nimBiz;
	private int timeoutSeconds = (int) TimeUnit.MINUTES.toSeconds(5L);

	@Autowired
	public SolarNodeImageMakerController(OptionalService<SolarNodeImageMakerBiz> nimBiz) {
		super();
		this.nimBiz = nimBiz;
	}

	/**
	 * Asynchronously handle getting an authentication key for the SolarNode
	 * Image Maker.
	 * 
	 * <p>
	 * This operation may need to start up the virtual machine hosting the NIM
	 * app, and so handles the request asynchronously.
	 * </p>
	 * 
	 * @return a {@code Callable} for the authorization key to use with NIM
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/authorize")
	@ResponseBody
	public WebAsyncTask<Response<String>> getAuthorizationKey(HttpServletRequest req) {
		String reqUrl = req.getRequestURL().toString();
		SecurityToken token = SecurityUtils.getCurrentToken();

		Callable<Response<String>> task = new Callable<Response<String>>() {

			@Override
			public Response<String> call() throws Exception {
				SolarNodeImageMakerBiz biz = nimBiz.service();
				if ( biz == null ) {
					return new Response<String>(false, null, "NIM service not available", null);
				}
				String key = biz.authorizeToken(token, reqUrl);
				return Response.response(key);
			}
		};

		return new WebAsyncTask<>(TimeUnit.SECONDS.toMillis(timeoutSeconds), task);
	}

	/**
	 * Set the number of seconds to allow asynchronous handler to take.
	 * 
	 * @param timeoutSeconds
	 *        the timeout, in seconds; defaults to 5 minutes
	 */
	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

}
