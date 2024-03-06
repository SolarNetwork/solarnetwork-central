/* ==================================================================
 * UserDatumInputController.java - 25/02/2024 7:06:49 am
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import net.solarnetwork.central.din.config.SolarNetDatumInputConfiguration;
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.user.din.domain.CredentialConfigurationInput;
import net.solarnetwork.central.user.din.domain.EndpointAuthConfigurationInput;
import net.solarnetwork.central.user.din.domain.EndpointConfigurationInput;
import net.solarnetwork.central.user.din.domain.TransformConfigurationInput;
import net.solarnetwork.central.user.din.domain.TransformOutput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.MaxUploadSizeInputStream;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for DNP3 management.
 *
 * @author matt
 * @version 1.1
 */
@Profile(SolarNetDatumInputConfiguration.DATUM_INPUT)
@GlobalExceptionRestController
@RestController("v1DatumInputController")
@RequestMapping(value = { "/api/v1/sec/user/din", "/u/sec/din" })
public class UserDatumInputController {

	private final UserDatumInputBiz userDatumInputBiz;
	private final long maxDatumInputLength;

	/**
	 * Constructor.
	 *
	 * @param userDatumInputBiz
	 *        the service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDatumInputController(UserDatumInputBiz userDatumInputBiz,
			@Value("${app.din.max-datum-input-length}") long maxDatumInputLength) {
		super();
		this.userDatumInputBiz = requireNonNullArgument(userDatumInputBiz, "userDatumInputBiz");
		this.maxDatumInputLength = maxDatumInputLength;
	}

	/**
	 * List the available transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = userDatumInputBiz.availableTransformServices(locale);
		return success(result);
	}

	@RequestMapping(value = "/credentials", method = RequestMethod.GET)
	public Result<FilterResults<CredentialConfiguration, UserLongCompositePK>> listCredentialConfigurations(
			BasicFilter filter) {
		FilterResults<CredentialConfiguration, UserLongCompositePK> result = userDatumInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, CredentialConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/credentials", method = RequestMethod.POST)
	public ResponseEntity<Result<CredentialConfiguration>> createCredentialConfiguration(
			@Valid @RequestBody CredentialConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		CredentialConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserDatumInputController.class)
				.getCredentialConfiguration(result.getCredentialId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.GET)
	public Result<CredentialConfiguration> getCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		return success(userDatumInputBiz.configurationForId(id, CredentialConfiguration.class));
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.PUT)
	public Result<CredentialConfiguration> updateCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId,
			@Valid @RequestBody CredentialConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		CredentialConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/credentials/{credentialId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableCredentialConfiguration(
			@PathVariable("credentialId") Long credentialId, @PathVariable("enabled") boolean enabled) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		userDatumInputBiz.enableConfiguration(id, enabled, CredentialConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/credentials/{credentialId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCredentialConfiguration(@PathVariable("credentialId") Long credentialId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), credentialId);
		userDatumInputBiz.deleteConfiguration(id, CredentialConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/transforms", method = RequestMethod.GET)
	public Result<FilterResults<TransformConfiguration, UserLongCompositePK>> listTransformConfigurations(
			BasicFilter filter) {
		FilterResults<TransformConfiguration, UserLongCompositePK> result = userDatumInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, TransformConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/transforms", method = RequestMethod.POST)
	public ResponseEntity<Result<TransformConfiguration>> createTransformConfiguration(
			@Valid @RequestBody TransformConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		TransformConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(
				on(UserDatumInputController.class).getTransformConfiguration(result.getTransformId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/transforms/{transformId}", method = RequestMethod.GET)
	public Result<TransformConfiguration> getTransformConfiguration(
			@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		return success(userDatumInputBiz.configurationForId(id, TransformConfiguration.class));
	}

	@RequestMapping(value = "/transforms/{transformId}", method = RequestMethod.PUT)
	public Result<TransformConfiguration> updateTransformConfiguration(
			@PathVariable("transformId") Long transformId,
			@Valid @RequestBody TransformConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		TransformConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/transforms/{transformId}", method = RequestMethod.DELETE)
	public Result<Void> deleteTransformConfiguration(@PathVariable("transformId") Long transformId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);
		userDatumInputBiz.deleteConfiguration(id, TransformConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/transforms/{transformId}/preview", method = RequestMethod.POST, consumes = {
			MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE })
	public Result<TransformOutput> previewTransform(@PathVariable("transformId") Long transformId,
			@RequestHeader(value = "Content-Type", required = true) String contentType,
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, InputStream in)
			throws IOException {
		return previewEndpointTransform(transformId, null, contentType, encoding, in);
	}

	@RequestMapping(value = "/transforms/{transformId}/preview/{endpointId}", method = RequestMethod.POST, consumes = {
			MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE })
	public Result<TransformOutput> previewEndpointTransform(
			@PathVariable("transformId") Long transformId, @PathVariable("endpointId") UUID endpointId,
			@RequestHeader(value = "Content-Type", required = true) String contentType,
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, InputStream in)
			throws IOException {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);

		final MediaType mediaType = MediaType.parseMediaType(contentType);

		InputStream input = in;
		if ( encoding != null && encoding.toLowerCase().contains("gzip") ) {
			input = new GZIPInputStream(in);
		}

		var result = userDatumInputBiz.previewTransform(id, endpointId, mediaType, input, null);

		return success(result);
	}

	/**
	 * Preview transform input DTO.
	 */
	public static record PreviewTransformInput(@JsonProperty(value = "contentType") String contentType,
			@JsonProperty(value = "data") String data,
			@JsonProperty(value = "parameters", required = false) Map<String, Object> parameters) {

	}

