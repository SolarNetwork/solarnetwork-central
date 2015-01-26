/* ==================================================================
 * DevNodePKIBiz.java - Jan 23, 2015 5:31:54 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dev;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.UUID;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.support.CertificateException;
import net.solarnetwork.support.CertificateService;
import net.solarnetwork.support.CertificationAuthorityService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Developer implementation of {@link NodePKIBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DevNodePKIBiz implements NodePKIBiz {

	private static final String DIR_REQUESTS = "requests";
	private static final String PASSWORD_FILE = "secret";

	private CertificateService certificateService;
	private CertificationAuthorityService caService;
	private File baseDir = new File("var/DeveloperCA");
	private int keySize = 2048;
	private String caDN = "CN=Developer CA, O=SolarDev";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public String submitCSR(X509Certificate certificate, PrivateKey privateKey) throws SecurityException {
		final String csr = certificateService.generatePKCS10CertificateRequestString(certificate,
				privateKey);
		final String csrID = DigestUtils.sha256Hex(csr);
		final File csrDir = new File(baseDir, DIR_REQUESTS);
		if ( !csrDir.isDirectory() ) {
			csrDir.mkdirs();
		}
		final File csrFile = new File(csrDir, csrID);
		try {
			FileCopyUtils.copy(csr.getBytes("US-ASCII"), csrFile);
		} catch ( UnsupportedEncodingException e ) {
			throw new CertificateException("Error saving CSR: " + e.getMessage());
		} catch ( IOException e ) {
			log.error("Error saving CSR to [{}]", csrFile, e);
			throw new CertificateException("Error saving CSR data");
		}
		return csrID;
	}

	@Override
	public X509Certificate[] approveCSR(String requestID) {
		final File csrFile = new File(new File(baseDir, DIR_REQUESTS), requestID);
		if ( !csrFile.canRead() ) {
			throw new CertificateException("CSR " + requestID + " not found.");
		}
		String csr;
		try {
			csr = new String(FileCopyUtils.copyToByteArray(csrFile), "US-ASCII");
		} catch ( UnsupportedEncodingException e ) {
			throw new CertificateException("Error reading CSR: " + e.getMessage());
		} catch ( IOException e ) {
			log.error("Error reading CSR to [{}]", csrFile, e);
			throw new CertificateException("Error reading CSR data");
		}

		final String caAlias = "ca";
		final KeyStore keyStore = loadKeyStore();
		X509Certificate caCert = getCertificate(keyStore, caAlias);
		if ( caCert == null ) {
			// generate a new CA
			caCert = createCACertificate(keyStore, caDN, caAlias);
		}
		PrivateKey caPrivateKey = getPrivateKey(keyStore, caAlias);
		X509Certificate signedCert = caService.signCertificate(csr, caCert, caPrivateKey);
		return new X509Certificate[] { caCert, signedCert };
	}

	@Override
	public X509Certificate generateCertificate(String dn, PublicKey publicKey, PrivateKey privateKey)
			throws CertificateException {
		return certificateService.generateCertificate(dn, publicKey, privateKey);
	}

	@Override
	public String generatePKCS10CertificateRequestString(X509Certificate cert, PrivateKey privateKey)
			throws CertificateException {
		return certificateService.generatePKCS10CertificateRequestString(cert, privateKey);
	}

	@Override
	public String generatePKCS7CertificateChainString(X509Certificate[] chain)
			throws CertificateException {
		return certificateService.generatePKCS7CertificateChainString(chain);
	}

	@Override
	public X509Certificate[] parsePKCS7CertificateChainString(String pem) throws CertificateException {
		return certificateService.parsePKCS7CertificateChainString(pem);
	}

	private X509Certificate getCertificate(KeyStore keyStore, String alias) {
		try {
			return (X509Certificate) keyStore.getCertificate(alias);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}
	}

	private PrivateKey getPrivateKey(KeyStore keyStore, String alias) {
		try {
			return (PrivateKey) keyStore.getKey(alias, getKeyStorePassword().toCharArray());
		} catch ( UnrecoverableKeyException e ) {
			throw new CertificateException("Error opening node certificate", e);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}
	}

	private X509Certificate createCACertificate(KeyStore keyStore, String dn, String alias) {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keySize, new SecureRandom());
			KeyPair keypair = keyGen.generateKeyPair();
			PublicKey publicKey = keypair.getPublic();
			PrivateKey privateKey = keypair.getPrivate();

			Certificate cert = caService.generateCertificationAuthorityCertificate(dn, publicKey,
					privateKey);
			keyStore.setKeyEntry(alias, privateKey, getKeyStorePassword().toCharArray(),
					new Certificate[] { cert });
			saveKeyStore(keyStore);
			return (X509Certificate) cert;
		} catch ( NoSuchAlgorithmException e ) {
			throw new CertificateException("Error setting up node key pair", e);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error setting up node key pair", e);
		}
	}

	private String getKeyStorePassword() {
		File pwFile = new File(baseDir, PASSWORD_FILE);
		if ( pwFile.canRead() ) {
			try {
				return new String(FileCopyUtils.copyToByteArray(pwFile), "US-ASCII");
			} catch ( UnsupportedEncodingException e ) {
				throw new CertificateException("Error decoding keystore secret file "
						+ pwFile.getAbsolutePath(), e);
			} catch ( IOException e ) {
				throw new CertificateException("Error reading keystore secret file"
						+ pwFile.getAbsolutePath(), e);
			}
		}

		// generate new random password
		String pw = UUID.randomUUID().toString();
		if ( !baseDir.isDirectory() ) {
			baseDir.mkdirs();
		}
		try {
			FileCopyUtils.copy(pw.getBytes(), pwFile);
		} catch ( IOException e ) {
			throw new CertificateException("Unable to save keystore secret file "
					+ pwFile.getAbsolutePath(), e);
		}
		return pw;
	}

	private File getKeyStoreFile() {
		return new File(baseDir, "ca.jks");
	}

	private synchronized KeyStore loadKeyStore() {
		File ksFile = getKeyStoreFile();
		InputStream in = null;
		String passwd = getKeyStorePassword();
		try {
			if ( ksFile.isFile() ) {
				in = new BufferedInputStream(new FileInputStream(ksFile));
			}
			return loadKeyStore(KeyStore.getDefaultType(), in, passwd);
		} catch ( IOException e ) {
			throw new CertificateException("Error opening file " + ksFile.getAbsolutePath(), e);
		}
	}

	private KeyStore loadKeyStore(String type, InputStream in, String password) {
		if ( password == null ) {
			password = "";
		}
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(type);
			keyStore.load(in, password.toCharArray());
			return keyStore;
		} catch ( GeneralSecurityException e ) {
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

	private synchronized void saveKeyStore(KeyStore keyStore) {
		if ( keyStore == null ) {
			return;
		}
		final File ksFile = getKeyStoreFile();
		final File ksDir = ksFile.getParentFile();
		if ( !ksDir.isDirectory() && !ksDir.mkdirs() ) {
			throw new RuntimeException("Unable to create KeyStore directory: " + ksFile.getParent());
		}
		OutputStream out = null;
		try {
			String passwd = getKeyStorePassword();
			out = new BufferedOutputStream(new FileOutputStream(ksFile));
			keyStore.store(out, passwd.toCharArray());
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error saving certificate key store", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new CertificateException("Error saving certificate key store", e);
		} catch ( java.security.cert.CertificateException e ) {
			throw new CertificateException("Error saving certificate key store", e);
		} catch ( IOException e ) {
			throw new CertificateException("Error saving certificate key store", e);
		} finally {
			if ( out != null ) {
				try {
					out.flush();
					out.close();
				} catch ( IOException e ) {
					throw new CertificateException("Error closing KeyStore file: " + ksFile.getPath(), e);
				}
			}
		}
	}

	public void setCertificateService(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	public void setCaService(CertificationAuthorityService caService) {
		this.caService = caService;
	}

	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

	public void setCaDN(String caDN) {
		this.caDN = caDN;
	}

}
