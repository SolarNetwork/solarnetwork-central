/* ==================================================================
 * UserInstructionInputController.java - 30/03/2024 7:38:00 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;
import net.solarnetwork.central.inin.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;
import net.solarnetwork.central.user.inin.domain.CredentialConfigurationInput;
import net.solarnetwork.central.user.inin.domain.EndpointAuthConfigurationInput;
import net.solarnetwork.central.user.inin.domain.EndpointConfigurationInput;
import net.solarnetwork.central.user.inin.domain.TransformConfigurationInput.RequestTransformConfigurationInput;
import net.solarnetwork.central.user.inin.domain.TransformConfigurationInput.ResponseTransformConfigurationInput;
import net.solarnetwork.central.user.inin.domain.TransformInstructionResults;
import net.solarnetwork.central.user.inin.domain.TransformOutput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.MaxUploadSizeInputStream;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for Instruction Input (ININ) management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT)
@GlobalExceptionRestController
@RestController("v1InstructionInputController")
@RequestMapping(value = { "/api/v1/sec/user/inin", "/u/sec/inin" })
public class UserInstructionInputController {

	private final UserInstructionInputBiz userInstructionInputBiz;
	private final long maxInstructionInputLength;

	/**
	 * Constructor.
	 *
	 * @param userInstructionInputBiz
	 *        the service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserInstructionInputController(UserInstructionInputBiz userInstructionInputBiz,
			@Value("${app.inin.max-input-length}") long maxInstructionInputLength) {
		super();
		this.userInstructionInputBiz = requireNonNullArgument(userInstructionInputBiz,
				"userInstructionInputBiz");
		this.maxInstructionInputLength = maxInstructionInputLength;
	}

	/**
	 * List the available request transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/request-transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableRequestTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = userInstructionInputBiz
				.availableRequestTransformServices(locale);
		return success(result);
	}

	/**
	 * List the available response transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/response-transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableResponseTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = userInstructionInputBiz
				.availableResponseTransformServices(locale);
		return success(result);
	}

	@RequestMapping(value = "/credentials", method = RequestMethod.GET)
	public Result<FilterResults<CredentialConfiguration, UserLongCompositePK>> listCredentialConfigurations(
			BasicFilter filter) {
		FilterResults<CredentialConfiguration, UserLongCompositePK> result = userInstructionInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, CredentialConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/credentials", method = RequestMethod.POST)
	public ResponseEntity<Result<CredentialConfiguration>> createCredentialConfiguration(
			@Valid @RequestBody CredentialConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		CredentialConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserInstructionInputController.class)
				.getCredentialConfiguration(result.getCredentialId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.GET)
	public Result<CredentialConfiguration> getCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		return success(userInstructionInputBiz.configurationForId(id, CredentialConfiguration.class));
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.PUT)
	public Result<CredentialConfiguration> updateCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId,
			@Valid @RequestBody CredentialConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		CredentialConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/credentials/{credentialId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId, @PathVariable("enabled") boolean enabled) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		userInstructionInputBiz.enableConfiguration(id, enabled, CredentialConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCredentialConfiguration(@PathVariable("credentialId") Long credentialId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		userInstructionInputBiz.deleteConfiguration(id, CredentialConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/request-transforms", method = RequestMethod.GET)
	public Result<FilterResults<RequestTransformConfiguration, UserLongCompositePK>> listRequestTransformConfigurations(
			BasicFilter filter) {
		FilterResults<RequestTransformConfiguration, UserLongCompositePK> result = userInstructionInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter,
						RequestTransformConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/request-transforms", method = RequestMethod.POST)
	public ResponseEntity<Result<RequestTransformConfiguration>> createRequestTransformConfiguration(
			@Valid @RequestBody RequestTransformConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		RequestTransformConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserInstructionInputController.class)
				.getRequestTransformConfiguration(result.getTransformId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/request-transforms/{transformId}", method = RequestMethod.GET)
	public Result<RequestTransformConfiguration> getRequestTransformConfiguration(
			@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		return success(
				userInstructionInputBiz.configurationForId(id, RequestTransformConfiguration.class));
	}

	@RequestMapping(value = "/request-transforms/{transformId}", method = RequestMethod.PUT)
	public Result<RequestTransformConfiguration> updateRequestTransformConfiguration(
			@PathVariable("transformId") Long transformId,
			@Valid @RequestBody RequestTransformConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		RequestTransformConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/request-transforms/{transformId}", method = RequestMethod.DELETE)
	public Result<Void> deleteRequestTransformConfiguration(
			@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		userInstructionInputBiz.deleteConfiguration(id, RequestTransformConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/response-transforms", method = RequestMethod.GET)
	public Result<FilterResults<ResponseTransformConfiguration, UserLongCompositePK>> listResponseTransformConfigurations(
			BasicFilter filter) {
		FilterResults<ResponseTransformConfiguration, UserLongCompositePK> result = userInstructionInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter,
						ResponseTransformConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/response-transforms", method = RequestMethod.POST)
	public ResponseEntity<Result<ResponseTransformConfiguration>> createResponseTransformConfiguration(
			@Valid @RequestBody ResponseTransformConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		ResponseTransformConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserInstructionInputController.class)
				.getResponseTransformConfiguration(result.getTransformId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/response-transforms/{transformId}", method = RequestMethod.GET)
	public Result<ResponseTransformConfiguration> getResponseTransformConfiguration(
			@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		return success(
				userInstructionInputBiz.configurationForId(id, ResponseTransformConfiguration.class));
	}

	@RequestMapping(value = "/response-transforms/{transformId}", method = RequestMethod.PUT)
	public Result<ResponseTransformConfiguration> updateResponseTransformConfiguration(
			@PathVariable("transformId") Long transformId,
			@Valid @RequestBody ResponseTransformConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		ResponseTransformConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/response-transforms/{transformId}", method = RequestMethod.DELETE)
	public Result<Void> deleteResponseTransformConfiguration(
			@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		userInstructionInputBiz.deleteConfiguration(id, ResponseTransformConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints", method = RequestMethod.GET)
	public Result<FilterResults<EndpointConfiguration, UserUuidPK>> listEndpointConfigurations(
			BasicFilter filter) {
		FilterResults<EndpointConfiguration, UserUuidPK> result = userInstructionInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, EndpointConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/endpoints", method = RequestMethod.POST)
	public ResponseEntity<Result<EndpointConfiguration>> createEndpointConfiguration(
			@Valid @RequestBody EndpointConfigurationInput input) {
		UserUuidPK id = UserUuidPK.unassignedUuidKey(getCurrentActorUserId());
		EndpointConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserInstructionInputController.class)
				.getEndpointConfiguration(result.getEndpointId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.GET)
	public Result<EndpointConfiguration> getEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		return success(userInstructionInputBiz.configurationForId(id, EndpointConfiguration.class));
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.PUT)
	public Result<EndpointConfiguration> updateEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId,
			@Valid @RequestBody EndpointConfigurationInput input) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		EndpointConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/endpoints/{endpointId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("enabled") boolean enabled) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		userInstructionInputBiz.enableConfiguration(id, enabled, EndpointConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.DELETE)
	public Result<Void> deleteEndpointConfiguration(@PathVariable("endpointId") UUID endpointId) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		userInstructionInputBiz.deleteConfiguration(id, EndpointConfiguration.class);
		return success();
	}

	/**
	 * Preview transform input DTO.
	 */
	public static record PreviewTransformInput(@JsonProperty("contentType") String contentType,
			@JsonProperty("data") String data, @JsonProperty("query") String query,
			@JsonProperty(value = "parameters", required = false) Map<String, Object> parameters,
			@JsonProperty(value = "instructionResults", required = false) List<TransformInstructionResults> instructionResults) {

		private Map<String, String> queryParameters() {
			if ( query != null && !query.isBlank() ) {
				try {
					URI uri = new URI("http://localhost/?" + query);
					var qMap = UriComponentsBuilder.fromUri(uri).build(true).getQueryParams();
					if ( qMap != null ) {
						Map<String, String> decoded = new HashMap<>(qMap.size());
						for ( Entry<String, List<String>> entry : qMap.entrySet() ) {
							List<String> vals = entry.getValue();
							if ( vals != null && !vals.isEmpty() ) {
								decoded.put(entry.getKey(),
										URLDecoder.decode(vals.get(0), StandardCharsets.UTF_8));
							}
						}
						return decoded;
					}
				} catch ( URISyntaxException e ) {
					// ignore
				}
			}
			return Collections.emptyMap();
		}

	}

	@RequestMapping(value = "/endpoints/{endpointId}/preview", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Result<TransformOutput> previewEndpointTransform(@PathVariable("endpointId") UUID endpointId,
			@RequestBody PreviewTransformInput previewInput) throws IOException {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);

		MediaType mediaType = MediaType.parseMediaType(previewInput.contentType());
		Charset encoding = (mediaType.getCharset() != null ? mediaType.getCharset()
				: StandardCharsets.UTF_8);
		InputStream input = new MaxUploadSizeInputStream(
				new ByteArrayInputStream(previewInput.data().getBytes(encoding)),
				maxInstructionInputLength);

		Map<String, Object> parameters = null;
		Map<String, String> queryParameters = previewInput.queryParameters();
		if ( queryParameters != null ) {
			parameters = new HashMap<>(8);
			parameters.putAll(queryParameters);
		}
		if ( previewInput.parameters() != null ) {
			if ( parameters == null ) {
				parameters = new HashMap<>(8);
			}
			parameters.putAll(previewInput.parameters());
		}

		var result = userInstructionInputBiz.previewTransform(id, mediaType, input, mediaType,
				previewInput.instructionResults(), null);

		return success(result);
	}

	@RequestMapping(value = "/endpoints/auths", method = RequestMethod.GET)
	public Result<FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK>> listEndpointAuthConfigurations(
			BasicFilter filter) {
		FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> result = userInstructionInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, EndpointAuthConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.PUT)
	public ResponseEntity<Result<EndpointAuthConfiguration>> createEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("credentialId") Long credentialId,
			@Valid @RequestBody EndpointAuthConfigurationInput input) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		EndpointAuthConfiguration result = userInstructionInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserInstructionInputController.class)
				.getEndpointAuthConfiguration(endpointId, credentialId)));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.GET)
	public Result<EndpointAuthConfiguration> getEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId,
			@PathVariable("credentialId") Long credentialId) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		return success(userInstructionInputBiz.configurationForId(id, EndpointAuthConfiguration.class));
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("credentialId") Long credentialId,
			@PathVariable("enabled") boolean enabled) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		userInstructionInputBiz.enableConfiguration(id, enabled, EndpointAuthConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.DELETE)
	public Result<Void> deleteEndpointAuthConfiguration(@PathVariable("endpointId") UUID endpointId,
			@PathVariable("credentialId") Long credentialId) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		userInstructionInputBiz.deleteConfiguration(id, EndpointAuthConfiguration.class);
		return success();
	}

}
