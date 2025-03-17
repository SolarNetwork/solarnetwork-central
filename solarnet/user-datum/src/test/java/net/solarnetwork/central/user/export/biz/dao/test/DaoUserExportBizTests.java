/* ==================================================================
 * DaoUserExportBizTests.java - 18/03/2025 7:34:34 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.biz.dao.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_ENTITY_ID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link DaoUserExportBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserExportBizTests {

	private static final String TEST_SECURE_SETTING = "watchout";

	private static final String TEST_OUTPUT_SERVICE_ID = randomString();

	private static final String TEST_DESTINATION_SERVICE_ID = randomString();

	@Mock
	private UserDatumExportConfigurationDao datumExportConfigDao;

	@Mock
	private UserDataConfigurationDao dataConfigDao;

	@Mock
	private UserDestinationConfigurationDao destinationConfigDao;

	@Mock
	private UserOutputConfigurationDao outputConfigDao;

	@Mock
	private UserDatumExportTaskInfoDao taskDao;

	@Mock
	private UserAdhocDatumExportTaskInfoDao adhocTaskDao;

	@Mock
	private UserExportTaskBiz userExportTaskBiz;

	@Mock
	private DatumExportOutputFormatService outputFormatService;

	@Mock
	private DatumExportDestinationService destinationService;

	private PrefixedTextEncryptor textEncryptor = PrefixedTextEncryptor.aesTextEncryptor(randomString(),
			randomString());

	private DaoUserExportBiz biz;

	@BeforeEach
	public void setup() {

		given(outputFormatService.getSettingUid()).willReturn(TEST_OUTPUT_SERVICE_ID);
		given(destinationService.getSettingUid()).willReturn(TEST_DESTINATION_SERVICE_ID);

		// provide settings to verify masking sensitive values
		List<SettingSpecifier> settings = Arrays.asList(new BasicTextFieldSettingSpecifier("foo", null),
				new BasicTextFieldSettingSpecifier(TEST_SECURE_SETTING, null, true));
		given(outputFormatService.getSettingSpecifiers()).willReturn(settings);
		given(destinationService.getSettingSpecifiers()).willReturn(settings);

		biz = new DaoUserExportBiz(datumExportConfigDao, dataConfigDao, destinationConfigDao,
				outputConfigDao, taskDao, adhocTaskDao, userExportTaskBiz, textEncryptor,
				List.of(outputFormatService), List.of(destinationService));
	}

	@Test
	public void outputConfiguration_create_secure() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserOutputConfiguration conf = new UserOutputConfiguration(userId, UNASSIGNED_ENTITY_ID,
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, "bam"));

		given(outputConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(2)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 1)
			.as("Secure setting encrypted")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo("bam")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	@Test
	public void destinationConfiguration_create_secure() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserDestinationConfiguration conf = new UserDestinationConfiguration(userId,
				UNASSIGNED_ENTITY_ID, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, "bam"));

		given(destinationConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(2)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 1)
			.as("Secure setting encrypted")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo("bam")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	@Test
	public void destinationConfiguration_update_replaceSecure() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserDestinationConfiguration existing = new UserDestinationConfiguration(userId,
				UNASSIGNED_ENTITY_ID, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		existing.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		existing.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, textEncryptor.encrypt("bing")));

		given(destinationConfigDao.get(pk, userId)).willReturn(existing);

		final UserDestinationConfiguration conf = new UserDestinationConfiguration(pk,
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", 2, "bar", 1, TEST_SECURE_SETTING, "bam"));

		given(destinationConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(3)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 2)
			.as("New plain setting saved as-is")
			.containsEntry("bar", 1)
			.as("Secure setting encrypted")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as updated plain text")
							.isEqualTo("bam")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	@Test
	public void destinationConfiguration_update_preserveSecure_fromBlank() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserDestinationConfiguration existing = new UserDestinationConfiguration(userId,
				UNASSIGNED_ENTITY_ID, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		existing.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		existing.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, textEncryptor.encrypt("bing")));

		given(destinationConfigDao.get(pk, userId)).willReturn(existing);

		final UserDestinationConfiguration conf = new UserDestinationConfiguration(pk,
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", 2, "bar", 1, TEST_SECURE_SETTING, ""));

		given(destinationConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(3)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 2)
			.as("New plain setting saved as-is")
			.containsEntry("bar", 1)
			.as("Secure setting preserved from existing")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to original plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as existing plain text")
							.isEqualTo("bing")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	@Test
	public void destinationConfiguration_update_preserveSecure_fromEncrypted() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserDestinationConfiguration existing = new UserDestinationConfiguration(userId,
				UNASSIGNED_ENTITY_ID, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		existing.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		existing.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, textEncryptor.encrypt("bing")));

		given(destinationConfigDao.get(pk, userId)).willReturn(existing);

		final UserDestinationConfiguration conf = new UserDestinationConfiguration(pk,
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(
				Map.of("foo", 2, "bar", 1, TEST_SECURE_SETTING, textEncryptor.encrypt("blamo")));

		given(destinationConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(3)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 2)
			.as("New plain setting saved as-is")
			.containsEntry("bar", 1)
			.as("Secure setting preserved from existing")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value has encryptor prefix")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to existing plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as existing plain text")
							.isEqualTo("bing")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

	@Test
	public void destinationConfiguration_update_preserveSecure_fromDigest() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		final UserDestinationConfiguration existing = new UserDestinationConfiguration(userId,
				UNASSIGNED_ENTITY_ID, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		existing.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		existing.setServiceProps(Map.of("foo", 1, TEST_SECURE_SETTING, "not.encrypted"));

		given(destinationConfigDao.get(pk, userId)).willReturn(existing);

		final UserDestinationConfiguration conf = new UserDestinationConfiguration(pk,
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setServiceIdentifier(TEST_OUTPUT_SERVICE_ID);
		conf.setServiceProps(Map.of("foo", 2, "bar", 1, TEST_SECURE_SETTING,
				"{SHA-256}%s".formatted(DigestUtils.sha256Hex("some.thing"))));

		given(destinationConfigDao.save(same(conf))).willReturn(pk);

		// WHEN
		Long result = biz.saveConfiguration(conf);

		// THEN
		// @formatter:off
		and.then(conf.getServiceProps())
			.as("Service props preserved")
			.hasSize(3)
			.as("Plain setting saved as-is")
			.containsEntry("foo", 2)
			.as("New plain setting saved as-is")
			.containsEntry("bar", 1)
			.as("Secure setting preserved from existing")
			.hasEntrySatisfying(TEST_SECURE_SETTING, v -> {
				and.then(v)
					.asInstanceOf(STRING)
					.as("Sensitive value is encrypted from unencrypted existing")
					.startsWith(textEncryptor.getPrefix())
					.as("Can decrypt value back to existing plain text")
					.satisfies(cipherText -> {
						and.then(textEncryptor.decrypt(cipherText))
							.as("Decrypted value same as existing plain text")
							.isEqualTo("not.encrypted")
							;
					})
					;
			})
			;

		and.then(result)
			.as("DAO entity ID returned")
			.isEqualTo(entityId)
			;
		// @formatter:on
	}

}
