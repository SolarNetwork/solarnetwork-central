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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.WebAsyncTask;
import net.solarnetwork.central.cloud.biz.VirtualMachineBiz;
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.domain.Response;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * REST API for the SolarNode Image Maker app.
 * 
 * @author matt
 * @version 1.0
 */
@RequestMapping(value = { "/sec/nim", "/v1/sec/user/nim" })
public class SolarNodeImageMakerController extends WebServiceControllerSupport {

	private final UserBiz userBiz;
	private final OptionalService<VirtualMachineBiz> virtualMachineBiz;
	private String virtualMachineName = "NIM";
	private String nimBaseUrl = "https://apps.solarnetwork.net/solarnode-image-maker";
	private int timeoutSeconds = (int) TimeUnit.MINUTES.toSeconds(5L);

	@Autowired
	public SolarNodeImageMakerController(UserBiz userBiz,
			@Qualifier("appsVirtualMachineBiz") OptionalService<VirtualMachineBiz> virtualMachineBiz) {
		super();
		this.userBiz = userBiz;
		this.virtualMachineBiz = virtualMachineBiz;
	}

	private static String uriHost(URI uri) {
		String host = uri.getHost();
		if ( uri.getPort() != 80 && uri.getPort() != 443 ) {
			host += ":" + uri.getPort();
		}
		return host;
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
	public WebAsyncTask<Response<String>> getAuthorizationKey(HttpServletRequest req)
			throws URISyntaxException {
		URI reqUri = new URI(req.getRequestURL().toString());
		String snHost = uriHost(reqUri);
		SecurityToken token = SecurityUtils.getCurrentToken();

		Callable<Response<String>> task = new Callable<Response<String>>() {

			@Override
			public Response<String> call() throws Exception {
				String key = null;
				if ( virtualMachineName != null ) {
					VirtualMachineBiz vmBiz = virtualMachineBiz.service();
					if ( vmBiz != null ) {
						VirtualMachine vm = vmBiz.virtualMachineForName(virtualMachineName);
						if ( vm != null ) {
							VirtualMachineState state = vmBiz
									.stateForVirtualMachines(Collections.singleton(vm.getId())).values()
									.stream().findFirst().orElse(VirtualMachineState.Unknown);
							if ( state == VirtualMachineState.Stopped
									|| state == VirtualMachineState.Stopping ) {
								// boot it up
								do {
									vmBiz.changeVirtualMachinesState(Collections.singleton(vm.getId()),
											VirtualMachineState.Running);

									try {
										Thread.sleep(TimeUnit.SECONDS.toMillis(15L));
									} catch ( InterruptedException e ) {
										// give up on interruption
										break;
									}

									state = vmBiz
											.stateForVirtualMachines(Collections.singleton(vm.getId()))
											.values().stream().findFirst()
											.orElse(VirtualMachineState.Unknown);

								} while ( state == VirtualMachineState.Starting );
							}
						}
					}
				}

				DateTime now = new DateTime();
				AuthorizationV2Builder authBuilder = userBiz
						.createAuthorizationV2Builder(token.getUserId(), token.getToken(), now);
				authBuilder.path("/solaruser/api/v1/sec/whoami").host(snHost);

				RestTemplate client = new RestTemplate();
				URI url = new URI(nimBaseUrl + "/api/v1/images/authorize");

				HttpHeaders nimReqHeaders = new HttpHeaders();
				nimReqHeaders.add("X-SN-Date", AuthorizationV2Builder.httpDate(now.toDate()));
				nimReqHeaders.add("X-SN-PreSignedAuthorization", authBuilder.build());
				RequestEntity<Object> nimReq = new RequestEntity<>(null, nimReqHeaders, HttpMethod.POST,
						url);

				@SuppressWarnings("rawtypes")
				ResponseEntity<Map> res = client.exchange(nimReq, Map.class);
				if ( !res.getStatusCode().is2xxSuccessful() && res.hasBody()
						&& res.getBody().get("data") instanceof String ) {
					String msg = (res.hasBody() && res.getBody().get("message") instanceof String
							? res.getBody().get("message").toString()
							: "NIM server returned error status code");
					return new Response<String>(false, res.getStatusCode().toString(), msg, null);
				}
				key = res.getBody().get("data").toString();
				return Response.response(key);
			}
		};

		return new WebAsyncTask<>(TimeUnit.SECONDS.toMillis(timeoutSeconds), task);
	}

	/**
	 * Set the name of the virtual machine to control for the SolarNode Image
	 * Maker.
	 * 
	 * @param virtualMachineName
	 *        the virtual machine name; defaults to {@literal NIM}
	 */
	public void setVirtualMachineName(String virtualMachineName) {
		this.virtualMachineName = virtualMachineName;
	}

	/**
	 * The base URL to the NIM REST API.
	 * 
	 * @param nimBaseUrl
	 *        the base URL; defaults to
	 *        {@literal https://apps.solarnetwork.net/solarnode-image-maker}
	 */
	public void setNimBaseUrl(String nimBaseUrl) {
		this.nimBaseUrl = nimBaseUrl;
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
