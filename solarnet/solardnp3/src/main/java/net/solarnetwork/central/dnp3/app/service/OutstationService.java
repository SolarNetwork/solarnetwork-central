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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
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
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * DNP3 Outstation service.
 * 
 * @author matt
 * @version 1.0
 */
public class OutstationService implements ServiceLifecycleObserver, ChannelListener {

	private static final Logger log = LoggerFactory.getLogger(OutstationService.class);

	/** The default event buffer size. */
	private static final int DEFAULT_EVENT_BUFFER_SIZE = 30;

	/** The default startup delay. */
	private static final int DEFAULT_STARTUP_DELAY_SECONDS = 5;

	/** The default uid value. */
	public static final String DEFAULT_UID = "DNP3 Outstation";

	private final LinkLayerConfig linkLayerConfig = new LinkLayerConfig(false);
	private final DNP3Manager manager;
	private final InstructorBiz instructorBiz;
	private final ServerAuthConfiguration auth;
	private final String bindAddress;
	private final int port;
	private final List<ServerMeasurementConfiguration> measurementConfigs;
	private final List<ServerControlConfiguration> controlConfigs;

	private final Application app;
	private final CommandHandler commandHandler;
	private final OutstationConfig outstationConfig;
	private final Map<MeasurementType, List<ServerMeasurementConfiguration>> measurementTypes;
	private final Map<ControlType, List<ServerControlConfiguration>> controlTypes;

	private TaskExecutor taskExecutor;
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;
	private int startupDelaySecs = DEFAULT_STARTUP_DELAY_SECONDS;

	private Outstation outstation;
	private ChannelState channelState = ChannelState.CLOSED;

