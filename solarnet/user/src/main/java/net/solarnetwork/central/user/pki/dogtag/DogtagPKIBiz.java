/* ==================================================================
 * DogtagPKIBiz.java - Oct 14, 2014 6:58:50 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dogtag;

import java.math.BigInteger;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.w3c.dom.Node;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.CertificateService;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.support.XmlSupport;
import net.solarnetwork.util.CachedResult;

/**
 * Dogtag implementation of {@link NodePKIBiz}.
 *
 * <p>
 * For proper support with different Dogtag versions, you must call
 * {@link #setDogtagVersionValue(String)} to configure the expected server
 * minimum version, or call {@link #performPingTest()} to attempt to
 * automatically detect the version.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
public class DogtagPKIBiz
		implements NodePKIBiz, PingTest, SettingsChangeObserver, ServiceLifecycleObserver {

	public static final String DOGTAG_10_PROFILE_SUBMIT_PATH = "/ca/ee/ca/profileSubmit";

	public static final String DOGTAG_10_PROFILE_PROCESS_PATH = "/ca/agent/ca/profileProcess";

	public static final String DOGTAG_10_PROFILE_SUBMIT_RESPONSE_REQUEST_ID_XPATH = "/*/RequestId/text()";

	// Dogtag 10.0
	public static final String DOGTAG_10_PROFILE_ID_XPATH = "/ProfileData/id";
	public static final String DOGTAG_10_PROFILE_ENABLED_XPATH = "/ProfileData/isEnabled";

	// Dogtag 10.6
	public static final String DOGTAG_10_CERTREQ_PROFILE_ID_XPATH = "/CertEnrollmentRequest/ProfileID";

	public static final String DOGTAG_10_CERT_GET_PATH = "/ca/rest/certs/{id}";
	public static final String DOGTAG_10_CERTREG_RENEW_PATH = "/ca/rest/certrequests";
	public static final String DOGTAG_10_CERTREG_GET_PATH = "/ca/rest/certrequests/{id}";
	public static final String DOGTAG_10_CERTREQ_GET_PROFILE = "/ca/rest/certrequests/profiles/{id}";
	public static final String DOGTAG_10_AGENT_CERTREQ_GET_PATH = "/ca/rest/agent/certrequests/{id}";
	public static final String DOGTAG_10_AGENT_CERTREQ_APPROVE_PATH = "/ca/rest/agent/certrequests/{id}/approve";
	public static final String DOGTAG_10_AGENT_PROFILE_GET_PATH = "/ca/rest/agent/profiles/{id}";

	public static final String DOGTAG_10_PKI_INFO = "/pki/rest/info";
	public static final String DOGTAG_10_PKI_INFO_VERSION_XPATH = "/Info/Version";

	public static final String DOGTAG_10_AGENT_CERTREQ_REQUEST_STATUS_XPATH = "/certReviewResponse/requestStatus";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String baseUrl;
	private XmlSupport xmlSupport = new XmlSupport();
	private CertificateService certificateService;
	private RestOperations restOps;
	private String dogtagProfileId = "SolarNode";
	private String dogtagRenewalProfileId = "caManualRenewal";
	private XPathExpression csrRequestIdXPath;
	private Map<String, XPathExpression> csrInfoMapping;
	private Map<String, XPathExpression> renewalInfoMapping;
	private Map<String, XPathExpression> certDetailMapping;
	private int pingResultsCacheSeconds = 300;

	private final int[] dogtagVersion = new int[] { 0, 0, 0 };

	private final Map<String, XPathExpression> xpathCache = new HashMap<>();
	private CachedResult<PingTestResult> cachedResult;

	/**
	 * Call to setup the Dogtag client.
	 *
	 * <p>
	 * This will attempt to detect the Dogtag version, unless one has already
	 * been configured via {@link #setDogtagVersionValue(String)}.
	 * </p>
	 *
	 * @since 2.1
	 */
	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( dogtagVersion[0] == 0 ) {
			detectDogtagVersion();
		}
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

	@Override
	public String submitCSR(final X509Certificate certificate, final PrivateKey privateKey)
			throws net.solarnetwork.central.security.BasicSecurityException {
		final SecurityUser requestor = SecurityUtils.getCurrentUser();
		String csr = certificateService.generatePKCS10CertificateRequestString(certificate, privateKey);

		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>(6);
		params.add("xmlOutput", "true");
		params.add("profileId", dogtagProfileId);
		params.add("requestor_name", requestor.getDisplayName());
		params.add("requestor_email", requestor.getEmail());
		params.add("cert_request_type", "pkcs10");
		params.add("cert_request", csr);

		ResponseEntity<DOMSource> result = restOps.postForEntity(baseUrl + DOGTAG_10_PROFILE_SUBMIT_PATH,
				params, DOMSource.class);
		DOMSource xmlResult = result.getBody();
		if ( log.isDebugEnabled() ) {
			log.debug("Got XML response: {}", xmlSupport.getXmlAsString(xmlResult, true));
		}
		if ( xmlResult == null ) {
			log.error("Request for CSR request ID did not return XML result");
			throw new CertificateException(
					"No certificate request ID could be extracted from CA CSR submit response");
		}
		String certReqId = xmlSupport.extractStringFromXml(xmlResult.getNode(),
				getCsrRequestIdXPathExpression());
		if ( certReqId == null ) {
			log.error("CSR request ID not found in CA response: {}",
					xmlSupport.getXmlAsString(xmlResult, true));
			throw new CertificateException(
					"No certificate request ID could be extracted from CA CSR submit response");
		}
		return certReqId.trim();
	}

	private boolean isVersionAtLeast(int major, int minor, int patch) {
		if ( dogtagVersion[0] == 0 ) {
			return false;
		}
		return (dogtagVersion[0] > major || dogtagVersion[1] > minor || dogtagVersion[2] >= patch);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String submitRenewalRequest(X509Certificate certificate) throws BasicSecurityException {
		BigInteger serialNumber = certificate.getSerialNumber();

		Object req;
		if ( isVersionAtLeast(10, 6, 0) ) {
			Map<String, Object> params = new LinkedHashMap<>(6);
			params.put("ProfileID", dogtagRenewalProfileId);
			params.put("Renewal", "true");
			params.put("SerialNumber", serialNumber.toString());
			HttpHeaders reqHeaders = new HttpHeaders();
			reqHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
			req = new HttpEntity<>(params, reqHeaders);
		} else {
			// Dogtag 10.0
			MultiValueMap<String, Object> params = new LinkedMultiValueMap<>(6);
			params.add("profileId", dogtagRenewalProfileId);
			params.add("renewal", "true");
			params.add("serial_num", serialNumber.toString());
			req = params;
		}

		ResponseEntity<DOMSource> result = restOps.postForEntity(baseUrl + DOGTAG_10_CERTREG_RENEW_PATH,
				req, DOMSource.class);
		DOMSource xmlResult = result.getBody();
		if ( log.isDebugEnabled() ) {
			log.debug("Got XML response: {}", xmlSupport.getXmlAsString(xmlResult, true));
		}

		final DogtagCertRequestInfo requestInfo = getRenewalRequestInfo(result.getBody().getNode());

		// the request ID is the last path value of the requestURL
		final URL requestURL = requestInfo.getRequestURL();
		String requestID = null;
		if ( requestURL != null ) {
			requestID = StringUtils.getFilename(requestURL.getPath());
		}
		if ( requestID == null ) {
			log.error("Renewal request ID not found in CA response: {}",
					xmlSupport.getXmlAsString(xmlResult, true));
			throw new CertificateException(
					"No certificate request ID could be extracted from CA renewal submit response");
		}
		return requestID;
	}

	@Override
	public X509Certificate[] approveCSR(String requestID) {
		if ( requestID == null ) {
			throw new IllegalArgumentException("The request ID argument must be provided.");
		}

		// get the agent details for the CSR (required to approve)
		ResponseEntity<DOMSource> result = restOps
				.getForEntity(baseUrl + DOGTAG_10_AGENT_CERTREQ_GET_PATH, DOMSource.class, requestID);
		if ( log.isDebugEnabled() ) {
			log.debug("Got agent cert req details: {}",
					xmlSupport.getXmlAsString(result.getBody(), true));
		}

		// check if the request is already complete
		String reqStatus = null;
		if ( result.getBody() != null ) {
			reqStatus = xmlSupport.extractStringFromXml(result.getBody().getNode(),
					xpathForString(DOGTAG_10_AGENT_CERTREQ_REQUEST_STATUS_XPATH));
		}

		if ( reqStatus == null || "pending".equalsIgnoreCase(reqStatus) ) {
			// approve the pending CSR
			restOps.postForEntity(baseUrl + DOGTAG_10_AGENT_CERTREQ_APPROVE_PATH, result.getBody(), null,
					requestID);
		}

		// get the CSR details, which will include our cert URL
		result = restOps.getForEntity(baseUrl + DOGTAG_10_CERTREG_GET_PATH, DOMSource.class, requestID);
		final DogtagCertRequestInfo info = getCertRequestInfo(result.getBody().getNode());
		if ( info.getCertURL() == null ) {
			log.warn("Expected to find certURL for approved CSR {}; req info: {}", requestID,
					xmlSupport.getXmlAsString(result.getBody(), false));
			throw new CertificateException("URL not available for request " + requestID);
		}

		// in Dogtag 10.0 for some reason the certURL returned is missing the "/certs" path element, so we have to insert that
		String certURL;
		if ( !info.getCertURL().getPath().contains("/certs/") ) {
			certURL = info.getCertURL().toExternalForm().replaceFirst("(/[^/]+)$", "/certs$1");
		} else {
			certURL = info.getCertURL().toExternalForm();
		}
		result = restOps.getForEntity(certURL, DOMSource.class);
		final DogtagCertificateData certData = getCertData(result.getBody().getNode());

		return certificateService.parsePKCS7CertificateChainString(certData.getPkcs7Chain());
	}

	private DogtagCertRequestInfo getCertRequestInfo(Node node) {
		DogtagCertRequestInfo info = new DogtagCertRequestInfo();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(info);
		xmlSupport.extractBeanDataFromXml(bean, node, getCsrInfoMapping());
		return info;
	}

	private DogtagCertRequestInfo getRenewalRequestInfo(Node node) {
		DogtagCertRequestInfo info = new DogtagCertRequestInfo();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(info);
		xmlSupport.extractBeanDataFromXml(bean, node, getRenewalInfoMapping());
		return info;
	}

	private DogtagCertificateData getCertData(Node node) {
		DogtagCertificateData data = new DogtagCertificateData();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
		xmlSupport.extractBeanDataFromXml(bean, node, getCertDetailMapping());
		return data;
	}

	//	<CertRequestInfos>
	//	  <CertRequestInfo>
	//	    <requestType>renewal</requestType>
	//	    <requestStatus>pending</requestStatus>
	//	    <requestURL>https://ca.solarnetworkdev.net:8443/ca/rest/73</requestURL>
	//	    <certRequestType>pkcs10</certRequestType>
	//	    <operationResult>success</operationResult>
	//	  </CertRequestInfo>
	//	</CertRequestInfos>
	private Map<String, XPathExpression> getRenewalInfoMapping() {
		Map<String, XPathExpression> result = this.renewalInfoMapping;
		if ( result == null ) {
			Map<String, String> map = new LinkedHashMap<>(3);
			map.put("requestURL", "//CertRequestInfo[1]/requestURL");
			map.put("requestStatus", "//CertRequestInfo[1]/requestStatus");
			result = xmlSupport.getXPathExpressionMap(map);
			this.renewalInfoMapping = result;
		}
		return result;
	}

	private Map<String, XPathExpression> getCsrInfoMapping() {
		Map<String, XPathExpression> result = this.csrInfoMapping;
		if ( result == null ) {
			Map<String, String> map = new LinkedHashMap<>(3);
			map.put("certURL", "/CertRequestInfo/certURL");
			map.put("requestURL", "/CertRequestInfo/certURL");
			map.put("requestStatus", "/CertRequestInfo/requestStatus");
			result = xmlSupport.getXPathExpressionMap(map);
			this.csrInfoMapping = result;
		}
		return result;
	}

	private Map<String, XPathExpression> getCertDetailMapping() {
		Map<String, XPathExpression> result = this.certDetailMapping;
		if ( result == null ) {
			Map<String, String> map = new LinkedHashMap<>(3);
			map.put("id", "/CertData/@id");
			map.put("pkcs7Chain", "/CertData/PKCS7CertChain");
			result = xmlSupport.getXPathExpressionMap(map);
			this.certDetailMapping = result;
		}
		return result;
	}

	private XPathExpression getCsrRequestIdXPathExpression() {
		if ( csrRequestIdXPath == null ) {
			setCsrRequestIdXPath(DOGTAG_10_PROFILE_SUBMIT_RESPONSE_REQUEST_ID_XPATH);
		}
		return this.csrRequestIdXPath;
	}

	public void setCsrRequestIdXPath(String csrRequestIdXPath) {
		try {
			this.csrRequestIdXPath = xmlSupport.getXPathExpression(csrRequestIdXPath);
		} catch ( XPathExpressionException e ) {
			throw new IllegalArgumentException(
					"The CSR result ID XPath [" + csrRequestIdXPath + "] is not valid", e);
		}
	}

	// PingTest support

	public String getBaseUrl() {
		return baseUrl;
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "CA Service";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 10000;
	}

	private XPathExpression xpathForString(String xpathString) {
		XPathExpression result = xpathCache.get(xpathString);
		if ( result == null ) {
			try {
				result = xmlSupport.getXPathExpression(xpathString);
				xpathCache.put(xpathString, result);
			} catch ( XPathExpressionException e ) {
				throw new IllegalArgumentException("The XPath [" + xpathString + "] is not valid", e);
			}
		}
		return result;
	}

	@Override
	public PingTestResult performPingTest() throws Exception {
		if ( cachedResult != null && cachedResult.isValid() ) {
			return cachedResult.getResult();
		}
		if ( restOps == null ) {
			return new PingTestResult(false, "RestOperations not configured.");
		}
		if ( dogtagProfileId == null ) {
			return new PingTestResult(false, "Profile ID not configured.");
		}

		if ( dogtagVersion[0] == 0 ) {
			// try to detect version of server
			detectDogtagVersion();
		}

		PingTestResult result;
		ResponseEntity<DOMSource> response = null;
		boolean certReqMethod = true;
		try {
			response = restOps.getForEntity(baseUrl + DOGTAG_10_CERTREQ_GET_PROFILE, DOMSource.class,
					dogtagProfileId);
		} catch ( HttpClientErrorException e ) {
			if ( e.getStatusCode() == HttpStatus.NOT_FOUND ) {
				try {
					certReqMethod = false;
					response = restOps.getForEntity(baseUrl + DOGTAG_10_AGENT_PROFILE_GET_PATH,
							DOMSource.class, dogtagProfileId);
				} catch ( HttpClientErrorException e2 ) {
					log.warn("Unable to get Dogtag profile {}: {}", dogtagProfileId, e2.getMessage());
				}
			}
		}
		if ( response == null ) {
			result = new PingTestResult(false, "HTTP response not available");
		} else if ( response.getStatusCode() != HttpStatus.OK ) {
			result = new PingTestResult(false, "HTTP status not 200: " + response.getStatusCode());
		} else if ( !response.hasBody() ) {
			result = new PingTestResult(false, "HTTP response has no content");
		} else {
			Node doc = response.getBody().getNode();
			boolean enabled;
			String resultId;
			if ( certReqMethod ) {
				resultId = xmlSupport.extractStringFromXml(doc,
						xpathForString(DOGTAG_10_CERTREQ_PROFILE_ID_XPATH));
				enabled = true;
			} else {
				resultId = xmlSupport.extractStringFromXml(doc,
						xpathForString(DOGTAG_10_PROFILE_ID_XPATH));
				enabled = "true".equalsIgnoreCase(xmlSupport.extractStringFromXml(doc,
						xpathForString(DOGTAG_10_PROFILE_ENABLED_XPATH)));
			}
			if ( !dogtagProfileId.equals(resultId) ) {
				result = new PingTestResult(false, "Profile ID mismatch. Expected [" + dogtagProfileId
						+ "] but found [" + resultId + "].");
			} else if ( !enabled ) {
				result = new PingTestResult(false, "Profile " + dogtagProfileId + " is disabled.");
			} else {
				result = new PingTestResult(true, "Profile " + dogtagProfileId + " available.");
			}
		}
		CachedResult<PingTestResult> cached = new CachedResult<>(result, pingResultsCacheSeconds,
				TimeUnit.SECONDS);
		cachedResult = cached;
		return result;
	}

	private void detectDogtagVersion() {
		try {
			ResponseEntity<DOMSource> response = restOps.getForEntity(baseUrl + DOGTAG_10_PKI_INFO,
					DOMSource.class);
			if ( response.getStatusCode() == HttpStatus.OK ) {
				String version = xmlSupport.extractStringFromXml(response.getBody().getNode(),
						xpathForString(DOGTAG_10_PKI_INFO_VERSION_XPATH));
				if ( version != null && !version.isEmpty() ) {
					log.info("Detected Dogtag server version {}", version);
					setDogtagVersionValue(version);
				}
			}
		} catch ( HttpClientErrorException e ) {
			log.info("Unable to detect Dogtag server version via {}: {}", DOGTAG_10_PKI_INFO,
					e.getMessage());
		} catch ( RestClientException e ) {
			log.error("Error detecting Dogtag server version via {}: {}", DOGTAG_10_PKI_INFO,
					e.toString());
		}
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public CertificateService getCertificateService() {
		return certificateService;
	}

	public void setCertificateService(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

	public RestOperations getRestOps() {
		return restOps;
	}

	public void setRestOps(RestOperations restOps) {
		this.restOps = restOps;
	}

	public String getDogtagProfileId() {
		return dogtagProfileId;
	}

	public void setDogtagProfileId(String dogtagProfileId) {
		this.dogtagProfileId = dogtagProfileId;
	}

	public XmlSupport getXmlSupport() {
		return xmlSupport;
	}

	public void setXmlSupport(XmlSupport xmlSupport) {
		this.xmlSupport = xmlSupport;
	}

	public int getPingResultsCacheSeconds() {
		return pingResultsCacheSeconds;
	}

	public void setPingResultsCacheSeconds(int pingResultsCacheSeconds) {
		this.pingResultsCacheSeconds = pingResultsCacheSeconds;
	}

	public String getDogtagRenewalProfileId() {
		return dogtagRenewalProfileId;
	}

	public void setDogtagRenewalProfileId(String dogtagRenewalProfileId) {
		this.dogtagRenewalProfileId = dogtagRenewalProfileId;
	}

	/**
	 * Set the Dogtag server version.
	 *
	 * <p>
	 * The version will be automatically detected when
	 * {@link #performPingTest()} is invoked, if not configured manually here.
	 * </p>
	 *
	 * @param version
	 *        the version string, which must be in the form
	 *        <code>major.minor.patch</code> where <em>major</em>,
	 *        <em>minor</em>, and <em>patch</em> are integers
	 * @since 1.3
	 */
	public void setDogtagVersionValue(String version) {
		if ( version == null || version.isEmpty() || !version.matches("\\d+\\.\\d+\\.\\d+") ) {
			return;
		}
		String[] tokens = StringUtils.delimitedListToStringArray(version, ".");
		try {
			for ( int i = 0; i < 3 && i < tokens.length; i++ ) {
				dogtagVersion[i] = Integer.parseInt(tokens[i]);
			}
		} catch ( NumberFormatException e ) {
			// ignore
		}
	}

}
