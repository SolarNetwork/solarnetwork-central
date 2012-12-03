/**
 * 
 */

package net.solarnetwork.central.reg.web;

import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.web.support.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web controller for confirming node association.
 */
@Controller
public class NewNodeController extends ControllerSupport {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The default view name. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	private final RegistrationBiz registrationBiz;

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
