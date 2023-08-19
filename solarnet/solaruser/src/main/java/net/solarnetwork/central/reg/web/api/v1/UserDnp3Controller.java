/* ==================================================================
 * UserDnp3Controller.java - 7/08/2023 10:24:40 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static java.lang.String.format;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.dnp3.config.SolarNetDnp3Configuration.DNP3;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.web.domain.Response.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.dnp3.domain.ServerAuthConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.central.user.dnp3.domain.ServerControlConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerMeasurementConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.TrustedIssuerCertificateInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for DNP3 management.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(DNP3)
@GlobalExceptionRestController
@RestController("v1Dnp3Controller")
@RequestMapping(value = { "/api/v1/sec/user/dnp3", "/u/sec/dnp3" })
public class UserDnp3Controller {

	private final UserDnp3Biz userDnp3Biz;

	/**
	 * Constructor.
	 * 
	 * @param userDnp3Biz
	 *        the user DNP3 service (optional)
	 */

	public UserDnp3Controller(@Autowired(required = false) UserDnp3Biz userDnp3Biz) {
		super();
		this.userDnp3Biz = userDnp3Biz;
	}

	/**
	 * Get the {@link UserDnp3Biz}.
	 * 
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserDnp3Biz userDnp3Biz() {
		if ( userDnp3Biz == null ) {
			throw new UnsupportedOperationException("DNP3 service not available.");
		}
		return userDnp3Biz;
	}

	/**
	 * Import a trusted issuer certificate for the current user.
	 * 
	 * @param data
	 *        the input
	 * @return the parsed certificate configurations
	 */
	@RequestMapping(method = POST, value = "/trusted-issuer-certs", consumes = MULTIPART_FORM_DATA_VALUE)
	public Result<Collection<TrustedIssuerCertificate>> importTrustedIssuerCertificates(
			@RequestPart("file") MultipartFile data) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CertificateFactory cf = CertificateUtils.x509CertificateFactory();
		Collection<? extends Certificate> certs;
		try {
			certs = cf.generateCertificates(data.getInputStream());
		} catch ( java.security.cert.CertificateException | IOException e ) {
			throw new CertificateException("Error parsing certificate data.", e);
		}
		if ( certs == null || certs.isEmpty() ) {
			return success();
		}
		X509Certificate[] x509Certs = certs.stream().map(X509Certificate.class::cast)
				.toArray(X509Certificate[]::new);
		Collection<TrustedIssuerCertificate> result = userDnp3Biz().saveTrustedIssuerCertificates(userId,
				x509Certs);
		return success(result);
	}

	/**
	 * List trusted issuer certificates for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all certificates
	 *        for the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/trusted-issuer-certs")
	public Result<FilterResults<TrustedIssuerCertificate, UserStringCompositePK>> listTrustedIssuerCertificates(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().trustedIssuerCertificatesForUser(userId, criteria));
	}

	/**
	 * Delete trusted issuer certificate for the current user.
	 * 
	 * @param identifier
	 *        the certificate identifier (subject DN) to delete
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/trusted-issuer-certs/{identifier}")
	public Result<Void> deleteTrustedIssuerCertificate(@PathVariable("identifier") String identifier) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().deleteTrustedIssuerCertificate(userId, identifier);
		return success();
	}

	/**
	 * Delete trusted issuer certificate for the current user.
	 * 
	 * @param identifier
	 *        the certificate identifier (subject DN) to delete
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/trusted-issuer-certs")
	public Result<Void> deleteTrustedIssuerCertificateAlt(@RequestParam("subjectDn") String identifier) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().deleteTrustedIssuerCertificate(userId, identifier);
		return success();
	}

	/**
	 * Update the enabled status of trusted issuer certificate for the current
	 * user.
	 * 
	 * @param input
	 *        the input
	 * @return the updated certificate configuration
	 */
	@RequestMapping(method = POST, value = "/trusted-issuer-certs", consumes = APPLICATION_JSON_VALUE)
	public Result<TrustedIssuerCertificate> updateTrustedIssuerCertificateEnabledStatus(
			@Valid @RequestBody TrustedIssuerCertificateInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final String subjectDn = input.getSubjectDn();
		BasicFilter criteria = new BasicFilter();
		criteria.setSubjectDn(subjectDn);
		userDnp3Biz().updateTrustedIssuerCertificateEnabledStatus(userId, criteria, input.isEnabled());
		TrustedIssuerCertificate result = stream(
				userDnp3Biz().trustedIssuerCertificatesForUser(userId, criteria).spliterator(), false)
						.findFirst().orElse(null);
		return success(result);
	}

	/**
	 * Update trusted issuer certificate enabled status for the current user.
	 * 
	 * @param enabled
	 *        the enabled status to set
	 * @param criteria
	 *        the optional criteria; if not provided then update all entities
	 *        for the active user
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/trusted-issuer-certs/enabled/{enabled}", consumes = APPLICATION_JSON_VALUE)
	public Result<Void> updateTrustedIssuerCertificateEnabledStatus(
			@PathVariable("enabled") boolean enabled,
			@RequestBody(required = false) final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().updateTrustedIssuerCertificateEnabledStatus(userId, criteria, enabled);
		return success();
	}

	/**
	 * Create a new server configuration.
	 * 
	 * @param input
	 *        the configuration input
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/servers", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Result<ServerConfiguration>> createServer(
			@Valid @RequestBody ServerConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ServerConfiguration result = userDnp3Biz().createServer(userId, input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(UserDnp3Controller.class).getServer(result.getServerId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * List server configurations for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all servers for
	 *        the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/servers")
	public Result<FilterResults<ServerConfiguration, UserLongCompositePK>> listServers(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().serversForUser(userId, criteria));
	}

	/**
	 * Update server enabled status for the current user.
	 * 
	 * @param enabled
	 *        the enabled status to set
	 * @param criteria
	 *        the optional criteria; if not provided then update all entities
	 *        for the active user
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/servers/enabled/{enabled}", consumes = APPLICATION_JSON_VALUE)
	public Result<Void> updateServerEnabledStatus(@PathVariable("enabled") boolean enabled,
			@RequestBody(required = false) final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().updateServerEnabledStatus(userId, criteria, enabled);
		return success();
	}

	/**
	 * Get a server configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/servers/{serverId}")
	public Result<ServerConfiguration> getServer(@PathVariable("serverId") Long serverId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final BasicFilter filter = new BasicFilter();
		filter.setServerId(serverId);
		var results = userDnp3Biz.serversForUser(userId, filter);
		ServerConfiguration result = requireNonNullObject(
				stream(results.spliterator(), false).findFirst().orElse(null), serverId);
		return success(result);
	}

	/**
	 * Update a server configuration for the current user.
	 * 
	 * @param
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/servers/{serverId}", consumes = APPLICATION_JSON_VALUE)
	public Result<ServerConfiguration> updateServer(@PathVariable("serverId") Long serverId,
			@Valid @RequestBody ServerConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ServerConfiguration result = userDnp3Biz().updateServer(userId, serverId, input);
		return success(result);
	}

	/**
	 * Delete a server configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to delete
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/servers/{serverId}")
	public Result<Void> deleteServer(@PathVariable("serverId") Long serverId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz.deleteServer(userId, serverId);
		return success();
	}

	/**
	 * List server auth configurations for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all servers for
	 *        the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/servers/auths")
	public Result<FilterResults<ServerAuthConfiguration, UserLongStringCompositePK>> listServerAuths(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().serverAuthsForUser(userId, criteria));
	}

	/**
	 * Get a server auth configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param identifier
	 *        the identifier to fetch
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/servers/{serverId}/auths")
	public Result<ServerAuthConfiguration> getServerAuth(@PathVariable("serverId") Long serverId,
			@RequestParam("identifier") String identifier) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final BasicFilter filter = new BasicFilter();
		filter.setServerId(serverId);
		filter.setIdentifier(identifier);
		var results = userDnp3Biz.serverAuthsForUser(userId, filter);
		ServerAuthConfiguration result = requireNonNullObject(
				stream(results.spliterator(), false).findFirst().orElse(null), identifier);
		return success(result);
	}

	/**
	 * Update a server auth configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param identifier
	 *        the identifier to fetch
	 * @param input
	 *        the input
	 * @return the configuration
	 */
	@RequestMapping(method = POST, value = "/servers/{serverId}/auths", consumes = APPLICATION_JSON_VALUE)
	public Result<ServerAuthConfiguration> saveServerAuth(@PathVariable("serverId") Long serverId,
			@Valid @RequestBody ServerAuthConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ServerAuthConfiguration result = userDnp3Biz().saveServerAuth(userId, serverId,
				input.getIdentifier(), input);
		return success(result);
	}

	/**
	 * Delete a server auth configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param identifier
	 *        the identifier to fetch
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/servers/{serverId}/auths")
	public Result<Void> deleteServerAuth(@PathVariable("serverId") Long serverId,
			@RequestParam("identifier") String identifier) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz.deleteServerAuth(userId, serverId, identifier);
		return success();
	}

	/**
	 * Update server auth enabled status for the current user.
	 * 
	 * @param enabled
	 *        the enabled status to set
	 * @param criteria
	 *        the optional criteria; if not provided then update all entities
	 *        for the active user
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/servers/auths/enabled/{enabled}", consumes = APPLICATION_JSON_VALUE)
	public Result<Void> updateServerAuthEnabledStatus(@PathVariable("enabled") boolean enabled,
			@RequestBody(required = false) final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().updateServerAuthEnabledStatus(userId, criteria, enabled);
		return success();
	}

	/**
	 * List server measurement configurations for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all servers for
	 *        the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/servers/measurements")
	public Result<FilterResults<ServerMeasurementConfiguration, UserLongIntegerCompositePK>> listServerMeasurements(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().serverMeasurementsForUser(userId, criteria));
	}

	/**
	 * Get a server measurement configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/servers/{serverId}/measurements/{index}")
	public Result<ServerMeasurementConfiguration> getServerMeasurement(
			@PathVariable("serverId") Long serverId, @PathVariable("index") Integer index) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final BasicFilter filter = new BasicFilter();
		filter.setServerId(serverId);
		filter.setIndex(index);
		var results = userDnp3Biz.serverMeasurementsForUser(userId, filter);
		ServerMeasurementConfiguration result = requireNonNullObject(
				stream(results.spliterator(), false).findFirst().orElse(null), index);
		return success(result);
	}

	/**
	 * Update a server measurement configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @param input
	 *        the input
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/servers/{serverId}/measurements/{index}", consumes = APPLICATION_JSON_VALUE)
	public Result<ServerMeasurementConfiguration> saveServerMeasurement(
			@PathVariable("serverId") Long serverId, @PathVariable("index") Integer index,
			@Valid @RequestBody ServerMeasurementConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ServerMeasurementConfiguration result = userDnp3Biz().saveServerMeasurement(userId, serverId,
				index, input);
		return success(result);
	}

	/**
	 * Delete a server measurement configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/servers/{serverId}/measurements/{index}")
	public Result<Void> deleteServerMeasurement(@PathVariable("serverId") Long serverId,
			@PathVariable("index") Integer index) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz.deleteServerMeasurement(userId, serverId, index);
		return success();
	}

	/**
	 * Update server measurement enabled status for the current user.
	 * 
	 * @param enabled
	 *        the enabled status to set
	 * @param criteria
	 *        the optional criteria; if not provided then update all entities
	 *        for the active user
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/servers/measurements/enabled/{enabled}", consumes = APPLICATION_JSON_VALUE)
	public Result<Void> updateServerMeasurementEnabledStatus(@PathVariable("enabled") boolean enabled,
			@RequestBody(required = false) final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().updateServerMeasurementEnabledStatus(userId, criteria, enabled);
		return success();
	}

	/**
	 * List server control configurations for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all servers for
	 *        the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/servers/controls")
	public Result<FilterResults<ServerControlConfiguration, UserLongIntegerCompositePK>> listServerControls(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().serverControlsForUser(userId, criteria));
	}

	/**
	 * Get a server control configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/servers/{serverId}/controls/{index}")
	public Result<ServerControlConfiguration> getServerControl(@PathVariable("serverId") Long serverId,
			@PathVariable("index") Integer index) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final BasicFilter filter = new BasicFilter();
		filter.setServerId(serverId);
		filter.setIndex(index);
		var results = userDnp3Biz.serverControlsForUser(userId, filter);
		ServerControlConfiguration result = requireNonNullObject(
				stream(results.spliterator(), false).findFirst().orElse(null), index);
		return success(result);
	}

	/**
	 * Update a server control configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @param input
	 *        the input
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/servers/{serverId}/controls/{index}", consumes = APPLICATION_JSON_VALUE)
	public Result<ServerControlConfiguration> saveServerControl(@PathVariable("serverId") Long serverId,
			@PathVariable("index") Integer index,
			@Valid @RequestBody ServerControlConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ServerControlConfiguration result = userDnp3Biz().saveServerControl(userId, serverId, index,
				input);
		return success(result);
	}

	/**
	 * Delete a server control configuration for the current user.
	 * 
	 * @param serverId
	 *        the server ID to fetch
	 * @param index
	 *        the index to fetch
	 * @return the result
	 */
	@RequestMapping(method = DELETE, value = "/servers/{serverId}/controls/{index}")
	public Result<Void> deleteServerControl(@PathVariable("serverId") Long serverId,
			@PathVariable("index") Integer index) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz.deleteServerControl(userId, serverId, index);
		return success();
	}

	/**
	 * Update server control enabled status for the current user.
	 * 
	 * @param enabled
	 *        the enabled status to set
	 * @param criteria
	 *        the optional criteria; if not provided then update all entities
	 *        for the active user
	 * @return the result
	 */
	@RequestMapping(method = POST, value = "/servers/controls/enabled/{enabled}", consumes = APPLICATION_JSON_VALUE)
	public Result<Void> updateServerControlEnabledStatus(@PathVariable("enabled") boolean enabled,
			@RequestBody(required = false) final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userDnp3Biz().updateServerControlEnabledStatus(userId, criteria, enabled);
		return success();
	}

	/**
	 * Download an example resource to use with the
	 * {@link #importServerConfigurationCsv(Long, MultipartFile, Locale)} API.
	 * 
	 * @param accept
	 *        the desired type: can be CSV or XLSX
	 * @return the result
	 */
	@RequestMapping(value = "/servers/csv-example", method = RequestMethod.GET, produces = {
			WebUtils.TEXT_CSV_MEDIA_TYPE_VALUE, WebUtils.XLSX_MEDIA_TYPE_VALUE })
	public ResponseEntity<Resource> getServerConfigurationCsvExample(
			@RequestHeader(HttpHeaders.ACCEPT) final String accept) {
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		MimeType mime = null;
		for ( MediaType acceptType : acceptTypes ) {
			if ( WebUtils.TEXT_CSV_MEDIA_TYPE.isCompatibleWith(acceptType) ) {
				mime = MimeType.valueOf(WebUtils.TEXT_CSV_MEDIA_TYPE_VALUE);
				break;
			} else if ( WebUtils.XLSX_MEDIA_TYPE.isCompatibleWith(acceptType) ) {
				mime = MimeType.valueOf(WebUtils.XLSX_MEDIA_TYPE_VALUE);
				break;
			}
		}
		if ( mime == null ) {
			return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
		}
		Resource result = userDnp3Biz().serverConfigurationCsvExample(mime);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.asMediaType(mime));
		headers.set(HttpHeaders.CONTENT_DISPOSITION,
				format("attachment; filename=\"%s\"", result.getFilename()));
		return new ResponseEntity<>(result, headers, HttpStatus.OK);
	}

	/**
	 * Import measurement and control configuration for a specific server from a
	 * CSV resource.
	 * 
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 * 
	 * @param serverId
	 *        the ID of the server configuration to import for
	 * @param data
	 *        the CSV data to import
	 * @param locale
	 *        the locale
	 * @return the result
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/servers/{serverId}/csv", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Response<ServerConfigurations> importServerConfigurationCsv(
			@PathVariable("serverId") Long serverId, @RequestPart("file") MultipartFile data,
			Locale locale) throws IOException {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		final ServerConfigurations result = userDnp3Biz().importServerConfigurationsCsv(userId, serverId,
				data, locale);
		return response(result);
	}

	/**
	 * Export measurement and control configuration for a specific server as a
	 * CSV resource.
	 * 
	 * <p>
	 * The actor must have an associated user ID as provided by
	 * {@link SecurityUtils#getCurrentActorUserId()}.
	 * </p>
	 * 
	 * @param serverId
	 *        the ID of the server configuration to import for
	 * @param response
	 *        the HTTP response
	 * @param locale
	 *        the locale
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/servers/{serverId}/csv", method = RequestMethod.GET)
	public void exportServerConfigurationCsv(@PathVariable("serverId") Long serverId,
			HttpServletResponse response, Locale locale) throws IOException {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"solarnet-dnp3-server-%d.csv\"".formatted(serverId));
		final BasicFilter filter = new BasicFilter();
		filter.setServerId(serverId);
		userDnp3Biz().exportServerConfigurationsCsv(userId, filter, response.getOutputStream(), locale);
	}

}
