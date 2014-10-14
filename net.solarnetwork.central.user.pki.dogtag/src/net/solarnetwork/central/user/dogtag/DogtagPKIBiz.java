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

package net.solarnetwork.central.user.dogtag;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.support.CertificateException;
import net.solarnetwork.support.CertificateService;
import net.solarnetwork.support.XmlSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.w3c.dom.Node;

/**
 * Dogtag implementation of {@link NodePKIBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DogtagPKIBiz implements NodePKIBiz {

	public static final String DOGTAG_10_PROFILE_SUBMIT_PATH = "/ca/ee/ca/profileSubmit";

	public static final String DOGTAG_10_PROFILE_PROCESS_PATH = "/ca/agent/ca/profileProcess";

	public static final String DOGTAG_10_PROFILE_SUBMIT_RESPONSE_REQUEST_ID_XPATH = "/*/RequestId/text()";

	public static final String DOGTAG_10_CERT_GET_PATH = "/ca/rest/certs/{id}";
	public static final String DOGTAG_10_CERTREG_GET_PATH = "/ca/rest/certrequests/{id}";
	public static final String DOGTAG_10_AGENT_CERTREQ_GET_PATH = "/ca/rest/agent/certrequests/{id}";
	public static final String DOGTAG_10_AGENT_CERTREQ_APPROVE_PATH = "/ca/rest/agent/certrequests/{id}/approve";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String baseUrl;
	private XmlSupport xmlSupport = new XmlSupport();
	private CertificateService certificateService;
	private RestOperations restOps;
	private String dogtagProfileId = "SolarNode";
	private XPathExpression csrRequestIdXPath;
	private Map<String, XPathExpression> csrInfoMapping;
	private Map<String, XPathExpression> certDetailMapping;
	private Map<String, XPathExpression> agentCsrInfoMapping;

	@Override
	public String submitCSR(final X509Certificate certificate, final PrivateKey privateKey)
			throws net.solarnetwork.central.security.SecurityException {
		final SecurityUser requestor = SecurityUtils.getCurrentUser();
		String csr = certificateService.generatePKCS10CertificateRequestString(certificate, privateKey);

		MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>(6);
		params.add("xmlOutput", "true");
		params.add("profileId", dogtagProfileId);
		params.add("requestor_name", requestor.getDisplayName());
		params.add("requestor_email", requestor.getEmail());
		params.add("cert_request_type", "pkcs10");
		params.add("cert_request", csr);

		ResponseEntity<DOMSource> result = restOps.postForEntity(
				baseUrl + DOGTAG_10_PROFILE_SUBMIT_PATH, params, DOMSource.class);
		DOMSource xmlResult = result.getBody();
		if ( log.isDebugEnabled() ) {
			log.debug("Got XML response: {}", xmlSupport.getXmlAsString(xmlResult, true));
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

	@Override
	public X509Certificate[] approveCSR(String requestID) {
		if ( requestID == null ) {
			throw new IllegalArgumentException("The request ID argument must be provided.");
		}

		final SecurityUser actor = SecurityUtils.getCurrentUser();

		// get the agent details for the CSR (required to approve)
		ResponseEntity<DOMSource> result = restOps.getForEntity(baseUrl
				+ DOGTAG_10_AGENT_CERTREQ_GET_PATH, DOMSource.class, requestID);
		if ( log.isDebugEnabled() ) {
			log.debug("Got agent cert req details: {}",
					xmlSupport.getXmlAsString(result.getBody(), true));
		}

		final DogtagAgentCertRequestInfo agentInfo = getAgentCsrInfo(result.getBody().getNode());
		if ( actor.getEmail().equalsIgnoreCase(agentInfo.getRequestorEmail()) == false ) {
			log.warn("Access DENIED to CSR request {} for user {}; email does not match", requestID,
					actor.getUserId());
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, requestID);
		}

		// approve the CSR
		restOps.postForEntity(baseUrl + DOGTAG_10_AGENT_CERTREQ_APPROVE_PATH, result.getBody(), null,
				requestID);

		// get the CSR details, which will include our cert URL
		result = restOps.getForEntity(baseUrl + DOGTAG_10_CERTREG_GET_PATH, DOMSource.class, requestID);
		final DogtagCertRequestInfo info = getCertRequestInfo(result.getBody().getNode());
		if ( info.getCertURL() == null ) {
			log.warn("Expected to find certURL for approved CSR {}; req info: {}", requestID,
					xmlSupport.getXmlAsString(result.getBody(), false));
			throw new CertificateException("URL not available for request " + requestID);
		}

		// for some reason the certURL returned is missing the "/certs" path element, so we have to insert that
		String certURL = info.getCertURL().toExternalForm().replaceFirst("(/[^/]+)$", "/certs$1");
		result = restOps.getForEntity(certURL, DOMSource.class);
		final DogtagCertificateData certData = getCertData(result.getBody().getNode());

		return certificateService.parsePKCS7CertificateChainString(certData.getPkcs7Chain());
	}

	private DogtagAgentCertRequestInfo getAgentCsrInfo(Node node) {
		DogtagAgentCertRequestInfo info = new DogtagAgentCertRequestInfo();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(info);
		xmlSupport.extractBeanDataFromXml(bean, node, getAgentCsrInfoMapping());
		return info;
	}

	private DogtagCertRequestInfo getCertRequestInfo(Node node) {
		DogtagCertRequestInfo info = new DogtagCertRequestInfo();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(info);
		xmlSupport.extractBeanDataFromXml(bean, node, getCsrInfoMapping());
		return info;
	}

	private DogtagCertificateData getCertData(Node node) {
		DogtagCertificateData data = new DogtagCertificateData();
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
		xmlSupport.extractBeanDataFromXml(bean, node, getCertDetailMapping());
		return data;
	}

	private Map<String, XPathExpression> getAgentCsrInfoMapping() {
		Map<String, XPathExpression> result = this.agentCsrInfoMapping;
		if ( result == null ) {
			Map<String, String> map = new LinkedHashMap<String, String>(3);
			map.put("requestorEmail", "//InputAttr[@name='requestor_email']");
			map.put("requestorName", "//InputAttr[@name='requestor_name']");
			map.put("csr", "//InputAttr[@name='cert_request']");
			map.put("subjectDn",
					"//def[@id='User Supplied Subject Name Default']/policyAttribute[@name='name']/value");
			result = xmlSupport.getXPathExpressionMap(map);
			this.agentCsrInfoMapping = result;
		}
		return result;
	}

	private Map<String, XPathExpression> getCsrInfoMapping() {
		Map<String, XPathExpression> result = this.csrInfoMapping;
		if ( result == null ) {
			Map<String, String> map = new LinkedHashMap<String, String>(3);
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
			Map<String, String> map = new LinkedHashMap<String, String>(3);
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
			throw new IllegalArgumentException("The CSR result ID XPath [" + csrRequestIdXPath
					+ "] is not valid", e);
		}
	}

	public String getBaseUrl() {
		return baseUrl;
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

}
