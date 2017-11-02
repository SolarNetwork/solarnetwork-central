/* ==================================================================
 * CloudSolarNodeImageMakerBiz.java - 1/11/2017 6:43:25 AM
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

package net.solarnetwork.central.user.nim.cloud;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.biz.MaintenanceSubscriber;
import net.solarnetwork.central.cloud.biz.VirtualMachineBiz;
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.nim.biz.SolarNodeImageMakerBiz;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * Cloud based implementation of {@link SolarNodeImageMakerBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class CloudSolarNodeImageMakerBiz implements SolarNodeImageMakerBiz, MaintenanceSubscriber {

	private final UserBiz userBiz;
	private final OptionalService<VirtualMachineBiz> virtualMachineBiz;
	private String uid;
	private String groupUid;
	private String virtualMachineName = "NIM";
	private String nimBaseUrl = "https://apps.solarnetwork.net/solarnode-image-maker";
	private int virtualMachineTimeoutSeconds = (int) TimeUnit.MINUTES.toSeconds(4);

	private final RestOperations client = new RestTemplate();
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Pattern PING_SUCCESS_REGEX = Pattern.compile("\"allGood\"\\s*:\\s*true");
	private static final Pattern PING_ACTIVE_SESSION_REGEX = Pattern
			.compile("\"activeSessionCount\"\\s*:\\s*\"?(\\d+)");

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        the UserBiz to use
	 * @param virtualMachineBiz
	 *        the virtual machine API to manage the NIM VM with
	 */
	public CloudSolarNodeImageMakerBiz(UserBiz userBiz,
			OptionalService<VirtualMachineBiz> virtualMachineBiz) {
		super();
		this.userBiz = userBiz;
		this.virtualMachineBiz = virtualMachineBiz;
		this.uid = UUID.randomUUID().toString();
	}

	@Override
	public String authorizeToken(SecurityToken token, String solarNetworkBaseUrl) {
		URI reqUri;
		try {
			reqUri = new URI(solarNetworkBaseUrl);
		} catch ( URISyntaxException e ) {
			throw new IllegalArgumentException("Malformed SolarNetwork URL", e);
		}

		// verify NIM VM running
		checkNimAvailability();

		// get authorized key from NIM
		String key = getNimAuthorizationKey(reqUri, token);
		return key;
	}

	/**
	 * Perform maintenance tasks on the configured virtual machine.
	 * 
	 * <p>
	 * If a virtual machine is configured, this method will try to shut it down
	 * if there are no active sessions in NIM. This method is designed to be
	 * called periodically from a scheduled job.
	 * </p>
	 * 
	 */
	@Override
	public void performServiceMaintenance(Map<String, ?> parameters) {
		int activeSessionCount = nimActiveSessionCount();
		log.info("NIM at {} has {} active sessions", nimBaseUrl, activeSessionCount);
		if ( activeSessionCount == 0 ) {
			shutdownNim();
		}
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public String getGroupUid() {
		return groupUid;
	}

	@Override
	public String getDisplayName() {
		return "Cloud SolarNodeImageMaker Integration";
	}

	private String getNimAuthorizationKey(URI reqUri, SecurityToken token) {
		URI url;
		try {
			url = new URI(nimBaseUrl + "/api/v1/images/authorize");
		} catch ( URISyntaxException e ) {
			throw new RuntimeException("Malformed NIM base URL: " + nimBaseUrl);
		}

		DateTime now = new DateTime();
		HttpHeaders nimReqHeaders = new HttpHeaders();
		nimReqHeaders.add("X-SN-Date", AuthorizationV2Builder.httpDate(now.toDate()));
		nimReqHeaders.add("X-SN-PreSignedAuthorization", preSignNimAuthorizeUrl(reqUri, token, now));
		RequestEntity<Object> nimReq = new RequestEntity<>(null, nimReqHeaders, HttpMethod.POST, url);

		try {
			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> res = client.exchange(nimReq, Map.class);
			if ( !res.getStatusCode().is2xxSuccessful() && res.hasBody()
					&& res.getBody().get("data") instanceof String ) {
				String msg = (res.hasBody() && res.getBody().get("message") instanceof String
						? res.getBody().get("message").toString()
						: "NIM server returned error status code " + res.getStatusCode());
				throw new RemoteServiceException(msg);
			}
			return res.getBody().get("data").toString();
		} catch ( HttpStatusCodeException e ) {
			final int statusCode = e.getStatusCode().value();
			if ( statusCode == 401 || statusCode == 403 ) {
				throw new AuthorizationException(token.getToken(), Reason.ACCESS_DENIED);
			}
			throw new RemoteServiceException(e);
		} catch ( RestClientException e ) {
			throw new RemoteServiceException(e);
		}
	}

	private static String uriHost(URI uri) {
		String host = uri.getHost();
		if ( uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443 ) {
			host += ":" + uri.getPort();
		}
		return host;
	}

	private String preSignNimAuthorizeUrl(URI reqUri, SecurityToken token, DateTime now) {
		String snHost = uriHost(reqUri);
		AuthorizationV2Builder authBuilder = userBiz.createAuthorizationV2Builder(token.getUserId(),
				token.getToken(), now);
		authBuilder.path("/solaruser/api/v1/sec/whoami").host(snHost);
		if ( log.isDebugEnabled() ) {
			log.debug("Pre-signing /whoami request canonical request data:\n{}\n\nSigning key: {}",
					authBuilder.buildCanonicalRequestData(), authBuilder.signingKeyHex());
		}
		log.info("Pre-signing /whoami request to {} @ {} on behalf of user {} token {}", snHost, now,
				token.getUserId(), token.getToken());
		return authBuilder.build();
	}

	private void checkNimAvailability() {
		if ( virtualMachineName == null ) {
			return;
		}
		VirtualMachineBiz vmBiz = virtualMachineBiz.service();
		if ( vmBiz == null ) {
			return;
		}
		VirtualMachine vm = vmBiz.virtualMachineForName(virtualMachineName);
		if ( vm == null ) {
			return;
		}
		VirtualMachineState state = vmBiz.stateForVirtualMachines(Collections.singleton(vm.getId()))
				.values().stream().findFirst().orElse(VirtualMachineState.Unknown);
		if ( !(state == VirtualMachineState.Stopped || state == VirtualMachineState.Stopping) ) {
			return;
		}
		// VM is stopped; boot it up now
		final long bootExpiration = System.currentTimeMillis()
				+ TimeUnit.SECONDS.toMillis(virtualMachineTimeoutSeconds);
		log.info("Starting NIM VM {} ({})", virtualMachineName, vm.getId());
		vmBiz.changeVirtualMachinesState(Collections.singleton(vm.getId()), VirtualMachineState.Running);
		do {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(15L));
			} catch ( InterruptedException e ) {
				// give up on interruption
				break;
			}

			state = vmBiz.stateForVirtualMachines(Collections.singleton(vm.getId())).values().stream()
					.findFirst().orElse(VirtualMachineState.Unknown);
			log.info("NIM VM {} state is {}", vm.getId(), state);

			if ( state == VirtualMachineState.Running ) {
				// verify /ping
				if ( !nimPingSuccess() ) {
					state = VirtualMachineState.Starting;
				}
			}

		} while ( state == VirtualMachineState.Starting && System.currentTimeMillis() < bootExpiration );
	}

	private void shutdownNim() {
		if ( virtualMachineName == null ) {
			return;
		}
		VirtualMachineBiz vmBiz = virtualMachineBiz.service();
		if ( vmBiz == null ) {
			return;
		}
		VirtualMachine vm = vmBiz.virtualMachineForName(virtualMachineName);
		if ( vm == null ) {
			return;
		}
		VirtualMachineState state = vmBiz.stateForVirtualMachines(Collections.singleton(vm.getId()))
				.values().stream().findFirst().orElse(VirtualMachineState.Unknown);
		if ( state != VirtualMachineState.Running ) {
			return;
		}
		// VM is running; stop it now
		log.info("Stopping NIM VM {} ({})", virtualMachineName, vm.getId());
		vmBiz.changeVirtualMachinesState(Collections.singleton(vm.getId()), VirtualMachineState.Stopped);

		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(5L));
		} catch ( InterruptedException e ) {
			// give up on interruption
		}

		state = vmBiz.stateForVirtualMachines(Collections.singleton(vm.getId())).values().stream()
				.findFirst().orElse(VirtualMachineState.Unknown);
		log.info("NIM VM {} state is {}", vm.getId(), state);
	}

	private boolean nimPingSuccess() {
		String pingUrl = nimBaseUrl + "/api/v1/ping";
		try {
			String data = client.getForObject(pingUrl, String.class);
			Matcher m = PING_SUCCESS_REGEX.matcher(data);
			if ( m.find() ) {
				log.info("NIM pinged succesfully at {}", pingUrl);
				return true;
			}
			log.warn("NIM ping at {} did not return success response: {}", pingUrl, data);
		} catch ( RestClientException e ) {
			log.warn("NIM ping request to {} did not succeed: {}", pingUrl, e.getMessage());
		}
		return false;
	}

	private int nimActiveSessionCount() {
		String pingUrl = nimBaseUrl + "/api/v1/ping";
		try {
			String data = client.getForObject(pingUrl, String.class);
			Matcher m = PING_ACTIVE_SESSION_REGEX.matcher(data);
			if ( m.find() ) {
				return Integer.parseInt(m.group(1));
			}
			log.debug("NIM ping at {} did not return active session count in response: {}", pingUrl,
					data);
		} catch ( RestClientException e ) {
			log.debug("NIM ping request to {} did not succeed: {}", pingUrl, e.getMessage());
		}
		return -1;
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
	 * The maximum number of seconds to wait for the NIM virtual machine to
	 * boot.
	 * 
	 * @param virtualMachineTimeoutSeconds
	 *        the maximum seconds; defaults to 4 minutes
	 */
	public void setVirtualMachineTimeoutSeconds(int virtualMachineTimeoutSeconds) {
		this.virtualMachineTimeoutSeconds = virtualMachineTimeoutSeconds;
	}

	/**
	 * Set the unique ID for this service.
	 * 
	 * @param uid
	 *        the UID
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Set the group ID for this service.
	 * 
	 * @param groupUid
	 *        the group ID
	 */
	public void setGroupUid(String groupUid) {
		this.groupUid = groupUid;
	}

}
