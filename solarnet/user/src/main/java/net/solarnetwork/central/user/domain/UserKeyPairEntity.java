/* ==================================================================
 * UserKeyPairEntity.java - 22/03/2025 10:22:34 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BasicUserEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.service.CertificateService;

/**
 * A user key pair entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "userId", "key", "created", "modified" })
@JsonIgnoreProperties({ "id" })
public class UserKeyPairEntity extends BasicUserEntity<UserKeyPairEntity, UserStringCompositePK>
		implements UserKeyPair {

	@Serial
	private static final long serialVersionUID = 6598578387510820116L;

	/** The expected format of the keystore data. */
	public static final String KEYSTORE_TYPE = "pkcs12";

	/** The expected alias for the key pair in the key store. */
	public static final String ALIAS = "key";

	private final byte[] keystore;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param keystore
	 *        the keystore value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserKeyPairEntity(UserStringCompositePK id, byte[] keystore) {
		super(id);
		this.keystore = Arrays.copyOf(keystore, requireNonNullArgument(keystore, "keystore").length);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param keystore
	 *        the keystore value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserKeyPairEntity(UserStringCompositePK id, Instant created, Instant modified,
			byte[] keystore) {
		super(id, created, modified);
		this.keystore = Arrays.copyOf(keystore, requireNonNullArgument(keystore, "keystore").length);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param keystore
	 *        the keystore value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserKeyPairEntity(Long userId, String key, Instant created, Instant modified,
			byte[] keystore) {
		this(new UserStringCompositePK(userId, key), created, modified, keystore);
	}

	@Override
	public UserKeyPairEntity copyWithId(UserStringCompositePK id) {
		return new UserKeyPairEntity(id, getCreated(), getModified(), keystore);
	}

	@Override
	public boolean isSameAs(UserKeyPairEntity other) {
		return Arrays.equals(keystore, other.keystore);
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	@Override
	public String getKey() {
		var pk = getId();
		return (pk != null ? pk.getEntityId() : null);
	}

	/**
	 * Get the raw keystore value.
	 * 
	 * @return the keystore value
	 */
	public byte[] keyStoreData() {
		return Arrays.copyOf(keystore, keystore.length);
	}

	/**
	 * Load the keystore data into a {@link KeyStore}.
	 * 
	 * @param password
	 *        the password to use
	 * @return the key store, never {@code null}
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws IllegalStateException
	 *         if the keystore cannot be loaded
	 */
	public KeyStore keyStore(String password) {
		try (var in = new ByteArrayInputStream(keystore)) {
			var keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			keyStore.load(in, requireNonNullArgument(password, "password").toCharArray());
			return keyStore;
		} catch ( KeyStoreException | NoSuchAlgorithmException
				| java.security.cert.CertificateException e ) {
			throw new IllegalStateException(
					"Error loading user %d key store [%s]".formatted(getUserId(), getKey()), e);
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading user %d key store [%s]";
			} else {
				msg = "Error loading user %d key store [%s]";
			}
			throw new IllegalStateException(msg.formatted(getUserId(), getKey()), e);
		}

	}

	@Override
	public KeyPair keyPair(String password) {
		KeyStore store = keyStore(password);
		try {
			RSAPrivateCrtKey key = (RSAPrivateCrtKey) store.getKey(ALIAS,
					(password == null ? null : password.toCharArray()));
			Certificate certificate = store.getCertificate(ALIAS);
			PublicKey publicKey = null;
			if ( certificate != null ) {
				publicKey = certificate.getPublicKey();
			} else if ( key != null ) {
				RSAPublicKeySpec spec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
				publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
			}
			return new KeyPair(publicKey, key);
		} catch ( Exception e ) {
			throw new IllegalStateException("Error retrieving key pair from user %d key store [%s]"
					.formatted(getUserId(), getKey()), e);
		}
	}

	/**
	 * Create a new entity out of a {@link KeyPair}.
	 * 
	 * @param userId
	 *        the user ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param keyPair
	 *        the key pair
	 * @param password
	 *        the password to encrypt the store with
	 * @param service
	 *        the certificate service to use
	 * @return the new entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws IllegalStateException
	 *         if the key store cannot be encoded
	 */
	public static UserKeyPairEntity withKeyPair(Long userId, String key, Instant created,
			Instant modified, KeyPair keyPair, String password, CertificateService service) {
		final var cert = service.generateCertificate("cn=User %d KeyPair %s".formatted(userId, key),
				keyPair.getPublic(), keyPair.getPrivate());
		final char[] pass = requireNonNullArgument(password, "password").toCharArray();
		try (var out = new ByteArrayOutputStream()) {
			KeyStore store = KeyStore.getInstance(KEYSTORE_TYPE);
			store.load(null, null);
			store.setKeyEntry(ALIAS, keyPair.getPrivate(), pass, new Certificate[] { cert });
			store.store(out, pass);
			return new UserKeyPairEntity(userId, key, created, modified, out.toByteArray());
		} catch ( IOException | GeneralSecurityException e ) {
			throw new IllegalStateException(
					"Unable to serialize user %d key store [%s]".formatted(userId, key), e);
		}
	}

}
