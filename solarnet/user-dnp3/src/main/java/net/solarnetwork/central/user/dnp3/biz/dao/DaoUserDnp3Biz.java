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

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserDnp3Biz(TrustedIssuerCertificateDao trustedCertDao, ServerConfigurationDao serverDao,
			ServerAuthConfigurationDao serverAuthDao,
			ServerMeasurementConfigurationDao serverMeasurementDao,
			ServerControlConfigurationDao serverControlDao) {
		super();
		this.trustedCertDao = requireNonNullArgument(trustedCertDao, "trustedCertDao");
		this.serverDao = requireNonNullArgument(serverDao, "serverDao");
		this.serverAuthDao = requireNonNullArgument(serverAuthDao, "serverAuthDao");
		this.serverMeasurementDao = requireNonNullArgument(serverMeasurementDao, "serverMeasurementDao");
		this.serverControlDao = requireNonNullArgument(serverControlDao, "serverControlDao");
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

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ServerConfiguration, UserLongCompositePK> serversForUser(Long userId,
			ServerFilter filter) {
		var userFilter = new BasicFilter(requireNonNullArgument(filter, "filter"));
		userFilter.setUserId(requireNonNullArgument(userId, "userId"));
		return serverDao.findFiltered(userFilter);
	}

}
