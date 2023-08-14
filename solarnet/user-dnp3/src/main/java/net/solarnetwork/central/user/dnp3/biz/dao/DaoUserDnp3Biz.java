/* ==================================================================
 * DaoUserDnp3Biz.java - 7/08/2023 8:52:07 am
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

package net.solarnetwork.central.user.dnp3.biz.dao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerDataPointFilter;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.dnp3.domain.ServerAuthConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationsInput;
import net.solarnetwork.central.user.dnp3.domain.ServerControlConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerMeasurementConfigurationInput;
import net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvParser;
import net.solarnetwork.central.user.dnp3.support.ServerConfigurationsCsvWriter;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO-based implementation of {@link UserDnp3Biz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserDnp3Biz implements UserDnp3Biz {

	private final TrustedIssuerCertificateDao trustedCertDao;
	private final ServerConfigurationDao serverDao;
	private final ServerAuthConfigurationDao serverAuthDao;
	private final ServerMeasurementConfigurationDao serverMeasurementDao;
	private final ServerControlConfigurationDao serverControlDao;
	private final ResourceLoader resourceLoader;
	private Map<String, String> csvImportExampleResources;

	private final MessageSource csvImportMessageSource;
	private Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param trustedCertDao
	 *        the trusted certificate DAO to use
	 * @param serverDao
	 *        the server DAO to use
	 * @param serverAuthDao
	 *        the server auth DAO to use
	 * @param serverMeasurementDao
	 *        the server measurement DAO to use
	 * @param serverControlDao
	 *        the server control DAO to use
	 * @param resourceLoader
	 *        the resource loader to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserDnp3Biz(TrustedIssuerCertificateDao trustedCertDao, ServerConfigurationDao serverDao,
			ServerAuthConfigurationDao serverAuthDao,
			ServerMeasurementConfigurationDao serverMeasurementDao,
			ServerControlConfigurationDao serverControlDao, ResourceLoader resourceLoader) {
		super();
		this.trustedCertDao = requireNonNullArgument(trustedCertDao, "trustedCertDao");
		this.serverDao = requireNonNullArgument(serverDao, "serverDao");
		this.serverAuthDao = requireNonNullArgument(serverAuthDao, "serverAuthDao");
		this.serverMeasurementDao = requireNonNullArgument(serverMeasurementDao, "serverMeasurementDao");
		this.serverControlDao = requireNonNullArgument(serverControlDao, "serverControlDao");
		this.resourceLoader = requireNonNullArgument(resourceLoader, "resourceLoader");

		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename(ServerConfigurationsCsvParser.class.getName());
		this.csvImportMessageSource = ms;

		Map<String, String> importExampleMapping = new HashMap<>(2);
		importExampleMapping.put(WebUtils.TEXT_CSV_MEDIA_TYPE_VALUE,
				"classpath:net/solarnetwork/central/user/dnp3/support/SolarDNP3 Configuration Example.csv");
		importExampleMapping.put(WebUtils.XLSX_MEDIA_TYPE_VALUE,
				"classpath:net/solarnetwork/central/user/dnp3/support/SolarDNP3 Configuration Example.xlsx");
		this.csvImportExampleResources = importExampleMapping;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Collection<TrustedIssuerCertificate> saveTrustedIssuerCertificates(Long userId,
			X509Certificate[] certificates) {
		requireNonEmptyArgument(certificates, "certificates");
		List<TrustedIssuerCertificate> result = new ArrayList<>(certificates.length);
		Instant now = Instant.now();
		for ( X509Certificate cert : certificates ) {
			TrustedIssuerCertificate trusted = new TrustedIssuerCertificate(userId, cert, now);
			trusted.setModified(now);
			trusted.setEnabled(true);
			trustedCertDao.save(trusted);
			result.add(trusted);
		}
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteTrustedIssuerCertificate(Long userId, String subjectDn) {
		trustedCertDao.delete(new TrustedIssuerCertificate(userId, subjectDn, Instant.now()));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<TrustedIssuerCertificate, UserStringCompositePK> trustedIssuerCertificatesForUser(
			Long userId, CertificateFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return trustedCertDao.findFiltered(userFilter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerConfiguration createServer(Long userId, ServerConfigurationInput input) {
		UserLongCompositePK unassignedId = unassignedEntityIdKey(userId);
		ServerConfiguration conf = requireNonNullArgument(input, "input").toEntity(unassignedId);

		UserLongCompositePK pk = serverDao.create(userId, conf);
		return serverDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerConfiguration updateServer(Long userId, Long serverId, ServerConfigurationInput input) {
		ServerConfiguration conf = requireNonNullArgument(input, "input")
				.toEntity(new UserLongCompositePK(userId, serverId));

		UserLongCompositePK pk = requireNonNullObject(serverDao.save(conf), serverId);
		return serverDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteServer(Long userId, Long serverId) {
		serverDao.delete(new ServerConfiguration(userId, serverId, Instant.EPOCH));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ServerConfiguration, UserLongCompositePK> serversForUser(Long userId,
			ServerFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return serverDao.findFiltered(userFilter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerAuthConfiguration saveServerAuth(Long userId, Long serverId, String identity,
			ServerAuthConfigurationInput input) {
		ServerAuthConfiguration conf = requireNonNullArgument(input, "input")
				.toEntity(new UserLongStringCompositePK(userId, serverId, identity));

		UserLongStringCompositePK pk = requireNonNullObject(serverAuthDao.save(conf), serverId);
		return serverAuthDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteServerAuth(Long userId, Long serverId, String identifier) {
		serverAuthDao.delete(new ServerAuthConfiguration(userId, serverId, identifier, Instant.EPOCH));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ServerAuthConfiguration, UserLongStringCompositePK> serverAuthsForUser(
			Long userId, ServerFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return serverAuthDao.findFiltered(userFilter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerMeasurementConfiguration saveServerMeasurement(Long userId, Long serverId,
			Integer index, ServerMeasurementConfigurationInput input) {
		ServerMeasurementConfiguration conf = requireNonNullArgument(input, "input")
				.toEntity(new UserLongIntegerCompositePK(userId, serverId, index));

		UserLongIntegerCompositePK pk = requireNonNullObject(serverMeasurementDao.save(conf), serverId);
		return serverMeasurementDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteServerMeasurement(Long userId, Long serverId, Integer index) {
		serverMeasurementDao
				.delete(new ServerMeasurementConfiguration(userId, serverId, index, Instant.EPOCH));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ServerMeasurementConfiguration, UserLongIntegerCompositePK> serverMeasurementsForUser(
			Long userId, ServerDataPointFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return serverMeasurementDao.findFiltered(userFilter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerControlConfiguration saveServerControl(Long userId, Long serverId, Integer index,
			ServerControlConfigurationInput input) {
		ServerControlConfiguration conf = requireNonNullArgument(input, "input")
				.toEntity(new UserLongIntegerCompositePK(userId, serverId, index));

		UserLongIntegerCompositePK pk = requireNonNullObject(serverControlDao.save(conf), serverId);
		return serverControlDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteServerControl(Long userId, Long serverId, Integer index) {
		serverControlDao.delete(new ServerControlConfiguration(userId, serverId, index, Instant.EPOCH));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ServerControlConfiguration, UserLongIntegerCompositePK> serverControlsForUser(
			Long userId, ServerDataPointFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return serverControlDao.findFiltered(userFilter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateTrustedIssuerCertificateEnabledStatus(Long userId, CertificateFilter filter,
			boolean enabled) {
		trustedCertDao.updateEnabledStatus(userId, filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateServerEnabledStatus(Long userId, ServerFilter filter, boolean enabled) {
		serverDao.updateEnabledStatus(userId, filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateServerAuthEnabledStatus(Long userId, ServerFilter filter, boolean enabled) {
		serverAuthDao.updateEnabledStatus(userId, filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateServerMeasurementEnabledStatus(Long userId, ServerDataPointFilter filter,
			boolean enabled) {
		serverMeasurementDao.updateEnabledStatus(userId, filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateServerControlEnabledStatus(Long userId, ServerDataPointFilter filter,
			boolean enabled) {
		serverControlDao.updateEnabledStatus(userId, filter, enabled);
	}

	@Override
	public Resource serverConfigurationCsvExample(MimeType mimeType) {
		String path = csvImportExampleResources
				.get(requireNonNullArgument(mimeType, "mimeType").toString());
		if ( path == null ) {
			throw new IllegalArgumentException("MIME type [" + mimeType + "] is not supported.");
		}
		Resource result = resourceLoader.getResource(path);
		if ( !result.exists() ) {
			throw new RuntimeException(
					"Server configuration CSV example resource [%s] for MIME type [%s] not found."
							.formatted(path, mimeType));
		}
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ServerConfigurations importServerConfigurationsCsv(Long userId, Long serverId,
			InputStreamSource csv, Locale locale) throws IOException {
		final Instant date = Instant.now();
		ServerConfigurationsInput result = null;
		try (ICsvListReader in = new CsvListReader(new InputStreamReader(csv.getInputStream(), UTF_8),
				CsvPreference.STANDARD_PREFERENCE)) {
			result = new ServerConfigurationsCsvParser(csvImportMessageSource,
					locale != null ? locale : Locale.getDefault()).parse(in);
		}

		if ( validator != null ) {
			var violations = validator.validate(result);
			if ( violations != null && !violations.isEmpty() ) {
				throw new ConstraintViolationException("Invalid CSV input detected.", violations);
			}
		}

		List<ServerMeasurementConfiguration> mConfs = null;
		int mCount = 0;
		if ( result.getMeasurementConfigs() != null ) {
			mConfs = new ArrayList<>(result.getMeasurementConfigs().size());
			for ( var conf : result.getMeasurementConfigs() ) {
				var c = conf.toEntity(new UserLongIntegerCompositePK(userId, serverId, mCount++), date);
				c.setModified(date);
				serverMeasurementDao.save(c);
				mConfs.add(c);
			}
		}
		// delete any indexes higher than the ones just saved
		serverMeasurementDao
				.deleteForMinimumIndex(new UserLongIntegerCompositePK(userId, serverId, mCount));

		List<ServerControlConfiguration> cConfs = null;
		int cCount = 0;
		if ( result.getControlConfigs() != null ) {
			cConfs = new ArrayList<>(result.getControlConfigs().size());
			for ( var conf : result.getControlConfigs() ) {
				var c = conf.toEntity(new UserLongIntegerCompositePK(userId, serverId, cCount++), date);
				c.setModified(date);
				serverControlDao.save(c);
				cConfs.add(c);
			}
		}
		// delete any indexes higher than the ones just saved
		serverControlDao.deleteForMinimumIndex(new UserLongIntegerCompositePK(userId, serverId, cCount));

		return new ServerConfigurations(mConfs, cConfs);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public void exportServerConfigurationsCsv(Long userId, ServerDataPointFilter filter,
			OutputStream out, Locale locale) throws IOException {
		try (ICsvListWriter csv = new CsvListWriter(new OutputStreamWriter(out, UTF_8),
				CsvPreference.STANDARD_PREFERENCE)) {
			var measurements = serverMeasurementsForUser(userId, filter);
			var controls = serverControlsForUser(userId, filter);
			new ServerConfigurationsCsvWriter(csv, csvImportMessageSource,
					locale != null ? locale : Locale.getDefault())
							.generateCsv(new ServerConfigurations(measurements, controls));
		}
	}

	/**
	 * Get the validator.
	 * 
	 * @return the validator
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 * 
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Get the CSV import example resource mapping of MIME types to resource
	 * paths.
	 * 
	 * @return the resources
	 */
	public Map<String, String> getCsvImportExampleResources() {
		return csvImportExampleResources;
	}

	/**
	 * Set the CSV import example resource mapping of MIME types to resource
	 * paths.
	 * 
	 * <p>
	 * The values are assumed to be file system paths, unless prefixed with
	 * {@code classpath:} in which case it is treated as a classpath resource.
	 * </p>
	 * 
	 * @param csvImportExampleResources
	 *        the resource mapping to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public void setCsvImportExampleResources(Map<String, String> csvImportExampleResources) {
		this.csvImportExampleResources = requireNonNullArgument(csvImportExampleResources,
				"csvImportExampleResources");
	}

}
