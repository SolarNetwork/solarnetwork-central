/* ==================================================================
 * CertificateController.java - 22/07/2016 1:02:23 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web.api;

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * Controller for node certificate API actions.
 * 
 * @author matt
 * @version 1.2
 */
@Controller("v1NodeCertificateController")
@RequestMapping(value = "/solarin/api/v1/sec/cert")
@GlobalExceptionRestController
public class NodeCertificateController {

	private static final Logger log = LoggerFactory.getLogger(NodeCertificateController.class);

	private final RegistrationBiz registrationBiz;

	/**
	 * Constructor.
	 * 
	 * @param regBiz
	 *        the RegistrationBiz to use
	 */
	@Autowired
	public NodeCertificateController(RegistrationBiz regBiz) {
		super();
		this.registrationBiz = regBiz;
	}

	/**
	 * Renew a node's certificate, saving the entire keystore on the server. The
	 * renewal will be processed asynchronously, and nodes can pick up the
	 * renewed certificate via the same process as implemented by
	 * {@link RegistrationBiz#renewNodeCertificate(net.solarnetwork.central.user.domain.UserNode, String)}.
	 * 
	 * @param keystorePassword
	 *        The password for the keystore.
	 * @param keystore
	 *        The PKCS12 keystore data with the existing node private/public
	 *        keys and signed certificate.
	 * @return the result
	 */
	@RequestMapping(value = "/renew", method = RequestMethod.POST,
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public Result<Object> renewActiveCert(@RequestParam("password") String keystorePassword,
			@RequestPart("keystore") MultipartFile keystore) {
		try {
			registrationBiz.renewNodeCertificate(keystore.getInputStream(), keystorePassword);
			return success();
		} catch ( IOException e ) {
			log.debug("IOException renewing certificate", e);
			return error(null, e.getMessage());
		}
	}

	/**
	 * Renew a node's certificate, saving the entire keystore on the server. The
	 * renewal will be processed asynchronously, and nodes can pick up the
	 * renewed certificate via the same process as implemented by
	 * {@link RegistrationBiz#renewNodeCertificate(net.solarnetwork.central.user.domain.UserNode, String)}.
	 * 
	 * 
	 * @param keystorePassword
	 *        The password for the keystore.
	 * @param base64Keystore
	 *        The PKCS12 keystore data, as a Base64-encoded string, with the
	 *        existing node private/public keys and signed certificate.
	 * @return the result
	 */
	@RequestMapping(value = "/renew", method = RequestMethod.POST, params = "keystore")
	@ResponseBody
	public Result<Object> renewActiveCert(@RequestParam("password") String keystorePassword,
			@RequestParam("keystore") String base64Keystore) {
		byte[] data = Base64.getMimeDecoder().decode(base64Keystore);
		try {
			registrationBiz.renewNodeCertificate(new ByteArrayInputStream(data), keystorePassword);
			return success();
		} catch ( IOException e ) {
			log.debug("IOException renewing certificate", e);
			return error(null, e.getMessage());
		}
	}

}