	/**
	 * Constructor.
	 * 
	 * @param manager
	 *        the manager
	 * @param instructorBiz
	 *        the instructor service
	 * @param auth
	 *        the auth associated with the server
	 * @param bindAddress
	 *        the bind address, e.g. 127.0.0.1, localhost, etc.
	 * @param port
	 *        the listen port
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OutstationService(DNP3Manager manager, InstructorBiz instructorBiz,
			ServerAuthConfiguration auth, String bindAddress, int port,
			List<ServerMeasurementConfiguration> measurementConfigs,
			List<ServerControlConfiguration> controlConfigs) {
		super();
		this.manager = requireNonNullArgument(manager, "manager");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
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
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received CROB operation request {} on {}[{}] control [{}]",
					getUid(), command.function, config.getControlType(), index, config.getControlId());
			final TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							operateBinaryControl(command, index, opType, config);
						} catch ( Exception e ) {
							log.error(
									"Error processing DNP3 outstation [{}] operate request {} on {}[{}] control [{}]",
									getUid(), command.function, config.getControlType(), index,
									config.getControlId(), e);
						}
					}
				});
			} else {
				operateBinaryControl(command, index, opType, config);
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
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received analog operation request {} on {}[{}] control [{}]",
					getUid(), opDescription, config.getControlType(), index, config.getControlId());
			TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							operateAnalogControl(command, index, opDescription, config, value);
						} catch ( Exception e ) {
							log.error(
									"Error processing DNP3 outstation [{}] analog operation request {} on {}[{}] control [{}]",
									getUid(), opDescription, config.getControlType(), index,
									config.getControlId(), e);
						}
					}
				});
			} else {
				operateAnalogControl(command, index, opDescription, config, value);
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
				instr.addParameter(config.getControlId(), Boolean.TRUE.toString());
				break;

			default:
				// nothing
		}
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
								"Control function %s not supported.".formatted(command.function),
								"DNP3.011"));
			}
		} finally {
			log.info("DNP3 outstation [{}] CROB operation request {} on {}[{}] control [{}] result: {}",
					getUid(), command.function, config.getControlType(), index, config.getControlId(),
					result);
		}
		return result;
	}

	private InstructionStatus operateAnalogControl(Object command, int index, String opDescription,
			ServerControlConfiguration config, Number value) {
		Instruction instr = new Instruction(INSTRUCTION_TOPIC_SET_CONTROL_PARAMETER, now());
		instr.addParameter(config.getControlId(), value.toString());
		InstructionStatus result = null;
		try {
			NodeInstruction entity = instructorBiz.queueInstruction(config.getNodeId(), instr);
			if ( entity != null ) {
				result = entity.toStatus();
			} else {
				result = createStatus(null, InstructionState.Declined, now(),
						createErrorResultParameters("Failed to queue instruction.", "DNP3.012"));
			}
		} finally {
			log.info(
					"DNP3 outstation [{}] analog operation request {} on {}[{}] control [{}] result: {}",
					getUid(), opDescription, config.getControlType(), index, config.getControlId(),
					result);
		}
		return result;
	}

	private void handleDatumCapturedEvent(Datum datum) {
		if ( datum == null ) {
			return;
		}
		final TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(() -> {
				applyDatumCapturedUpdates(datum);
			});
		} else {
			applyDatumCapturedUpdates(datum);
		}
	}

	private void applyDatumCapturedUpdates(Datum datum) {
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

	private OutstationChangeSet changeSetForDatumCapturedEvent(final Datum datum) {
		if ( datum == null || (measurementTypes.isEmpty() && controlTypes.isEmpty()) ) {
			return null;
		}
		final String sourceId = datum.getSourceId();
		final Instant timestamp = datum.getTimestamp();
		if ( timestamp == null ) {
			return null;
		}
		final long ts = timestamp.toEpochMilli();
		final Map<String, ?> datumProps = datum.getSampleData();
		OutstationChangeSet changes = null;

		for ( Map.Entry<MeasurementType, List<ServerMeasurementConfiguration>> me : measurementTypes
				.entrySet() ) {
			MeasurementType type = me.getKey();
			List<ServerMeasurementConfiguration> list = me.getValue();
			for ( ListIterator<ServerMeasurementConfiguration> itr = list.listIterator(); itr
					.hasNext(); ) {
				ServerMeasurementConfiguration config = itr.next();
				if ( sourceId.equals(config.getSourceId()) ) {
					Object propVal = datumProps.get(config.getProperty());
					if ( propVal != null ) {
						if ( propVal instanceof Number ) {
							if ( config.getMultiplier() != null ) {
								propVal = applyUnitMultiplier((Number) propVal, config.getMultiplier());
							}
							if ( config.getScale() >= 0 ) {
								propVal = applyDecimalScale((Number) propVal, config.getScale());
							}
						}
						if ( changes == null ) {
							changes = new OutstationChangeSet();
						}
						log.debug("Updating DNP3 {}[{}] from [{}].{} -> {}", type, itr.previousIndex(),
								sourceId, config.getProperty(), propVal);
						switch (type) {
							case AnalogInput:
								if ( propVal instanceof Number ) {
									changes.update(
											new AnalogInput(((Number) propVal).doubleValue(),
													(byte) AnalogQuality.ONLINE.toType(), ts),
											itr.previousIndex());
								}
								break;

							case AnalogOutputStatus:
								if ( propVal instanceof Number ) {
									changes.update(
											new AnalogOutputStatus(((Number) propVal).doubleValue(),
													(byte) AnalogOutputStatusQuality.ONLINE.toType(),
													ts),
											itr.previousIndex());
								}
								break;

							case BinaryInput:
								changes.update(
										new BinaryInput(booleanPropertyValue(propVal),
												(byte) BinaryQuality.ONLINE.toType(), ts),
										itr.previousIndex());
								break;

							case BinaryOutputStatus:
								changes.update(
										new BinaryOutputStatus(booleanPropertyValue(propVal),
												(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts),
										itr.previousIndex());
								break;

							case Counter:
								if ( propVal instanceof Number ) {
									changes.update(
											new Counter(((Number) propVal).longValue(),
													(byte) CounterQuality.ONLINE.toType(), ts),
											itr.previousIndex());
								}
								break;

							case DoubleBitBinaryInput:
								changes.update(
										new DoubleBitBinaryInput(
												booleanPropertyValue(propVal) ? DoubleBit.DETERMINED_ON
														: DoubleBit.DETERMINED_OFF,
												(byte) DoubleBitBinaryQuality.ONLINE.toType(), ts),
										itr.previousIndex());
								break;

							case FrozenCounter:
								if ( propVal instanceof Number ) {
									changes.update(
											new FrozenCounter(((Number) propVal).longValue(),
													(byte) FrozenCounterQuality.ONLINE.toType(), ts),
											itr.previousIndex());
								}
								break;
						}
					}
				}
			}
		}

		int analogStatusOffset = typeConfigCount(MeasurementType.AnalogOutputStatus, measurementTypes);
		int binaryStatusOffset = typeConfigCount(MeasurementType.BinaryOutputStatus, measurementTypes);
		for ( Map.Entry<ControlType, List<ServerControlConfiguration>> me : controlTypes.entrySet() ) {
			ControlType type = me.getKey();
			List<ServerControlConfiguration> list = me.getValue();
			for ( ListIterator<ServerControlConfiguration> itr = list.listIterator(); itr.hasNext(); ) {
				ServerControlConfiguration config = itr.next();
				if ( sourceId.equals(config.getControlId()) ) {
					if ( changes == null ) {
						changes = new OutstationChangeSet();
					}

					int index = (type == ControlType.Analog ? analogStatusOffset : binaryStatusOffset)
							+ itr.previousIndex();

					Object propVal = datumProps.get("value");
					log.debug("Updating DNP3 control {}[{}] from [{}].value -> {}", type, index,
							sourceId, propVal);
					switch (type) {
						case Analog:
							try {
								Number n = null;
								if ( propVal instanceof Number ) {
									n = (Number) propVal;
								} else {
									n = new BigDecimal(propVal.toString());
								}
								changes.update(
										new AnalogOutputStatus(n.doubleValue(),
												(byte) AnalogOutputStatusQuality.ONLINE.toType(), ts),
										index);
							} catch ( NumberFormatException e ) {
								log.warn("Cannot convert control [{}] value [{}] to number: {}",
										sourceId, propVal, e.getMessage());
							}
							break;

						case Binary:
							changes.update(
									new BinaryOutputStatus(booleanPropertyValue(propVal),
											(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts),
									index);
							break;

					}
				}
			}
		}

		return changes;
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

	private Map<MeasurementType, List<ServerMeasurementConfiguration>> createMeasurementTypeMap() {
		Map<MeasurementType, List<ServerMeasurementConfiguration>> map = new LinkedHashMap<>(
				measurementConfigs.size());
		for ( ServerMeasurementConfiguration config : measurementConfigs ) {
			MeasurementType type = config.getMeasurementType();
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
			ControlType type = config.getControlType();
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
				buf.append(String.format("  %3d: %s\n", i, conf.getSourceId()));
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
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 * 
	 * @param taskExecutor
	 *        the task executor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
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
