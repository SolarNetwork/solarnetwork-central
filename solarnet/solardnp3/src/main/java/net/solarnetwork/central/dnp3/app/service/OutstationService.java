/* ==================================================================
 * OutstationService.java - 9/08/2023 10:21:10 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.service;

import static java.time.Instant.now;
import static net.solarnetwork.central.dnp3.app.service.Dnp3Utils.copySettings;
import static net.solarnetwork.central.instructor.biz.InstructorBiz.createErrorResultParameters;
import static net.solarnetwork.domain.InstructionStatus.createStatus;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.AnalogOutputDouble64;
import com.automatak.dnp3.AnalogOutputFloat32;
import com.automatak.dnp3.AnalogOutputInt16;
import com.automatak.dnp3.AnalogOutputInt32;
import com.automatak.dnp3.AnalogOutputStatus;
import com.automatak.dnp3.BinaryInput;
import com.automatak.dnp3.BinaryOutputStatus;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.ChannelListener;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.DatabaseConfig;
import com.automatak.dnp3.DoubleBitBinaryInput;
import com.automatak.dnp3.EventBufferConfig;
import com.automatak.dnp3.FrozenCounter;
import com.automatak.dnp3.LinkLayerConfig;
import com.automatak.dnp3.Outstation;
import com.automatak.dnp3.OutstationChangeSet;
import com.automatak.dnp3.OutstationConfig;
import com.automatak.dnp3.OutstationStackConfig;
import com.automatak.dnp3.enums.AnalogOutputStatusQuality;
import com.automatak.dnp3.enums.AnalogQuality;
import com.automatak.dnp3.enums.BinaryOutputStatusQuality;
import com.automatak.dnp3.enums.BinaryQuality;
import com.automatak.dnp3.enums.ChannelState;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.CounterQuality;
import com.automatak.dnp3.enums.DoubleBit;
import com.automatak.dnp3.enums.DoubleBitBinaryQuality;
import com.automatak.dnp3.enums.FrozenCounterQuality;
import com.automatak.dnp3.enums.OperateType;
import com.automatak.dnp3.enums.ServerAcceptMode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.Dnp3UserEvents;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.domain.CompositeKey3;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * DNP3 Outstation service.
 * 
 * @author matt
 * @version 1.0
 */
