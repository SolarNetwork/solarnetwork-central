/* ==================================================================
 * DelegatingRegistrationBiz.java - 21/07/2016 8:02:34 AM
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

package net.solarnetwork.central.user.support;

import java.io.IOException;
import java.io.InputStream;
import org.joda.time.ReadablePeriod;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificateRenewal;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.RegistrationReceipt;

/**
 * Delegating implementation of {@link RegistrationBiz}, mostly to help with
 * AOP.
 * 
 * @author matt
 * @version 1.1
 */
public class DelegatingRegistrationBiz implements RegistrationBiz {

	private final RegistrationBiz delegate;

	/**
	 * Construct with a delegate;
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingRegistrationBiz(RegistrationBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public RegistrationReceipt registerUser(User user) throws AuthorizationException {
		return delegate.registerUser(user);
	}

	@Override
	public RegistrationReceipt createReceipt(String username, String confirmationCode) {
		return delegate.createReceipt(username, confirmationCode);
	}

	@Override
	public User confirmRegisteredUser(RegistrationReceipt receipt) throws AuthorizationException {
		return delegate.confirmRegisteredUser(receipt);
	}

	@Override
	public NetworkAssociation createNodeAssociation(NewNodeRequest request) {
		return delegate.createNodeAssociation(request);
	}

	@Override
	public NetworkAssociation getNodeAssociation(Long userNodeConfirmationId)
			throws AuthorizationException {
		return delegate.getNodeAssociation(userNodeConfirmationId);
	}

	@Override
	public void cancelNodeAssociation(Long userNodeConfirmationId) throws AuthorizationException {
		delegate.cancelNodeAssociation(userNodeConfirmationId);
	}

	@SuppressWarnings("deprecation")
	@Override
	public NetworkCertificate confirmNodeAssociation(String username, String confirmationKey)
			throws AuthorizationException {
		return delegate.confirmNodeAssociation(username, confirmationKey);
	}

	@Override
	public NetworkCertificate confirmNodeAssociation(NetworkAssociation association)
			throws AuthorizationException {
		return delegate.confirmNodeAssociation(association);
	}

	@Override
	public NetworkCertificate getNodeCertificate(NetworkAssociation association) {
		return delegate.getNodeCertificate(association);
	}

	@Override
	public NetworkCertificate renewNodeCertificate(InputStream pkcs12InputStream,
			String keystorePassword) throws IOException {
		return delegate.renewNodeCertificate(pkcs12InputStream, keystorePassword);
	}

	@Override
	public ReadablePeriod getNodeCertificateRenewalPeriod() {
		return delegate.getNodeCertificateRenewalPeriod();
	}

	@Override
	public UserNodeCertificateRenewal renewNodeCertificate(UserNode userNode, String keystorePassword) {
		return delegate.renewNodeCertificate(userNode, keystorePassword);
	}

	@Override
	public UserNodeCertificateRenewal getPendingNodeCertificateRenewal(UserNode userNode,
			String confirmationKey) {
		return delegate.getPendingNodeCertificateRenewal(userNode, confirmationKey);
	}

	@Override
	public User updateUser(User userEntry) {
		return delegate.updateUser(userEntry);
	}

	@Override
	public RegistrationReceipt generateResetPasswordReceipt(String email) throws AuthorizationException {
		return delegate.generateResetPasswordReceipt(email);
	}

	@Override
	public void resetPassword(RegistrationReceipt receipt, PasswordEntry password) {
		delegate.resetPassword(receipt, password);
	}

}
