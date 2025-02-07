/* ==================================================================
 * JdbcTrustedIssuerCertificateDao.java - 5/08/2023 5:24:08 pm
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

package net.solarnetwork.central.dnp3.dao.jdbc;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.SelectTrustedIssuerCertificate;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateEnabledCertificateFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpsertTrustedIssuerCertificate;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link TrustedIssuerCertificateDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcTrustedIssuerCertificateDao implements TrustedIssuerCertificateDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcTrustedIssuerCertificateDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends TrustedIssuerCertificate> getObjectType() {
		return TrustedIssuerCertificate.class;
	}

	@Override
	public UserStringCompositePK create(Long userId, TrustedIssuerCertificate entity) {
		final var sql = new UpsertTrustedIssuerCertificate(userId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<TrustedIssuerCertificate> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectTrustedIssuerCertificate(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				TrustedIssuerCertificateRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserStringCompositePK save(TrustedIssuerCertificate entity) {
		return create(entity.getUserId(), entity);
	}

	@Override
	public TrustedIssuerCertificate get(UserStringCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setSubjectDn(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectTrustedIssuerCertificate(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				TrustedIssuerCertificateRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<TrustedIssuerCertificate> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardnp3.dnp3_ca_cert";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "subject_dn" };

	@Override
	public void delete(TrustedIssuerCertificate entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<TrustedIssuerCertificate, UserStringCompositePK> findFiltered(
			CertificateFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectTrustedIssuerCertificate(filter);
		return executeFilterQuery(jdbcOps, filter, sql, TrustedIssuerCertificateRowMapper.INSTANCE);
	}

	@Override
	public int updateEnabledStatus(Long userId, CertificateFilter filter, boolean enabled) {
		var sql = new UpdateEnabledCertificateFilter(TABLE_NAME, userId, filter, enabled);
		return jdbcOps.update(sql);
	}
}