public class OutstationService
		implements ServiceLifecycleObserver, ChannelListener, Dnp3UserEvents, Consumer<ObjectDatum> {

	private static final Logger log = LoggerFactory.getLogger(OutstationService.class);

	/** The default event buffer size. */
	private static final int DEFAULT_EVENT_BUFFER_SIZE = 30;

	/** The default startup delay. */
	private static final int DEFAULT_STARTUP_DELAY_SECONDS = 5;

	/** The default uid value. */
	public static final String DEFAULT_UID = "DNP3 Outstation";

	private final LinkLayerConfig linkLayerConfig = new LinkLayerConfig(false);
	private final DNP3Manager manager;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final InstructorBiz instructorBiz;
	private final ServerAuthConfiguration auth;
	private final String bindAddress;
	private final int port;
	private final List<ServerMeasurementConfiguration> measurementConfigs;
	private final List<ServerControlConfiguration> controlConfigs;

	private final Application app;
	private final CommandHandler commandHandler;
	private final OutstationConfig outstationConfig;

	// the following two maps define the DNP3 set point record tables for each type
	private final Map<MeasurementType, List<ServerMeasurementConfiguration>> measurementTypes;
	private final Map<ControlType, List<ServerControlConfiguration>> controlTypes;

	// the following two maps support fast lookup to map datum passed to accept() to associated
	// measurements and controls that reference the datum's node and source ID
	private final Map<Long, Map<String, List<ServerMeasurementConfiguration>>> datumMeasurements;
	private final Map<Long, Map<String, List<ServerControlConfiguration>>> datumControls;

	private Executor taskExecutor;
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;
	private int startupDelaySecs = DEFAULT_STARTUP_DELAY_SECONDS;

	private Outstation outstation;
	private ChannelState channelState = ChannelState.CLOSED;

	/**
	 * Constructor.
	 * 
	 * @param manager
	 *        the manager
	 * @param userEventAppenderBiz
	 *        the event appender service
	 * @param instructorBiz
	 *        the instructor service
	 * @param auth
	 *        the authorization associated with the server
	 * @param bindAddress
	 *        the bind address, e.g. 127.0.0.1, localhost, etc.
	 * @param port
	 *        the listen port
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OutstationService(DNP3Manager manager, UserEventAppenderBiz userEventAppenderBiz,
			InstructorBiz instructorBiz, ServerAuthConfiguration auth, String bindAddress, int port,
			List<ServerMeasurementConfiguration> measurementConfigs,
			List<ServerControlConfiguration> controlConfigs) {
		super();
		this.manager = requireNonNullArgument(manager, "manager");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.auth = requireNonNullArgument(auth, "auth");
		this.bindAddress = requireNonNullArgument(bindAddress, "bindAddress");
		this.port = port;
		this.measurementConfigs = requireNonNullArgument(measurementConfigs, "measurementConfigs");
		this.controlConfigs = requireNonNullArgument(controlConfigs, "controlConfigs");

		this.app = new Application();
		this.commandHandler = new CommandHandler();
		this.outstationConfig = new OutstationConfig();
		this.measurementTypes = createMeasurementTypeMap();
		this.controlTypes = createControlTypeMap();
		this.datumMeasurements = createDatumMeasurements();
		this.datumControls = createDatumControls();
	}

	@Override
	public synchronized void serviceDidStartup() {
		if ( outstation != null ) {
			return;
		}
		outstation = createOutstation();
		outstation.enable();
		log.info("DNP3 outstation [{}] enabled", getUid());
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( outstation != null && channelState != ChannelState.SHUTDOWN ) {
			outstation.shutdown();
			outstation = null;
		}
	}

	@Override
	public void onStateChange(ChannelState state) {
		log.info("Channel [{}] state changed to {}", getUid(), state);
		this.channelState = state;
	}

	/**
	 * Get the unique ID of this service.
	 * 
	 * @return the unique ID
	 */
	public String getUid() {
		return auth.getIdentifier();
	}

	/**
	 * Get the channel state.
	 * 
	 * @return the channel state.
	 */
	public ChannelState getChannelState() {
		return channelState;
	}

	/*
	 * =========================================================================
	 * OutstationApplication implementation
	 * =========================================================================
	 */

	private class Application extends BaseOutstationApplication {

	}

	/*
	 * =========================================================================
	 * CommandHandler implementation
	 * =========================================================================
	 */

	private class CommandHandler extends BaseCommandHandler {

		private CommandHandler() {
			super(CommandStatus.SUCCESS);
		}

		@Override
		public CommandStatus operateCROB(ControlRelayOutputBlock command, int index,
				OperateType opType) {
			ServerControlConfiguration config = controlConfigForIndex(ControlType.Binary, index);
			if ( config == null ) {
				userEventAppenderBiz.addEvent(auth.getUserId(),
						Dnp3UserEvents.eventWithEntity(auth, INSTRUCTION_TAGS,
								"Binary control does not exist.", Map.of(INDEX_DATA_KEY, index),
								ERROR_TAG));
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received CROB operation request {} on {}[{}] control [{}]",
					getUid(), command.function, config.getType(), index, config.getControlId());
			userEventAppenderBiz.addEvent(auth.getUserId(), Dnp3UserEvents.eventWithEntity(config,
					INSTRUCTION_TAGS, "CROB control operation request received."));

			final Runnable task = () -> {
				try {
					operateBinaryControl(command, index, opType, config);
				} catch ( Exception e ) {
					userEventAppenderBiz.addEvent(auth.getUserId(),
							Dnp3UserEvents.eventWithEntity(config, INSTRUCTION_TAGS,
									"Binary control operation failed.",
									Map.of(MESSAGE_DATA_KEY, e.getMessage()), ERROR_TAG));
					log.error(
							"Error processing DNP3 outstation [{}] operate request {} on {}[{}] control [{}]",
							getUid(), command.function, config.getType(), index, config.getControlId(),
							e);
				}
			};
			final Executor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(task);
			} else {
				task.run();
			}
			return CommandStatus.SUCCESS;
		}

		@Override
		public CommandStatus operateAOI16(AnalogOutputInt16 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputInt16", command.value);
		}

		@Override
		public CommandStatus operateAOI32(AnalogOutputInt32 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputInt32", command.value);
		}

		@Override
		public CommandStatus operateAOF32(AnalogOutputFloat32 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputFloat32", command.value);
		}

		@Override
		public CommandStatus operateAOD64(AnalogOutputDouble64 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputDouble64", command.value);
		}

		private CommandStatus handleAnalogOperation(Object command, int index, String opDescription,
				Number value) {
			ServerControlConfiguration config = controlConfigForIndex(ControlType.Analog, index);
			if ( config == null ) {
				userEventAppenderBiz.addEvent(auth.getUserId(),
						Dnp3UserEvents.eventWithEntity(auth, INSTRUCTION_TAGS,
								opDescription + " control does not exist.",
								Map.of(INDEX_DATA_KEY, index), ERROR_TAG));
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received analog operation request {} on {}[{}] control [{}]",
					getUid(), opDescription, config.getType(), index, config.getControlId());
			userEventAppenderBiz.addEvent(auth.getUserId(), Dnp3UserEvents.eventWithEntity(config,
					INSTRUCTION_TAGS, opDescription + " control operation request received."));

			final Runnable task = () -> {
				try {
					operateAnalogControl(command, index, opDescription, config, value);
				} catch ( Exception e ) {
					userEventAppenderBiz.addEvent(auth.getUserId(),
							Dnp3UserEvents.eventWithEntity(config, INSTRUCTION_TAGS,
									opDescription + " control operation failed.",
									Map.of(MESSAGE_DATA_KEY, e.getMessage()), ERROR_TAG));
					log.error(
							"Error processing DNP3 outstation [{}] analog operation request {} on {}[{}] control [{}]",
							getUid(), opDescription, config.getType(), index, config.getControlId(), e);
				}
			};
			Executor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(task);
			} else {
				task.run();
			}
			return CommandStatus.SUCCESS;
		}
	}

	private ServerControlConfiguration controlConfigForIndex(ControlType controlType, int index) {
		MeasurementType measType = (controlType == ControlType.Analog
				? MeasurementType.AnalogOutputStatus
				: MeasurementType.BinaryOutputStatus);
		int binaryStatusOffset = typeConfigCount(measType, measurementTypes);
		int controlConfigIndex = index - binaryStatusOffset;
		List<ServerControlConfiguration> configs = getControlConfigs();
		if ( configs != null && controlConfigIndex < configs.size() ) {
			return configs.get(controlConfigIndex);
		}
		return null;
	}

	private static final String INSTRUCTION_TOPIC_SET_CONTROL_PARAMETER = "SetControlParameter";

	private InstructionStatus operateBinaryControl(ControlRelayOutputBlock command, int index,
			OperateType opType, ServerControlConfiguration config) {
		Instruction instr = null;
		switch (command.function) {
			case LATCH_ON:
				instr = new Instruction(INSTRUCTION_TOPIC_SET_CONTROL_PARAMETER, now());
				instr.addParameter(config.getControlId(), Boolean.TRUE.toString());
				break;

			case LATCH_OFF:
				instr = new Instruction(INSTRUCTION_TOPIC_SET_CONTROL_PARAMETER, now());
				instr.addParameter(config.getControlId(), Boolean.FALSE.toString());
				break;

			default:
				// nothing
		}
		return issueInstruction("CROB " + command.function, index, config, instr);
	}

	private InstructionStatus operateAnalogControl(Object type, int index, String opDescription,
			ServerControlConfiguration config, Number value) {
		Instruction instr = new Instruction(INSTRUCTION_TOPIC_SET_CONTROL_PARAMETER, now());
		instr.addParameter(config.getControlId(), value.toString());
		return issueInstruction(opDescription, index, config, instr);
	}

	private InstructionStatus issueInstruction(String opDescription, int index,
			ServerControlConfiguration config, Instruction instr) {
		InstructionStatus result = null;
		try {
			if ( instr != null ) {
				NodeInstruction entity = instructorBiz.queueInstruction(config.getNodeId(), instr);
				if ( entity != null ) {
					result = entity.toStatus();
				} else {
					result = createStatus(null, InstructionState.Declined, now(),
							createErrorResultParameters("Failed to queue instruction.", "DNP3.010"));
				}
			} else {
				result = createStatus(null, InstructionState.Declined, now(),
						createErrorResultParameters(
								"Control function %s not supported.".formatted(opDescription),
								"DNP3.011"));
			}
		} finally {
			log.info(
					"DNP3 outstation [{}] {} control operation request on {}[{}] node {} control [{}] result: {}",
					getUid(), opDescription, config.getType(), index, config.getNodeId(),
					config.getControlId(), result);
		}
		if ( result.getInstructionState() == InstructionState.Declined ) {
			userEventAppenderBiz.addEvent(auth.getUserId(),
					Dnp3UserEvents.eventWithEntity(config, INSTRUCTION_TAGS,
							opDescription + " control operation failed.", result.getResultParameters(),
							ERROR_TAG));
		} else {
			Map<String, Object> eventData = new LinkedHashMap<>(2);
			eventData.put(TOPIC_DATA_KEY, instr.getTopic());
			if ( instr.getParameters() != null && !instr.getParameters().isEmpty() ) {
				eventData.put(VALUE_DATA_KEY, instr.getParameters().get(0).getValue());
			}
			userEventAppenderBiz.addEvent(auth.getUserId(),
					Dnp3UserEvents.eventWithEntity(config, INSTRUCTION_TAGS,
							opDescription + " control operation queued.", eventData, ERROR_TAG));
		}
		return result;
	}

	@Override
	public void accept(final ObjectDatum datum) {
		if ( datum == null ) {
			return;
		}
		final Executor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(() -> {
				applyDatumCapturedUpdates(datum);
			});
		} else {
			applyDatumCapturedUpdates(datum);
		}
	}

	private void applyDatumCapturedUpdates(final Datum datum) {
		OutstationChangeSet changes = changeSetForDatumCapturedEvent(datum);
		if ( changes == null ) {
			return;
		}
		synchronized ( this ) {
			final Outstation station = this.outstation;
			if ( station != null ) {
				log.info("Applying changes to DNP3 [{}]", getUid());
				station.apply(changes);
			}
		}
	}

	private static <T> List<T> nonNullList(List<T> list) {
		return (list != null ? list : Collections.emptyList());
	}

	private OutstationChangeSet changeSetForDatumCapturedEvent(final Datum datum) {
		if ( datum == null || (measurementTypes.isEmpty() && controlTypes.isEmpty()) ) {
			return null;
		}
		final Long nodeId = datum.getObjectId();
		final String sourceId = datum.getSourceId();

		final List<ServerMeasurementConfiguration> measurementConfigs = nonNullList(
				datumMeasurements.containsKey(nodeId) ? datumMeasurements.get(nodeId).get(sourceId)
						: null);
		final List<ServerControlConfiguration> controlConfigs = nonNullList(
				datumControls.containsKey(nodeId) ? datumControls.get(nodeId).get(sourceId) : null);
		if ( measurementConfigs.isEmpty() && controlConfigs.isEmpty() ) {
			return null;
		}

		final Instant timestamp = datum.getTimestamp();
		if ( timestamp == null ) {
			return null;
		}
		final long ts = timestamp.toEpochMilli();

		final Map<Entity<? extends CompositeKey3<Long, Long, ?>>, Object> updatedValues = new LinkedHashMap<>(
				measurementConfigs.size() + controlConfigs.size());

		OutstationChangeSet changes = null;

		for ( ServerMeasurementConfiguration config : measurementConfigs ) {
			Object propVal = datum.asSampleOperations().findSampleValue(config.getProperty());
			if ( propVal instanceof Number propNum ) {
				if ( config.getMultiplier() != null ) {
					propNum = applyUnitMultiplier(propNum, config.getMultiplier());
				}
				if ( config.getScale() != null ) {
					propNum = applyDecimalScale(propNum, config.getScale());
				}
				propVal = propNum;
			}
			final int idx = measurementTypes.get(config.getType()).indexOf(config);
			if ( idx < 0 ) {
				// really shouldn't be here
				continue;
			}
			if ( changes == null ) {
				changes = new OutstationChangeSet();
			}
			log.debug("Updating DNP3 {}[{}] from [{}].{} -> {}", config.getType(), idx, sourceId,
					config.getProperty(), propVal);
			updatedValues.put(config, propVal);
			switch (config.getType()) {
				case AnalogInput -> {
					if ( propVal instanceof Number propNum ) {
						changes.update(new AnalogInput(propNum.doubleValue(),
								(byte) AnalogQuality.ONLINE.toType(), ts), idx);
					}
				}

				case AnalogOutputStatus -> {
					if ( propVal instanceof Number propNum ) {
						changes.update(new AnalogOutputStatus(propNum.doubleValue(),
								(byte) AnalogOutputStatusQuality.ONLINE.toType(), ts), idx);
					}
				}

				case BinaryInput -> changes.update(new BinaryInput(booleanPropertyValue(propVal),
						(byte) BinaryQuality.ONLINE.toType(), ts), idx);

				case BinaryOutputStatus -> changes
						.update(new BinaryOutputStatus(booleanPropertyValue(propVal),
								(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts), idx);

				case Counter -> {
					if ( propVal instanceof Number propNum ) {
						changes.update(new Counter(propNum.longValue(),
								(byte) CounterQuality.ONLINE.toType(), ts), idx);
					}
				}

				case DoubleBitBinaryInput -> changes.update(new DoubleBitBinaryInput(
						booleanPropertyValue(propVal) ? DoubleBit.DETERMINED_ON
								: DoubleBit.DETERMINED_OFF,
						(byte) DoubleBitBinaryQuality.ONLINE.toType(), ts), idx);

				case FrozenCounter -> {
					if ( propVal instanceof Number propNum ) {
						changes.update(new FrozenCounter(propNum.longValue(),
								(byte) FrozenCounterQuality.ONLINE.toType(), ts), idx);
					}
				}
			}
		}

		final int analogStatusOffset = typeConfigCount(MeasurementType.AnalogOutputStatus,
				measurementTypes);
		final int binaryStatusOffset = typeConfigCount(MeasurementType.BinaryOutputStatus,
				measurementTypes);

		for ( ServerControlConfiguration config : controlConfigs ) {
			final Object controlVal = controlValue(datum, config.getProperty());
			final int idx = controlTypes.get(config.getType()).indexOf(config);
			if ( idx < 0 ) {
				// really shouldn't be here
				continue;
			}
			if ( changes == null ) {
				changes = new OutstationChangeSet();
			}
			int index = (config.getType() == ControlType.Analog ? analogStatusOffset
					: binaryStatusOffset) + idx;
			log.debug("Updating DNP3 control {}[{}] from [{}].value -> {}", config.getType(), index,
					sourceId, controlVal);
			updatedValues.put(config, controlVal);
			switch (config.getType()) {
				case Analog -> {
					try {
						Number n = null;
						if ( controlVal instanceof Number controlNum ) {
							n = controlNum;
						} else {
							n = new BigDecimal(controlVal.toString());
						}
						changes.update(new AnalogOutputStatus(n.doubleValue(),
								(byte) AnalogOutputStatusQuality.ONLINE.toType(), ts), index);
					} catch ( NumberFormatException e ) {
						log.warn("Cannot convert control [{}] value [{}] to number: {}", sourceId,
								controlVal, e.getMessage());
					}
				}

				case Binary -> changes.update(new BinaryOutputStatus(booleanPropertyValue(controlVal),
						(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts), index);
			}
		}

		if ( !updatedValues.isEmpty() ) {
			List<Map<String, Object>> updates = new ArrayList<>(updatedValues.size());
			for ( var e : updatedValues.entrySet() ) {
				Map<String, Object> update = Dnp3UserEvents.eventDataForEntity(e.getKey());
				update.remove(SERVER_ID_DATA_KEY);
				update.put(VALUE_DATA_KEY, e.getValue());
				updates.add(update);
			}
			userEventAppenderBiz.addEvent(auth.getUserId(), Dnp3UserEvents.eventWithEntity(auth,
					DATUM_TAGS, "Processing datum updates.", Map.of(UPDATE_LIST_DATA_KEY, updates)));
		}

		return changes;
	}

	/**
	 * Extract a control value from a datum.
	 * 
	 * <p>
	 * If {@code property} is given, return that value. Otherwise try the
	 * node-default "val" first, followed by "value". If neither of those work,
	 * return the first-available status property.
	 * </p>
	 * 
	 * @param datum
	 *        the datum
	 * @param property
	 *        the optional datum property name to extract
	 * @return the control value, or {@literal null}
	 */
	private static final Object controlValue(final Datum datum, final String property) {
		final DatumSamplesOperations ops = datum.asSampleOperations();
		if ( property != null && !property.isBlank() ) {
			return ops.findSampleValue(property);
		}
		Object v = ops.findSampleValue("val");
		if ( v != null ) {
			return v;
		}
		v = ops.findSampleValue("value");
		if ( v != null ) {
			return v;
		}
		Map<String, ?> status = ops.getSampleData(DatumSamplesType.Status);
		if ( status != null && !status.isEmpty() ) {
			v = status.values().iterator().next();
		}
		return v;
	}

	private <T, C> int typeConfigCount(T key, Map<T, List<C>> map) {
		if ( map == null || map.isEmpty() ) {
			return 0;
		}
		List<?> list = map.get(key);
		return (list != null ? list.size() : 0);
	}

	private boolean booleanPropertyValue(Object propVal) {
		if ( propVal instanceof Boolean ) {
			return ((Boolean) propVal).booleanValue();
		} else if ( propVal instanceof Number ) {
			return ((Number) propVal).intValue() == 0 ? false : true;
		} else {
			return StringUtils.parseBoolean(propVal.toString());
		}
	}

	private Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	private Outstation createOutstation() {
		TcpServerChannelConfiguration conf = new TcpServerChannelConfiguration();
		conf.setBindAddress(bindAddress);
		conf.setPort(port);
		try {
			Channel channel = channel(conf);
			if ( channel == null ) {
				log.info("DNP3 channel not available for outstation [{}]", getUid());
				return null;
			}
			log.info("Initializing DNP3 outstation [{}]", getUid());
			return channel.addOutstation(getUid(), commandHandler, app, createOutstationStackConfig());
		} catch ( DNP3Exception e ) {
			log.error("Error creating outstation application [{}]: {}", getUid(), e.getMessage(), e);
			return null;
		}
	}

	private Channel channel(TcpServerChannelConfiguration configuration) throws DNP3Exception {
		return getManager().addTCPServer(auth.getIdentifier(), configuration.getLogLevels(),
				ServerAcceptMode.CloseNew, configuration.getBindAddress(), configuration.getPort(),
				this);
	}

	private OutstationStackConfig createOutstationStackConfig() {
		OutstationStackConfig config = new OutstationStackConfig(
				createDatabaseConfig(measurementTypes, controlTypes),
				createEventBufferConfig(measurementTypes, controlTypes));
		copySettings(getLinkLayerConfig(), config.linkConfig);
		copySettings(getOutstationConfig(), config.outstationConfig);
		return config;
	}

	private Map<Long, Map<String, List<ServerMeasurementConfiguration>>> createDatumMeasurements() {
		Map<Long, Map<String, List<ServerMeasurementConfiguration>>> map = new LinkedHashMap<>(
				measurementConfigs.size());
		for ( ServerMeasurementConfiguration config : measurementConfigs ) {
			if ( config.isValid() ) {
				map.computeIfAbsent(config.getNodeId(), k -> new HashMap<>(measurementConfigs.size()))
						.computeIfAbsent(config.getSourceId(), k -> new ArrayList<>(4)).add(config);
			}
		}
		return map;
	}

	private Map<Long, Map<String, List<ServerControlConfiguration>>> createDatumControls() {
		Map<Long, Map<String, List<ServerControlConfiguration>>> map = new LinkedHashMap<>(
				measurementConfigs.size());
		for ( ServerControlConfiguration config : controlConfigs ) {
			if ( config.isValid() ) {
				map.computeIfAbsent(config.getNodeId(), k -> new HashMap<>(measurementConfigs.size()))
						.computeIfAbsent(config.getControlId(), k -> new ArrayList<>(4)).add(config);
			}
		}
		return map;
	}

	private Map<MeasurementType, List<ServerMeasurementConfiguration>> createMeasurementTypeMap() {
		Map<MeasurementType, List<ServerMeasurementConfiguration>> map = new LinkedHashMap<>(
				measurementConfigs.size());
		for ( ServerMeasurementConfiguration config : measurementConfigs ) {
			MeasurementType type = config.getType();
			if ( type != null && config.getProperty() != null && !config.getProperty().isEmpty() ) {
				map.computeIfAbsent(type, k -> new ArrayList<>(4)).add(config);
			}
		}
		return map;
	}

	private Map<ControlType, List<ServerControlConfiguration>> createControlTypeMap() {
		Map<ControlType, List<ServerControlConfiguration>> map = new LinkedHashMap<>(
				controlConfigs.size());
		for ( ServerControlConfiguration config : controlConfigs ) {
			ControlType type = config.getType();
			if ( type != null && config.getControlId() != null && !config.getControlId().isEmpty() ) {
				map.computeIfAbsent(type, k -> new ArrayList<>(4)).add(config);
			}
		}
		return map;
	}

	private void appendMeasurementInfos(StringBuilder buf, MeasurementType type,
			List<ServerMeasurementConfiguration> list) {
		buf.append(type.getTitle()).append(" (").append(list != null ? list.size() : 0).append(")\n");
		if ( list != null ) {
			int i = 0;
			for ( ServerMeasurementConfiguration conf : list ) {
				buf.append(String.format("  %3d: %s.%s\n", i, conf.getSourceId(), conf.getProperty()));
				i++;
			}
		}
	}

	private void appendControlInfos(StringBuilder buf, ControlType type,
			List<ServerControlConfiguration> list, int offset) {
		buf.append(type.getTitle()).append(" output status (").append(list != null ? list.size() : 0)
				.append(")\n");
		if ( list != null ) {
			int i = 0;
			for ( ServerControlConfiguration conf : list ) {
				buf.append(String.format("  %3d: %s\n", i + offset, conf.getControlId()));
				i++;
			}
		}
	}

	private DatabaseConfig createDatabaseConfig(
			Map<MeasurementType, List<ServerMeasurementConfiguration>> configs,
			Map<ControlType, List<ServerControlConfiguration>> controlConfigs) {
		int analogCount = 0;
		int aoStatusCount = 0;
		int binaryCount = 0;
		int boStatusCount = 0;
		int counterCount = 0;
		int doubleBinaryCount = 0;
		int frozenCounterCount = 0;
		StringBuilder infoBuf = new StringBuilder();
		if ( configs != null ) {
			for ( Map.Entry<MeasurementType, List<ServerMeasurementConfiguration>> me : configs
					.entrySet() ) {
				MeasurementType type = me.getKey();
				List<ServerMeasurementConfiguration> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case AnalogInput:
						analogCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case AnalogOutputStatus:
						aoStatusCount = list.size();
						break;

					case BinaryInput:
						binaryCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case BinaryOutputStatus:
						boStatusCount = list.size();
						break;

					case Counter:
						counterCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case DoubleBitBinaryInput:
						doubleBinaryCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case FrozenCounter:
						frozenCounterCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;
				}
			}
		}
		if ( controlConfigs != null ) {
			for ( Map.Entry<ControlType, List<ServerControlConfiguration>> me : controlConfigs
					.entrySet() ) {
				ControlType type = me.getKey();
				List<ServerControlConfiguration> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case Analog:
						appendControlInfos(infoBuf, type, list, aoStatusCount);
						aoStatusCount += list.size();
						break;

					case Binary:
						appendControlInfos(infoBuf, type, list, boStatusCount);
						boStatusCount += list.size();
						break;

				}
			}
		}
		log.info("DNP3 outstation [{}] database configured with following registers:\n{}", getUid(),
				infoBuf);
		return new DatabaseConfig(binaryCount, doubleBinaryCount, analogCount, counterCount,
				frozenCounterCount, boStatusCount, aoStatusCount);
	}

	private EventBufferConfig createEventBufferConfig(
			Map<MeasurementType, List<ServerMeasurementConfiguration>> configs,
			Map<ControlType, List<ServerControlConfiguration>> controlConfigs) {
		EventBufferConfig config = EventBufferConfig.allTypes(0);
		final int size = getEventBufferSize();
		if ( configs != null ) {
			for ( Map.Entry<MeasurementType, List<ServerMeasurementConfiguration>> me : configs
					.entrySet() ) {
				MeasurementType type = me.getKey();
				List<ServerMeasurementConfiguration> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case AnalogInput:
						config.maxAnalogEvents = size;
						break;

					case AnalogOutputStatus:
						config.maxAnalogOutputStatusEvents = size;
						break;

					case BinaryInput:
						config.maxBinaryEvents = size;
						break;

					case BinaryOutputStatus:
						config.maxBinaryOutputStatusEvents = size;
						break;

					case Counter:
						config.maxCounterEvents = size;
						break;

					case DoubleBitBinaryInput:
						config.maxDoubleBinaryEvents = size;
						break;

					case FrozenCounter:
						config.maxFrozenCounterEvents = size;
						break;
				}
			}
		}
		if ( controlConfigs != null ) {
			for ( Map.Entry<ControlType, List<ServerControlConfiguration>> me : controlConfigs
					.entrySet() ) {
				ControlType type = me.getKey();
				List<ServerControlConfiguration> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case Analog:
						config.maxAnalogOutputStatusEvents = size;
						break;

					case Binary:
						config.maxBinaryOutputStatusEvents = size;
						break;

				}
			}
		}
		return config;
	}

	/**
	 * Get the DNP3 manager.
	 * 
	 * @return the manager
	 */
	public DNP3Manager getManager() {
		return manager;
	}

	/**
	 * Get the measurement configurations.
	 * 
	 * @return the configurations
	 */
	public List<ServerMeasurementConfiguration> getMeasurementConfigs() {
		return measurementConfigs;
	}

	/**
	 * Get the control configurations.
	 * 
	 * @return the configurations
	 */
	public List<ServerControlConfiguration> getControlConfigs() {
		return controlConfigs;
	}

	/**
	 * Get the link layer configuration.
	 * 
	 * @return the configuration
	 */
	public LinkLayerConfig getLinkLayerConfig() {
		return linkLayerConfig;
	}

	/**
	 * Get the Outstation configuration.
	 * 
	 * @return the configuration
	 */
	public OutstationConfig getOutstationConfig() {
		return outstationConfig;
	}

	/**
	 * Get the task executor.
	 * 
	 * @return the task executor
	 */
	public Executor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 * 
	 * @param taskExecutor
	 *        the task executor to set
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Get the event buffer size.
	 * 
	 * <p>
	 * This buffer is used by DNP3 to hold updated values.
	 * </p>
	 * 
	 * @return the buffer size, defaults to {@link #DEFAULT_EVENT_BUFFER_SIZE}
	 */
	public int getEventBufferSize() {
		return eventBufferSize;
	}

	/**
	 * Set the event buffer size.
	 * 
	 * @param eventBufferSize
	 *        the buffer size to set
	 */
	public void setEventBufferSize(int eventBufferSize) {
		if ( eventBufferSize < 0 ) {
			return;
		}
		this.eventBufferSize = eventBufferSize;
	}

	/**
	 * Get the startup delay, in seconds.
	 * 
	 * @return the delay; defaults to {@link #DEFAULT_STARTUP_DELAY_SECONDS}
	 */
	public int getStartupDelaySecs() {
		return startupDelaySecs;
	}

	/**
	 * Set the startup delay, in seconds.
	 * 
	 * <p>
	 * This delay is used to allow the class to be configured fully before
	 * starting.
	 * </p>
	 * 
	 * @param startupDelaySecs
	 *        the delay
	 */
	public void setStartupDelaySecs(int startupDelaySecs) {
		this.startupDelaySecs = startupDelaySecs;
	}

}
