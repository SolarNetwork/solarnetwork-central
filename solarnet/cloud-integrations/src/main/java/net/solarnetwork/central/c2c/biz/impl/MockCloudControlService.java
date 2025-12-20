/* ==================================================================
 * MockCloudControlService.java - 12/11/2025 6:47:45 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CommonInstructionTopic;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Mock implementation of {@link CloudControlService} for development and
 * testing.
 *
 * @author matt
 * @version 1.0
 */
public class MockCloudControlService extends BaseCloudControlService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ctrl.mock";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a filter ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/** The supported topics. */
	// @formatter:off
	public static final SortedSet<String> SUPPORTED_TOPICS = Collections.unmodifiableSortedSet(new TreeSet<>(List.of(
			CommonInstructionTopic.SetControlParameter.name()
			)));
	// @formatter:on

	// a static set of data values
	private static final SequencedMap<String, SequencedMap<String, List<CloudDataValue>>> DATA_VALUES;
	static {
		SequencedMap<String, SequencedMap<String, List<CloudDataValue>>> sites = new LinkedHashMap<>(2);
		for ( int s = 1; s <= 2; s++ ) {
			String systemId = "S" + s;
			SequencedMap<String, List<CloudDataValue>> siteValues = new LinkedHashMap<>(2);

			// a simple switch
			// @formatter:off
			String switchDeviceId = "D1";
			siteValues.put("D1", List.of(
					dataValue(List.of(systemId, switchDeviceId, CommonInstructionTopic.SetControlParameter.name()), "Switch")
					));
			// @formatter:on

			sites.put(systemId, Collections.unmodifiableSequencedMap(siteValues));
		}
		DATA_VALUES = Collections.unmodifiableSequencedMap(sites);
	}

	/**
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param integrationDao
	 *        the integration DAO
	 * @param controlDao
	 *        the control DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MockCloudControlService(UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			Clock clock, CloudIntegrationConfigurationDao integrationDao,
			CloudControlConfigurationDao controlDao) {
		super(SERVICE_IDENTIFIER, "Mock Control Service", userEventAppenderBiz, encryptor, List.of(),
				clock, integrationDao, controlDao);
	}

	@Override
	public Set<String> supportedTopics() {
		return SUPPORTED_TOPICS;
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SYSTEM_ID_FILTER, DEVICE_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			Map<String, ?> filters) {
		requireNonNullObject(integrationDao.get(requireNonNullArgument(integrationId, "integrationId")),
				"integration");
		List<CloudDataValue> result = Collections.emptyList();
		if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null
				&& filters.get(DEVICE_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			String deviceId = filters.get(DEVICE_ID_FILTER).toString();
			result = deviceTopics(systemId, deviceId);
		} else if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(systemId);
		} else {
			// list available systems
			result = systems();
		}
		try {
			Collections.sort(result);
		} catch ( UnsupportedOperationException e ) {
			// probably immutable collection
		}
		return result;
	}

	private List<CloudDataValue> systems() {
		List<CloudDataValue> result = new ArrayList<>(8);
		for ( var systemId : DATA_VALUES.keySet() ) {
			result.add(intermediateDataValue(List.of(systemId), systemId, null));
		}
		return result;
	}

	private List<CloudDataValue> systemDevices(final String systemId) {
		var devices = DATA_VALUES.get(systemId);
		if ( devices == null ) {
			return List.of();
		}
		List<CloudDataValue> result = new ArrayList<>(8);
		for ( var deviceId : devices.keySet() ) {
			result.add(intermediateDataValue(List.of(systemId, deviceId), deviceId, null));
		}
		return result;
	}

	private List<CloudDataValue> deviceTopics(final String systemId, final String deviceId) {
		var devices = DATA_VALUES.get(systemId);
		if ( devices == null ) {
			return List.of();
		}
		var deviceTopics = devices.get(deviceId);
		if ( deviceTopics == null ) {
			return List.of();
		}
		return deviceTopics;
	}

	@Override
	public InstructionStatus executeInstruction(UserLongCompositePK cloudControlId,
			NodeInstruction instruction) {

		validateTopicIsSupported(instruction);

		return performAction(cloudControlId, (ms, control, integration) -> {
			InstructionStatus result = instruction.toStatus()
					.newCopyWithState(InstructionState.Completed);
			// @formatter:off
			userEventAppenderBiz.addEvent(control.getUserId(), eventForUserRelatedKey(control.getId(),
					INTEGRATION_CONTROL_INSTRUCTION_TAGS, "Executed instruction",
					Map.of(
							INTEGRATION_ID_DATA_KEY, integration.getConfigId(),
							INSTRUCTION_TOPIC_DATA_KEY, instruction.getInstruction().getTopic(),
							INSTRUCTION_ID_DATA_KEY, instruction.getId(),
							INSTRUCTION_STATE_DATA_KEY, InstructionState.Completed,
							INSTRUCTION_DATA_KEY, getStringMap(getJSONString(instruction, null))
					)));
			// @formatter:on
			return result;
		});
	}

}