	@RequestMapping(value = "/transforms/{transformId}/preview/{endpointId}/params", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Result<TransformOutput> previewEndpointTransform(
			@PathVariable("transformId") Long transformId, @PathVariable("endpointId") UUID endpointId,
			@RequestBody PreviewTransformInput previewInput) throws IOException {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), transformId);

		MediaType mediaType = MediaType.parseMediaType(previewInput.contentType());
		Charset encoding = (mediaType.getCharset() != null ? mediaType.getCharset()
				: StandardCharsets.UTF_8);
		InputStream input = new MaxUploadSizeInputStream(
				new ByteArrayInputStream(previewInput.data().getBytes(encoding)), maxDatumInputLength);

		Map<String, Object> parameters = null;
		if ( previewInput.parameters() != null ) {
			parameters = new HashMap<>(8);
			parameters.putAll(previewInput.parameters());
		}
		var result = userDatumInputBiz.previewTransform(id, endpointId, mediaType, input, parameters);

		return success(result);
	}

	@RequestMapping(value = "/endpoints", method = RequestMethod.GET)
	public Result<FilterResults<EndpointConfiguration, UserUuidPK>> listEndpointConfigurations(
			BasicFilter filter) {
		FilterResults<EndpointConfiguration, UserUuidPK> result = userDatumInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, EndpointConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/endpoints", method = RequestMethod.POST)
	public ResponseEntity<Result<EndpointConfiguration>> createEndpointConfiguration(
			@Valid @RequestBody EndpointConfigurationInput input) {
		UserUuidPK id = UserUuidPK.unassignedUuidKey(getCurrentActorUserId());
		EndpointConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(
				on(UserDatumInputController.class).getEndpointConfiguration(result.getEndpointId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.GET)
	public Result<EndpointConfiguration> getEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		return success(userDatumInputBiz.configurationForId(id, EndpointConfiguration.class));
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.PUT)
	public Result<EndpointConfiguration> updateEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId,
			@Valid @RequestBody EndpointConfigurationInput input) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		EndpointConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/endpoints/{endpointId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableEndpointConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("enabled") boolean enabled) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		userDatumInputBiz.enableConfiguration(id, enabled, EndpointConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints/{endpointId}", method = RequestMethod.DELETE)
	public Result<Void> deleteEndpointConfiguration(@PathVariable("endpointId") UUID endpointId) {
		UserUuidPK id = new UserUuidPK(getCurrentActorUserId(), endpointId);
		userDatumInputBiz.deleteConfiguration(id, EndpointConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints/auths", method = RequestMethod.GET)
	public Result<FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK>> listEndpointAuthConfigurations(
			BasicFilter filter) {
		FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> result = userDatumInputBiz
				.configurationsForUser(getCurrentActorUserId(), filter, EndpointAuthConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.PUT)
	public ResponseEntity<Result<EndpointAuthConfiguration>> createEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("credentialId") Long credentialId,
			@Valid @RequestBody EndpointAuthConfigurationInput input) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		EndpointAuthConfiguration result = userDatumInputBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserDatumInputController.class)
				.getEndpointAuthConfiguration(endpointId, credentialId)));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.GET)
	public Result<EndpointAuthConfiguration> getEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId,
			@PathVariable("credentialId") Long credentialId) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		return success(userDatumInputBiz.configurationForId(id, EndpointAuthConfiguration.class));
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CredentialConfiguration> enableEndpointAuthConfiguration(
			@PathVariable("endpointId") UUID endpointId, @PathVariable("credentialId") Long credentialId,
			@PathVariable("enabled") boolean enabled) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		userDatumInputBiz.enableConfiguration(id, enabled, EndpointAuthConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/endpoints/{endpointId}/auths/{credentialId}", method = RequestMethod.DELETE)
	public Result<Void> deleteEndpointAuthConfiguration(@PathVariable("endpointId") UUID endpointId,
			@PathVariable("credentialId") Long credentialId) {
		UserUuidLongCompositePK id = new UserUuidLongCompositePK(getCurrentActorUserId(), endpointId,
				credentialId);
		userDatumInputBiz.deleteConfiguration(id, EndpointAuthConfiguration.class);
		return success();
	}

}
