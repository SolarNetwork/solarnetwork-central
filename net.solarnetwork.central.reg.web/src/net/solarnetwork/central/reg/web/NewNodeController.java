/**
 * 
 */

package net.solarnetwork.central.reg.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.user.biz.AuthorizationException;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.web.support.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web controller for confirming node association.
 */
@Controller
public class NewNodeController {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The default view name. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	private final RegistrationBiz registrationBiz;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param regBiz
	 *        the RegistrationBiz to use
	 */
	@Autowired
	public NewNodeController(RegistrationBiz regBiz) {
		super();
		this.registrationBiz = regBiz;
	}

	/**
	 * SecurityException handler.
	 * 
	 * <p>
	 * Logs a WARN log and returns HTTP 403 (Forbidden).
	 * </p>
	 * 
	 * @param e
	 *        the security exception
	 * @param res
	 *        the servlet response
	 */
	@ExceptionHandler(SecurityException.class)
	public void handleSecurityException(SecurityException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Security exception: " + e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	/**
	 * AuthorizationException handler.
	 * 
	 * <p>
	 * Logs a WARN log and returns HTTP 403 (Forbidden).
	 * </p>
	 * 
	 * @param e
	 *        the exception
	 * @param res
	 *        the servlet response
	 */
	@ExceptionHandler(AuthorizationException.class)
	public void handleSecurityException(AuthorizationException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Authorization exception: " + e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	/**
	 * Confirm a node association
	 * 
	 * @param request
	 *        the servlet request
	 * @param model
	 *        the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST }, value = "/associate.*")
	public String confirmNodeAssociation(HttpServletRequest request,
			@RequestParam("username") String username, @RequestParam("key") String key, Model model) {
		NetworkCertificate receipt = registrationBiz.confirmNodeAssociation(username, key);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, receipt);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}

}
