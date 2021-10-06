/* ==================================================================
 * RegistrationBizConfig.java - 7/10/2021 10:59:29 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.biz.UserMetadataBiz;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserAlertBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserMetadataBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserMetadataDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.service.CertificateService;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Configuration for the registration service.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class RegistrationBizConfig {

	/** The qualifier for the user registration related services. */
	public static final String USER_REGISTRATION = "user-registration";

	/** The qualifier for the email throttle {@link Cache}. */
	public static final String EMAIL_THROTTLE = "email-throttle";

	@Autowired
	private UserAlertDao userAlertDao;

	@Autowired
	private UserAlertSituationDao userAlertSituationDao;

	@Autowired
	private UserAuthTokenDao userAuthTokenDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserMetadataDao userMetadataDao;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private UserNodeConfirmationDao userNodeConfirmationDao;

	@Autowired
	private UserNodeCertificateDao userNodeCertificateDao;

	@Autowired
	private SolarNodeDao solarNodeDao;

	@Autowired
	private SolarLocationDao solarLocationDao;

	@Autowired
	private NetworkIdentificationBiz networkIdentificationBiz;

	@Autowired
	@Qualifier(USER_REGISTRATION)
	private PasswordEncoder passwordEncoder;

	@Autowired
	@Qualifier(USER_REGISTRATION)
	private Validator userValidator;

	@Autowired
	@Qualifier(EMAIL_THROTTLE)
	private Cache<String, Boolean> emailThottleCache;

	@Autowired
	private NodePKIBiz nodePkiBiz;

	@Autowired
	private InstructorBiz instructorBiz;

	@Autowired
	@Qualifier(USER_REGISTRATION)
	private CertificateService certificateService;

	@Bean
	public RegistrationBiz registrationBiz() {
		DaoRegistrationBiz biz = new DaoRegistrationBiz();
		biz.setUserDao(userDao);
		biz.setUserNodeDao(userNodeDao);
		biz.setUserNodeConfirmationDao(userNodeConfirmationDao);
		biz.setUserNodeCertificateDao(userNodeCertificateDao);
		biz.setSolarNodeDao(solarNodeDao);
		biz.setSolarLocationDao(solarLocationDao);
		biz.setNetworkIdentificationBiz(networkIdentificationBiz);
		biz.setUserValidator(userValidator);
		biz.setPasswordEncoder(passwordEncoder);
		biz.setEmailThrottleCache(emailThottleCache);
		biz.setNodePKIBiz(nodePkiBiz);
		biz.setInstructorBiz(instructorBiz);
		biz.setCertificateService(certificateService);
		return biz;
	}

	@Bean
	public UserBiz userBiz() {
		DaoUserBiz biz = new DaoUserBiz();
		biz.setUserDao(userDao);
		biz.setUserAlertDao(userAlertDao);
		biz.setUserNodeDao(userNodeDao);
		biz.setUserNodeCertificateDao(userNodeCertificateDao);
		biz.setUserNodeCertificateDao(userNodeCertificateDao);
		biz.setUserAuthTokenDao(userAuthTokenDao);
		biz.setSolarNodeDao(solarNodeDao);
		biz.setSolarLocationDao(solarLocationDao);
		return biz;
	}

	@Bean
	public UserMetadataBiz userMetadataBiz() {
		return new DaoUserMetadataBiz(userMetadataDao);
	}

	@Bean
	public UserAlertBiz userAlertBiz() {
		return new DaoUserAlertBiz(userAlertDao, userAlertSituationDao);
	}

}
