/* ==================================================================
 * RegistrationBiz.java - Dec 18, 2009 3:51:09 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz;

import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.RegistrationReceipt;

/**
 * API for user registration tasks.
 * 
 * @author matt
 * @version 1.4
 */
public interface RegistrationBiz {

	/**
	 * Flag for a String value that should not change.
	 * 
	 * <p>
	 * For example, when updating a User, the password field can be left
	 * unchanged when set to this value.
	 * </p>
	 */
	static final String DO_NOT_CHANGE_VALUE = "**DO_NOT_CHANGE**";

	/**
	 * Register a new user.
	 * 
	 * <p>
	 * Use this method to register a new user. After registration the user will
	 * be stored in the back end, but the user will require confirmation before
	 * they can officially log into the application (see
	 * {@link #confirmRegisteredUser(RegistrationReceipt)}).
	 * </p>
	 * 
	 * @param user
	 *        the new user to register
	 * @return a confirmation string suitable to pass to
	 *         {@link #confirmRegisteredUser(String, String, BizContext)}
	 * @throws AuthorizationException
	 *         if the desired login is taken already, this exception will be
	 *         thrown with the reason code
	 *         {@link AuthorizationException.Reason#DUPLICATE_LOGIN}
	 */
	RegistrationReceipt registerUser(User user) throws AuthorizationException;

	/**
	 * Helper method for creating a RegistrationReceipt instance from a username
	 * and code.
	 * 
	 * <p>
	 * This has been added to support web flows.
	 * </p>
	 * 
	 * @param username
	 *        the username
	 * @param confirmationCode
	 *        the confirmation code
	 * @return the receipt instance
	 */
	public RegistrationReceipt createReceipt(String username, String confirmationCode);

	/**
	 * Confirm a registered user.
	 * 
	 * <p>
	 * After a user has registered (see {@link #registerUser(User)}) they must
	 * confirm the registration via this method. After confirmation the user can
	 * login as a normal user.
	 * </p>
	 * 
	 * @param receipt
	 *        the registration receipt to confirm
	 * @return the confirmed user
	 * @throws AuthorizationException
	 *         if the receipt details do not match those returned from a
	 *         previous call to {@link #registerUser(User)} then the reason code
	 *         will be set to
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED};
	 *         if the login is not found then
	 *         {@link AuthorizationException.Reason#UNKNOWN_EMAIL}; if the
	 *         account has already been confirmed then
	 *         {@link AuthorizationException.Reason#REGISTRATION_ALREADY_CONFIRMED}
	 */
	User confirmRegisteredUser(RegistrationReceipt receipt) throws AuthorizationException;

	/**
	 * Generate a new {@link NetworkAssociationDetails} entity.
	 * 
	 * <p>
	 * This will return a new {@link NetworkAssociationDetails} and the system
	 * details associated with specified User. The node will still need to
	 * associate with the system before it will be recognized.
	 * </p>
	 * 
	 * @param request
	 *        the node request details
	 * 
	 * @return new NodeAssociationDetails entity
	 */
	NetworkAssociation createNodeAssociation(NewNodeRequest request);

	/**
	 * Get a {@link NetworkAssociationDetails} previously created via
	 * {@link #createNodeAssociation(Long)}
	 * 
	 * @param userNodeConfirmationId
	 *        the UserNodeConfirmation ID to create the details for
	 * @return the NetworkAssociationDetails
	 * @throws AuthorizationException
	 *         if the acting user does not have permission to view the requested
	 *         confirmation then
	 *         {@link AuthorizationException.Reason#ACCESS_DENIED}
	 */
	NetworkAssociation getNodeAssociation(Long userNodeConfirmationId) throws AuthorizationException;

	/**
	 * Cancel a {@link NetworkAssociationDetails} previously created via
	 * {@link #createNodeAssociation(Long)}
	 * 
	 * @param userNodeConfirmationId
	 *        the UserNodeConfirmation ID to create the details for
	 * @throws AuthorizationException
	 *         if the acting user does not have permission to view the requested
	 *         confirmation then
	 *         {@link AuthorizationException.Reason#ACCESS_DENIED}
	 */
	void cancelNodeAssociation(Long userNodeConfirmationId) throws AuthorizationException;

	/**
	 * Confirm a node association previously created via
	 * {@link #createNodeAssociation(User)}.
	 * 
	 * <p>
	 * This method must be called after a call to
	 * {@link #createNodeAssociation(Long, String)} to confirm the node
	 * association.
	 * </p>
	 * 
	 * @param username
	 *        the username to associate the node with
	 * @param confirmationKey
	 *        the confirmation code from
	 *        {@link NetworkAssociation#getConfirmationKey()}
	 * @return new RegistrationReceipt object
	 * @throws AuthorizationException
	 *         if the details do not match those returned from a previous call
	 *         to {@link #createNodeAssociation(User)} then the reason code will
	 *         be set to
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED};
	 *         if the node has already been confirmed then
	 *         {@link AuthorizationException.Reason#REGISTRATION_ALREADY_CONFIRMED}
	 * @deprecated see {@link #confirmNodeAssociation(NetworkAssociation)}
	 */
	@Deprecated
	NetworkCertificate confirmNodeAssociation(String username, String confirmationKey)
			throws AuthorizationException;

