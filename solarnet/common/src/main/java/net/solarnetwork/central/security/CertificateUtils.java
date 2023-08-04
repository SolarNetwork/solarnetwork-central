/* ==================================================================
 * CertificateUtils.java - 3/08/2023 11:29:23 am
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

package net.solarnetwork.central.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.security.auth.x500.X500Principal.RFC2253;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import net.solarnetwork.service.CertificateException;

/**
 * Certificate utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CertificateUtils {

	/** The RFC 822 Subject Alternative Name ID. */
	public static final Integer RFC_822_SAN_ID = 1;

	/** The {@code emailAddress} OID. */
	public static final String EMAIL_ADDRESS_OID = "1.2.840.113549.1.9.1";

	private static final Map<String, String> CANONICAL_DN_MAPPING;
	static {
		Map<String, String> m = new HashMap<>(4);
		m.put("1.2.840.113549.1.9.1", "emailAddress");
		CANONICAL_DN_MAPPING = Collections.unmodifiableMap(m);
	}

	/**
	 * Extract the first available RFC 822 (email) value from the Subject
	 * Alternative Name e
	 * 
	 * @param cert
	 *        the certificate to extract the email from
	 * @return the extracted email, or {@literal null} if none available
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws CertificateParsingException
	 *         if an error parsing the subject alternative names occurs
	 */
	public static String emailSubjectAlternativeName(X509Certificate cert)
			throws CertificateParsingException {
		Collection<List<?>> alts = requireNonNullArgument(cert, "cert").getSubjectAlternativeNames();
		if ( alts == null ) {
			return null;
		}
		for ( List<?> entry : alts ) {
			if ( entry.size() > 1 && RFC_822_SAN_ID.equals(entry.get(0)) ) {
				Object val = entry.get(1);
				if ( val != null ) {
					return val.toString();
				}
			}
		}
		return null;
	}

	/**
	 * Get the canonical subject DN value of a certificate.
	 * 
	 * @param cert
	 *        the certificate to extract the canonical subject DN value from
	 * @return the canonical subject DN
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static String canonicalSubjectDn(X509Certificate cert) {
		return requireNonNullArgument(cert, "cert").getSubjectX500Principal().getName(RFC2253,
				CANONICAL_DN_MAPPING);
	}

	/**
	 * Parse PEM certificate data.
	 * 
	 * @param pemData
	 *        the PEM data to parse
	 * @return the parsed certificates
	 * @throws CertificateException
	 *         if any error occurs
	 */
	public static X509Certificate[] parsePemCertificates(Reader pemData) throws CertificateException {
		List<X509Certificate> results = new ArrayList<>(3);
		try (PemReader reader = new PemReader(pemData)) {
			PemObject pemObj;
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			while ( (pemObj = reader.readPemObject()) != null ) {
				Collection<? extends Certificate> certs = cf
						.generateCertificates(new ByteArrayInputStream(pemObj.getContent()));
				for ( Certificate c : certs ) {
					if ( c instanceof X509Certificate x509 ) {
						results.add(x509);
					}
				}
			}
		} catch ( IOException e ) {
			throw new CertificateException("Error reading certificate", e);
		} catch ( java.security.cert.CertificateException e ) {
			throw new CertificateException("Error loading CertificateFactory", e);
		}
		return results.toArray(X509Certificate[]::new);
	}

	/**
	 * Create a new key store from TLS server settings.
	 * 
	 * @param certificatePath
	 *        the path to the PEM encoded certificate file
	 * @param certificateKey
	 *        the path to the PEM encoded, unencrypted private key
	 * @param alias
	 *        the key store alias to use for the certificate
	 * @return the key store, or {@literal null} if no settings are available
	 * @throws net.solarnetwork.service.CertificateException
	 *         if an error occurs initializing the key store
	 */
	public static KeyStore serverKeyStore(Path certificatePath, Path certificateKey, String alias)
			throws net.solarnetwork.service.CertificateException {
		if ( certificatePath == null || certificateKey == null || alias == null ) {
			return null;
		}
		X509Certificate[] certs;
		try {
			certs = parsePemCertificates(Files.newBufferedReader(certificatePath, UTF_8));
		} catch ( IOException e ) {
			throw new net.solarnetwork.service.CertificateException(
					"Error reading server certificate path [%s]: %s".formatted(certificatePath,
							e.toString()));
		}

		try (Reader reader = Files.newBufferedReader(certificateKey);
				PemReader pemReader = new PemReader(reader)) {
			KeyStore result = KeyStore.getInstance(KeyStore.getDefaultType());
			result.load(null);

			// load private key, and associate with certificate chain
			KeyFactory factory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = null;
			PemObject pemObject = pemReader.readPemObject();
			byte[] content = pemObject.getContent();
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
			privateKey = factory.generatePrivate(privKeySpec);
			result.setKeyEntry(alias, privateKey, new char[0], certs);
			return result;
		} catch ( Exception e ) {
			throw new net.solarnetwork.service.CertificateException(
					"Error initializing certificate key store.", e);
		}
	}

	/**
	 * Validate a certificate chain.
	 * 
	 * @param trustStore
	 *        the trust store containing all available trusted CA certificates
	 * @param chain
	 *        the certificate chain to validate
	 * @return the validation result, if successful
	 * @throws net.solarnetwork.service.CertificateException
	 *         if validation fails for any reason
	 */
	public static PKIXCertPathValidatorResult validateCertificateChain(KeyStore trustStore,
			X509Certificate[] chain) throws net.solarnetwork.service.CertificateException {
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			CertPath path = cf.generateCertPath(Arrays.asList(chain));
			PKIXParameters pkixParams = new PKIXParameters(trustStore);
			pkixParams.setRevocationEnabled(false); // possibly enable in future
			CertPathValidator validator = CertPathValidator.getInstance("PKIX");
			CertPathValidatorResult result = validator.validate(path, pkixParams);
			return (PKIXCertPathValidatorResult) result;
		} catch ( Exception e ) {
			throw new net.solarnetwork.service.CertificateException(e);
		}
	}

}
