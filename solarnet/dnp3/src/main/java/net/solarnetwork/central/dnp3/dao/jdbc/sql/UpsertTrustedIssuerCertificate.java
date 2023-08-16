/* ==================================================================
 * UpsertTrustedIssuerCertificate.java - 5/08/2023 11:59:51 am
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

package net.solarnetwork.central.dnp3.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;

/**
 * Support for INSERT ... ON CONFLICT {@link TrustedIssuerCertificate} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertTrustedIssuerCertificate implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardnp3.dnp3_ca_cert (
				  created,modified,user_id,subject_dn
				, expires,enabled,cert
			)
			VALUES (
				  ?,CAST(COALESCE(?, ?) AS TIMESTAMP WITH TIME ZONE),?,?
				, ?,?,?)
			ON CONFLICT (user_id, subject_dn) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, expires = EXCLUDED.expires
					, enabled = EXCLUDED.enabled
					, cert = EXCLUDED.cert
			""";

	private final Long userId;
	private final TrustedIssuerCertificate entity;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertTrustedIssuerCertificate(Long userId, TrustedIssuerCertificate entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : null;
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setTimestamp(++p, ts);
		stmt.setObject(++p, userId);
		stmt.setString(++p, entity.getSubjectDn());
		stmt.setTimestamp(++p, Timestamp.from(entity.getExpires()));
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setBytes(++p, entity.certificateData());
		return stmt;
	}

}
