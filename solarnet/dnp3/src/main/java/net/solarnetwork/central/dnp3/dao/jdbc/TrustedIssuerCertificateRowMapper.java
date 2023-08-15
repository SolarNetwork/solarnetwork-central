/* ==================================================================
 * TrustedIssuerCertificateRowMapper.java - 5/08/2023 5:36:02 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.service.CertificateException;

/**
 * Row mapper for {@link TrustedIssuerCertificate} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>subject_dn (TEXT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>user_id (BIGINT)</li>
 * <li>expires (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>cert (BYTE ARRAY)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class TrustedIssuerCertificateRowMapper implements RowMapper<TrustedIssuerCertificate> {

	/** A default instance. */
	public static final RowMapper<TrustedIssuerCertificate> INSTANCE = new TrustedIssuerCertificateRowMapper();

	private final CertificateFactory certificateFactory;
	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public TrustedIssuerCertificateRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 * 
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public TrustedIssuerCertificateRowMapper(int columnOffset) {
		this(columnOffset, CertificateUtils.x509CertificateFactory());
	}

	/**
	 * Constructor.
	 * 
	 * @param columnOffset
	 *        a column offset to apply
	 * @param certificateFactory
	 *        the certificate factory, must be X.509 type
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TrustedIssuerCertificateRowMapper(int columnOffset, CertificateFactory certificateFactory) {
		super();
		this.columnOffset = columnOffset;
		this.certificateFactory = requireNonNullArgument(certificateFactory, "certificateFactory");
		if ( !"X.509".equals(certificateFactory.getType()) ) {
			throw new IllegalArgumentException("CertificateFactory must be of type X.509");
		}
	}

	@Override
	public TrustedIssuerCertificate mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		String subjectDn = rs.getString(++p);
		Timestamp ts = rs.getTimestamp(++p);
		Timestamp mod = rs.getTimestamp(++p);
		Long userId = rs.getLong(++p);
		TrustedIssuerCertificate conf = new TrustedIssuerCertificate(userId, subjectDn, ts.toInstant());
		conf.setModified(mod.toInstant());
		++p; // skip expires
		conf.setEnabled(rs.getBoolean(++p));
		try {
			conf.setCertificate((X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(rs.getBytes(++p))));
		} catch ( java.security.cert.CertificateException e ) {
			throw new CertificateException("Error parsing certificate", e);
		}
		return conf;
	}

}