	/**
	 * Confirm a node association previously created via
	 * {@link #createNodeAssociation(User)}. This method must be called after a
	 * call to {@link #createNodeAssociation(Long, String)} to confirm the node
	 * association. The {@code username} and {@code confirmationKey} are
	 * required. If a {@code keystorePassword} is provided a private key will be
	 * generated and a certificate will be automatically requested for the node,
	 * which will be encrypted with the provided password.
	 * 
	 * 
	 * @param association
	 *        the association details
	 * @return new RegistrationReceipt object
	 * @throws AuthorizationException
	 *         if the details do not match those returned from a previous call
	 *         to {@link #createNodeAssociation(User)} then the reason code will
	 *         be set to
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED};
	 *         if the node has already been confirmed then
	 *         {@link AuthorizationException.Reason#REGISTRATION_ALREADY_CONFIRMED}
	 * @since 1.3
	 */
	NetworkCertificate confirmNodeAssociation(NetworkAssociation association)
			throws AuthorizationException;

	/**
	 * Obtain a certificate generated and signed by SolarUser on behalf of the
	 * node. This method can be called <em>after</em> a call to
	 * {@link #confirmNodeAssociation(NetworkAssociation)} where a
	 * {@code keystorePassword} was also supplied. The {@code username},
	 * {@code confirmationKey}, and {@code keystorePassword} are required in
	 * this call, and must match the values previously used in
	 * {@link #confirmNodeAssociation(NetworkAssociation)}.
	 * 
	 * @param association
	 *        the association details
	 * @return the network certificate
	 * @throws AuthorizationException
	 *         if the details do not match those returned from a previous call
	 *         to {@link #confirmNodeAssociation(NetworkAssociation)}
	 * @since 1.3
	 */
	NetworkCertificate getNodeCertificate(NetworkAssociation association);

	/**
	 * Renew a certificate generated and signed by SolarUser by a previous call
	 * to {@link #confirmNodeAssociation(NetworkAssociation)} where a
	 * {@code keystorePassword} was also supplied. The {@code username},
	 * {@code confirmationKey}, and {@code keystorePassword} are required in
	 * this call, and must match the values previously used in
	 * {@link #confirmNodeAssociation(NetworkAssociation)}.
	 * 
	 * This method is meant to support renewing certificates via a SolarNode.
	 * 
	 * @param association
	 *        the association details
	 * @return the network certificate
	 * @throws AuthorizationException
	 *         if the details do not match those returned from a previous call
	 *         to {@link #confirmNodeAssociation(NetworkAssociation)}
	 * @since 1.4
	 * @see #renewNodeCertificate(UserNode, String)
	 */
	NetworkCertificate renewNodeCertificate(NetworkAssociation association);

	/**
	 * Renew a certificate generated and signed by SolarUser by a previous call
	 * to {@link #confirmNodeAssociation(NetworkAssociation)} where a
	 * {@code keystorePassword} was also supplied.
	 *
	 * This method is meant to support renewing certificates via SolarUser.
	 *
	 * @param userNode
	 *        the user node to renew the certificate for
	 * @param keystorePassword
	 *        the password used to encrypt the certificate store
	 * @return the network certificate
	 * @throws AuthorizationException
	 *         if the details do not match those returned from a previous call
	 *         to {@link #confirmNodeAssociation(NetworkAssociation)}
	 * @since 1.4
	 * @see #renewNodeCertificate(NetworkAssociation)
	 */
	NetworkCertificate renewNodeCertificate(UserNode userNode, String keystorePassword);

	/**
	 * Update the details of a user entity.
	 * 
	 * <p>
	 * The {@link User#getId()} value must be populated with the ID of the user
	 * to update, and then any modifiable fields that are not <em>null</em> will
	 * be updated with the provided value.
	 * </p>
	 * 
	 * @param userEntry
	 *        the input data
	 * @return the updated user entity
	 */
	User updateUser(User userEntry);

	/**
	 * Generate a password reset receipt for a given username.
	 * 
	 * @param email
	 *        the email to generate the receipt for
	 * @return the receipt
	 * @throws AuthorizationException
	 *         if the username is not found, then
	 *         {@link AuthorizationException.Reason#UNKNOWN_EMAIL}
	 */
	RegistrationReceipt generateResetPasswordReceipt(String email) throws AuthorizationException;

	/**
	 * Reset a user's password.
	 * 
	 * @param receipt
	 *        the receipt obtained previously via a call to
	 *        {@link #generateResetPasswordReceipt(String)}
	 * @param password
	 *        the new password to set
	 * @throws AuthorizationException
	 *         if the user cannot be found, or the details do not match those
	 *         returned from a previous call to
	 *         {@link #generateResetPasswordReceipt(String)} then the reason
	 *         code will be set to
	 *         {@link AuthorizationException.Reason#FORGOTTEN_PASSWORD_NOT_CONFIRMED}
	 */
	void resetPassword(RegistrationReceipt receipt, PasswordEntry password);

}
