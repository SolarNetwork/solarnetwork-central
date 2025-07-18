/* ==================================================================
 * UserNodeCertificate.java - Nov 29, 2012 8:19:09 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.service.CertificateException;

/**
 * A user node certificate. The certificate is expected to be in X.509 format.
 *
 * @author matt
 * @version 3.0
 */
@JsonIgnoreProperties({ "id", "node", "user" })
public class UserNodeCertificate extends BaseObjectEntity<UserNodePK>
		implements UserRelatedEntity<UserNodePK> {

	@Serial
	private static final long serialVersionUID = 3070315335910395052L;

	/** The expected format of the keystore data. */
	public static final String KEYSTORE_TYPE = "pkcs12";

	/** The alias of the node certificate in the keystore. */
	public static final String KEYSTORE_NODE_ALIAS = "node";

	private byte[] keystoreData;
	private UserNodeCertificateStatus status;
	private String requestId;

	private User user;
	private SolarNode node;

	public UserNodeCertificate() {
		super();
		setId(new UserNodePK());
	}

	/**
	 * Get the node certificate from a keystore. The certificate is expected to
	 * be available on the {@link #KEYSTORE_NODE_ALIAS} alias.
	 *
	 * @param keyStore
	 *        the keystore
	 * @return the certificate, or <em>null</em> if not available
	 */
	public X509Certificate getNodeCertificate(KeyStore keyStore) {
		X509Certificate nodeCert;
		try {
			nodeCert = (X509Certificate) keyStore.getCertificate(KEYSTORE_NODE_ALIAS);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}
		return nodeCert;
	}

	/**
	 * Get the node certificate chain from a keystore. The certificate is
	 * expected to be available on the {@link #KEYSTORE_NODE_ALIAS} alias.
	 *
	 * @param keyStore
	 *        the keystore
	 * @return the certificate chain, or <em>null</em> if not available
	 */
	public X509Certificate[] getNodeCertificateChain(KeyStore keyStore) {
		Certificate[] chain;
		try {
			chain = keyStore.getCertificateChain(KEYSTORE_NODE_ALIAS);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}
		if ( chain == null || chain.length < 1 ) {
			return null;
		}
		X509Certificate[] x509Chain = new X509Certificate[chain.length];
		for ( int i = 0; i < chain.length; i++ ) {
			assert chain[i] instanceof X509Certificate;
			x509Chain[i] = (X509Certificate) chain[i];
		}
		return x509Chain;
	}

	/**
	 * Open the key store from {@link #getKeystoreData()}.
	 *
	 * @param password
	 *        the password to use to open, or <em>null</em> for no password
	 * @return the KeyStore
	 */
	public KeyStore getKeyStore(String password) {
		KeyStore keyStore;
		InputStream in = null;
		if ( keystoreData != null ) {
			in = new ByteArrayInputStream(keystoreData);
		}
		try {
			keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			keyStore.load(in, (password == null ? null : password.toCharArray()));
			return keyStore;
		} catch ( KeyStoreException | NoSuchAlgorithmException
				| java.security.cert.CertificateException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store";
			} else {
				msg = "Error loading certificate key store";
			}
			throw new CertificateException(msg, e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
		}
	}

	/**
	 * Convenience getter for {@link UserNodePK#getNodeId()}.
	 *
	 * @return the nodeId
	 */
	public Long getNodeId() {
		UserNodePK id = getId();
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for {@link UserNodePK#setNodeId(Long)}.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		UserNodePK id = getId();
		if ( id == null ) {
			id = new UserNodePK();
			setId(id);
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link UserNodePK#getUserId()}.
	 *
	 * @return the userId
	 */
	@Override
	public Long getUserId() {
		UserNodePK id = getId();
		return (id == null ? null : id.getUserId());
	}

	/**
	 * Convenience setter for {@link UserNodePK#setUserId(Long)}.
	 *
	 * @param userId
	 *        the userId to set
	 */
	public void setUserId(Long userId) {
		UserNodePK id = getId();
		if ( id == null ) {
			id = new UserNodePK();
			setId(id);
		}
		id.setUserId(userId);
	}

	@JsonIgnore
	@SerializeIgnore
	@Override
	public UserNodePK getId() {
		return super.getId();
	}

	@Override
	public String toString() {
		return "UserNodeCertificate{" + getId() + "}";
	}

	@JsonIgnore
	@SerializeIgnore
	public byte[] getKeystoreData() {
		return keystoreData;
	}

	public void setKeystoreData(byte[] keystoreData) {
		this.keystoreData = keystoreData;
	}

	public UserNodeCertificateStatus getStatus() {
		return status;
	}

	public void setStatus(UserNodeCertificateStatus status) {
		this.status = status;
	}

	@JsonIgnore
	@SerializeIgnore
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@JsonIgnore
	@SerializeIgnore
	public SolarNode getNode() {
		return node;
	}

	public void setNode(SolarNode node) {
		this.node = node;
	}

	/**
	 * Get an external certificate request ID, to be used when a certificate
	 * status is pending.
	 *
	 * @return the request ID
	 * @since 1.1
	 */
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

}
