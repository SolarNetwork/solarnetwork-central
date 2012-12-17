/* ==================================================================
 * DaoRegistrationBizTest.java - Jan 11, 2010 9:34:47 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.RegistrationReceipt;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Test case for the {@link DaoRegistrationBiz} class.
 * 
 * @author matt
 * @version $Id$
 */
@ContextConfiguration
public class DaoRegistrationBizTest extends AbstractCentralTransactionalTest {

	/**
	 * The tables to delete from at the start of the tests (within a
	 * transaction).
	 */
	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_user" };

	private static final String TEST_PASSWORD = "password";
	private static final String TEST_NAME = "Foo Bar";
	private static final String TEST_EMAIL = "foo@localhost.localdomain";

	@Autowired
	private DaoRegistrationBiz daoRegistrationBiz;
	@Autowired
	private UserDao userDao;
	@Autowired
	private UserNodeDao userNodeDao;
	@Autowired
	private SolarNodeDao solarNodeDao;

	@Before
	public void setUp() throws Exception {
		deleteFromTables(DELETE_TABLES);
	}

	/**
	 * Test able to register a new user.
	 */
	@Test
	public void testRegisterUser() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		RegistrationReceipt receipt = daoRegistrationBiz.registerUser(newUser);
		logger.debug("Got receipt: " + receipt);
		assertNotNull(receipt);
		assertNotNull(receipt.getUsername());
		assertNotNull(receipt.getConfirmationCode());
		assertEquals("The receipt email should equal user email", newUser.getEmail(),
				receipt.getUsername());
	}

	/**
	 * Test able to register and confirm a new user.
	 */
	@Test
	public void testRegisterAndConfirmUser() {
		User newUser = new User();
		newUser.setCreated(new DateTime(2009, 1, 11, 16, 28, 0, 0));
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		RegistrationReceipt receipt = daoRegistrationBiz.registerUser(newUser);
		logger.debug("Got receipt: " + receipt);
		assertNotNull(receipt);
		assertNotNull(receipt.getConfirmationCode());

		// now confirm registered user
		User confirmedUser = daoRegistrationBiz.confirmRegisteredUser(receipt);
		logger.debug("Got confirmed user: " + confirmedUser);
		assertNotNull(confirmedUser);
		assertEquals("The confirmed user's email should match the registered email.",
				newUser.getEmail(), confirmedUser.getEmail());
		assertEquals("The confirmed user's name should match the registered name.", newUser.getName(),
				confirmedUser.getName());
		assertNotNull(confirmedUser.getId());
		assertNotNull(confirmedUser.getPassword());
		logger.debug("Confirmed user password: " + confirmedUser.getPassword());
		assertEquals("The confirmed user's password should be encrypted",
				"{SHA}5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
				confirmedUser.getPassword());

		assertNotNull(confirmedUser.getRoles());
		assertEquals(1, confirmedUser.getRoles().size());
		assertEquals("ROLE_USER", confirmedUser.getRoles().iterator().next());
	}

	/**
	 * Test duplicate emails are not allowed to be registered.
	 */
	@Test
	public void testAttemptRegisterDuplicateEmail() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		RegistrationReceipt receipt = daoRegistrationBiz.registerUser(newUser);
		logger.debug("Got receipt: " + receipt);
		assertNotNull(receipt);

		User dupUser = new User();
		dupUser.setCreated(new DateTime());
		dupUser.setEmail(newUser.getEmail());
		dupUser.setName(newUser.getName());
		newUser.setPassword("other.password");

		try {
			receipt = daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.DUPLICATE_EMAIL, e.getReason());
			assertEquals(newUser.getEmail(), e.getEmail());
		}
	}

	/**
	 * Test attempting to confirm a non-existing email fails.
	 */
	@Test
	public void testAttemptConfirmNonExistingEmail() {
		BasicRegistrationReceipt receipt = new BasicRegistrationReceipt("does.not.exist@localhost",
				"not a code");
		try {
			daoRegistrationBiz.confirmRegisteredUser(receipt);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.UNKNOWN_EMAIL, e.getReason());
			assertEquals(receipt.getUsername(), e.getEmail());
		}
	}

	/**
	 * Test attempting to confirm a valid email with the wrong confirmation
	 * code.
	 */
	@Test
	public void testAttemptConfirmBadCode() {
		User newUser = new User();
		newUser.setCreated(new DateTime(2009, 1, 11, 16, 28, 0, 0));
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		RegistrationReceipt receipt = daoRegistrationBiz.registerUser(newUser);
		logger.debug("Got receipt: " + receipt);
		assertNotNull(receipt);
		assertNotNull(receipt.getConfirmationCode());

		// now confirm with bad code
		BasicRegistrationReceipt badReceipt = new BasicRegistrationReceipt(newUser.getEmail(),
				"not a code");
		assertTrue(!badReceipt.getConfirmationCode().equals(receipt.getConfirmationCode()));
		try {
			daoRegistrationBiz.confirmRegisteredUser(badReceipt);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED, e.getReason());
			assertEquals(receipt.getUsername(), e.getEmail());
		}
	}

	/**
	 * Test attempting to confirm a user that has already been confirmed.
	 */
	@Test
	public void testAttemptConfirmConfirmedUser() {
		User newUser = new User();
		newUser.setCreated(new DateTime(2009, 1, 11, 16, 28, 0, 0));
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		RegistrationReceipt receipt = daoRegistrationBiz.registerUser(newUser);
		logger.debug("Got receipt: " + receipt);
		assertNotNull(receipt);
		assertNotNull(receipt.getConfirmationCode());

		User confirmedUser = daoRegistrationBiz.confirmRegisteredUser(receipt);
		assertNotNull(confirmedUser);

		try {
			daoRegistrationBiz.confirmRegisteredUser(receipt);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED, e.getReason());
			assertEquals(receipt.getUsername(), e.getEmail());
		}
	}

	/**
	 * Test not able to register an empty email.
	 */
	@Test
	public void testAttemptRegisterEmptyEmail() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail("");
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("email");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register a null email.
	 */
	@Test
	public void testAttemptRegisterNullEmail() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(null);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("email");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register an empty password.
	 */
	@Test
	public void testAttemptRegisterEmptyPassword() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword("");

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("password");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register a null password.
	 */
	@Test
	public void testAttemptRegisterNullPassword() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(null);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("password");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register an empty email and password.
	 */
	@Test
	public void testAttemptRegisterEmptyEmailAndPassword() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail("");
		newUser.setName(TEST_NAME);
		newUser.setPassword("");

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			assertNotNull(errors.getFieldError("email"));
			assertNotNull(errors.getFieldError("password"));
		}
	}

	/**
	 * Test not able to register an empty name.
	 */
	@Test
	public void testAttemptRegisterEmptyName() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName("");
		newUser.setPassword(TEST_PASSWORD);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("name");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register a null name.
	 */
	@Test
	public void testAttemptRegisterNullName() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(null);
		newUser.setPassword(TEST_PASSWORD);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("name");
			assertNotNull(emailError);
		}
	}

	/**
	 * Test not able to register a too-long name.
	 */
	@Test
	public void testAttemptRegisterLongName() {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(TEST_EMAIL);
		StringBuilder buf = new StringBuilder();
		for ( int i = 0; i < 50; i++ ) {
			buf.append(TEST_NAME);
		}
		newUser.setName(buf.toString());
		newUser.setPassword(TEST_PASSWORD);

		try {
			daoRegistrationBiz.registerUser(newUser);
			fail("Should have thrown ValidationException");
		} catch ( ValidationException e ) {
			assertNotNull(e.getErrors());
			Errors errors = e.getErrors();
			FieldError emailError = errors.getFieldError("name");
			assertNotNull(emailError);
		}
	}

}
