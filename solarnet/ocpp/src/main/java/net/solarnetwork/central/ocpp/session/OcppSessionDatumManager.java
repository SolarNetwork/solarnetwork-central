/* ==================================================================
 * OcppSessionManager.java - 27/02/2020 7:56:35 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.session;

import static java.util.Collections.singleton;
import static net.solarnetwork.domain.datum.Datum.REVERSE_ACCUMULATING_SUFFIX_KEY;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.domain.datum.AcEnergyDatum;
import net.solarnetwork.domain.datum.AtmosphericDatum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.dao.PurgePostedChargeSessionsTask;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndInfo;
import net.solarnetwork.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.Phase;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.ocpp.service.AuthorizationException;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.cs.ChargeSessionManager;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * A {@link ChargeSessionManager} that generates datum from charge session
 * transaction data.
 * 
 * @author matt
 * @version 2.1
 */
public class OcppSessionDatumManager extends BasicIdentifiable
		implements ChargeSessionManager, SettingsChangeObserver, ServiceLifecycleObserver {

	/**
	 * Datum property name enumeration.
	 */
	public enum DatumProperty {

		/** An authorization token, e.g. RFID ID. */
		AuthorizationToken("token", DatumSamplesType.Status),

		/** The reservation ID. */
		ReservationId("reservationId", DatumSamplesType.Status),

		/** A charging session ID. */
		SessionId("sessionId", DatumSamplesType.Status),

		/** The session duration, in seconds. */
		SessionDuration("duration", DatumSamplesType.Status),

		/**
		 * The session end authorization token, if different from the starting
		 * token.
		 */
		SessionEndAuthorizationToken("endToken", DatumSamplesType.Status),

		/** The session end date. */
		SessionEndDate("endDate", DatumSamplesType.Status),

		/** The session end reason. */
		SessionEndReason("endReason", DatumSamplesType.Status),

		/** The charging session transaction ID. */
		TransactionId("transactionId", DatumSamplesType.Status),

		;

		private final String propertyName;
		private final DatumSamplesType classification;

		private DatumProperty(String propertyName, DatumSamplesType classification) {
			this.propertyName = propertyName;
			this.classification = classification;
		}

		/**
		 * Get the property name.
		 * 
		 * @return the property name
		 */
		public String getPropertyName() {
			return propertyName;
		}

		/**
		 * Get the property classification.
		 * 
		 * @return the classification
		 */
		public DatumSamplesType getClassification() {
			return classification;
		}

	}

	/** The default {@code maxTemperatureScale} value. */
	public static final int DEFAULT_MAX_TEMPERATURE_SCALE = 1;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AuthorizationService authService;
	private final ChargePointDao chargePointDao;
	private final ChargeSessionDao chargeSessionDao;
	private final DatumEntityDao datumDao;
	private final ChargePointSettingsDao chargePointSettingsDao;
	private DatumProcessor fluxPublisher;
	private String sourceIdTemplate = UserSettings.DEFAULT_SOURCE_ID_TEMPLATE;
	private int maxTemperatureScale = DEFAULT_MAX_TEMPERATURE_SCALE;
	private TaskScheduler taskScheduler;

	private final PurgePostedChargeSessionsTask purgePostedTask = new PurgePostedChargeSessionsTask();
	private ScheduledFuture<?> configurationFuture;
	private ScheduledFuture<?> purgePostedFuture;

	/**
	 * Constructor.
	 * 
	 * @param authService
	 *        the authorization service to use
	 * @param chargePointDao
	 *        the charge point DAO to use; its {@code objectType} must be
	 *        {@link CentralChargePoint}
	 * @param chargeSessionDao
	 *        the charge session DAO to use
	 * @param datumDao
	 *        the DAO for saving Datum
	 * @param chargePointSettingsDao
	 *        the charge point settings DAO
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppSessionDatumManager(AuthorizationService authService, ChargePointDao chargePointDao,
			ChargeSessionDao chargeSessionDao, DatumEntityDao datumDao,
			ChargePointSettingsDao chargePointSettingsDao) {
		super();
		this.authService = requireNonNullArgument(authService, "authService");
		if ( chargePointDao == null ) {
			throw new IllegalArgumentException("The chargePointDao parameter must not be null.");
		}
		if ( !CentralChargePoint.class.isAssignableFrom(chargePointDao.getObjectType()) ) {
			throw new IllegalArgumentException(
					"The chargePointDao objectType must be CentralChargePoint.");
		}
		this.chargePointDao = chargePointDao;
		this.chargeSessionDao = requireNonNullArgument(chargeSessionDao, "chargeSessionDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.chargePointSettingsDao = requireNonNullArgument(chargePointSettingsDao,
				"chargePointSettingsDao");
	}

	@Override
	public void serviceDidStartup() {
		reconfigure();

	}

	@Override
	public void serviceDidShutdown() {
		stopTasks();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}
		reconfigure();
	}

	private synchronized void stopTasks() {
		if ( purgePostedFuture != null ) {
			if ( !purgePostedFuture.isDone() ) {
				purgePostedFuture.cancel(true);
			}
			purgePostedFuture = null;
		}
	}

	private synchronized void reconfigure() {
		if ( taskScheduler != null ) {
			stopTasks();
			if ( configurationFuture != null ) {
				if ( !configurationFuture.isDone() ) {
					configurationFuture.cancel(true);
				}
			}
			configurationFuture = taskScheduler.schedule(new ConfigurationTask(),
					new Date(System.currentTimeMillis() + 1000));
		}
	}

	private final class ConfigurationTask implements Runnable {

		@Override
		public void run() {
			TaskScheduler scheduler = getTaskScheduler();
			if ( scheduler == null ) {
				return;
			}
			synchronized ( OcppSessionDatumManager.this ) {
				configurationFuture = null;
				stopTasks();
				int purgeHours = getPurgePostedChargeSessionsExpirationHours();
				if ( purgeHours > 0 ) {
					log.info("Scheduling OCPP posted charge session purge task at {} hours.",
							purgeHours);
					long purgeMs = TimeUnit.HOURS.toMillis(purgeHours) / 4;
					purgePostedFuture = scheduler.scheduleWithFixedDelay(purgePostedTask,
							new Date(System.currentTimeMillis() + purgeMs), purgeMs);
				}
			}
		}

	}

	private CentralChargePoint chargePoint(ChargePointIdentity identifier, String authId) {
		CentralChargePoint cp = (CentralChargePoint) chargePointDao.getForIdentity(identifier);
		if ( cp == null ) {
			throw new AuthorizationException(
					String.format("ChargePoint %s not available.", identifier.getIdentifier()),
					new AuthorizationInfo(authId, AuthorizationStatus.Invalid));
		}
		return cp;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargeSession startChargingSession(ChargeSessionStartInfo info)
			throws AuthorizationException {
		// check authorization
		AuthorizationInfo authInfo = authService.authorize(info.getChargePointId(),
				info.getAuthorizationId());
		if ( authInfo == null || AuthorizationStatus.Accepted != authInfo.getStatus() ) {
			throw new AuthorizationException(authInfo);
		}

		CentralChargePoint cp = chargePoint(info.getChargePointId(), info.getAuthorizationId());

		// check for existing session, e.g. ConcurrentTx
		ChargeSession sess = chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(),
				info.getConnectorId());
		if ( sess != null ) {
			throw new AuthorizationException(
					String.format("ChargeSession %s already active for Charge Point %s connector %d",
							sess.getId(), info.getChargePointId(), info.getConnectorId()),
					new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.ConcurrentTx));
		}

		// persist a new session and then re-load to get the generated transaction ID
		try {
			sess = new ChargeSession(UUID.randomUUID(), info.getTimestampStart(),
					info.getAuthorizationId(), cp.getId(), info.getConnectorId(), 0);
			sess = chargeSessionDao.get(chargeSessionDao.save(sess));
		} catch ( DataIntegrityViolationException e ) {
			// assume this is from no matching Charge Point for the given chargePointId value
			throw new AuthorizationException(new AuthorizationInfo(info.getAuthorizationId(),
					AuthorizationStatus.Invalid, null, null));
		}

		// generate Datum from start meter value

		// @formatter:off
		SampledValue reading = SampledValue.builder()
				.withSessionId(sess.getId())
				.withTimestamp(sess.getCreated())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue(String.valueOf(info.getMeterStart()))
				.build();
		// @formatter:on
		chargeSessionDao.addReadings(singleton(reading));

		ChargePointSettings cps = settingsForChargePoint(cp.getUserId(), cp.getId());
		publishDatum(datum(cp, cps, sess, reading));

		return sess;
	}

	private void publishDatum(Datum d) {
		if ( d != null ) {
			if ( d.settings.isPublishToSolarIn() ) {
				datumDao.store(d);
			}
			if ( d.settings.isPublishToSolarFlux() ) {
				final DatumProcessor publisher = getFluxPublisher();
				if ( publisher != null && publisher.isConfigured() ) {
					publisher.processDatum(d);
				}
			}
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ChargeSession getActiveChargingSession(ChargePointIdentity identifier, int transactionId)
			throws AuthorizationException {
		ChargePoint cp = chargePoint(identifier, null);
		return chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(), transactionId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<ChargeSession> getActiveChargingSessions(ChargePointIdentity identifier) {
		if ( identifier != null ) {
			ChargePoint cp = chargePoint(identifier, null);
			return chargeSessionDao.getIncompleteChargeSessionsForChargePoint(cp.getId());
		}
		return chargeSessionDao.getIncompleteChargeSessions();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AuthorizationInfo endChargingSession(ChargeSessionEndInfo info) {
		CentralChargePoint cp = chargePoint(info.getChargePointId(), info.getAuthorizationId());
		ChargeSession sess = chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(),
				info.getTransactionId());
		if ( sess == null ) {
			throw new AuthorizationException("No active charging session found.", new AuthorizationInfo(
					info.getAuthorizationId(), AuthorizationStatus.Invalid, null, null));
		}

		sess.setEndAuthId(info.getAuthorizationId());
		sess.setEnded(info.getTimestampEnd());
		sess.setEndReason(info.getReason());
		sess.setPosted(Instant.now());
		chargeSessionDao.save(sess);

		// generate reading from end meter value

		// @formatter:off
		SampledValue reading = SampledValue.builder()
				.withSessionId(sess.getId())
				.withTimestamp(sess.getEnded())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue(String.valueOf(info.getMeterEnd()))
				.build();
		// @formatter:on

		// add all provided readings, plus our final TransactionEnd reading
		List<SampledValue> readings = new ArrayList<>();
		if ( info.getTransactionData() != null ) {
			for ( SampledValue v : info.getTransactionData() ) {
				readings.add(v);
			}
		}
		readings.add(reading);
		Map<UUID, ChargeSession> sessions = new HashMap<>(2);
		sessions.put(sess.getId(), sess);
		Map<ChargePointIdentity, CentralChargePoint> chargePoints = new HashMap<>(2);
		chargePoints.put(info.getChargePointId(), cp);
		addReadings(info.getChargePointId(), readings, sessions, chargePoints, new HashMap<>(2));

		return new AuthorizationInfo(info.getAuthorizationId(), AuthorizationStatus.Accepted, null,
				null);
	}

	/** Class to associate settings with datum. */
	private static class Datum extends GeneralNodeDatum {

		private static final long serialVersionUID = 7826346170884900517L;

		private final ChargePointSettings settings;

		private Datum(ChargePointSettings settings) {
			super();
			this.settings = settings;
		}

	}

	private Datum datum(CentralChargePoint chargePoint, ChargePointSettings chargePointSettings,
			ChargeSession sess, SampledValue reading) {
		final String sourceId = sourceId(chargePointSettings, chargePoint.getInfo().getId(),
				sess != null ? sess.getConnectorId() : 0, reading.getLocation());
		return datum(sourceId, chargePoint, chargePointSettings, sess, reading);
	}

	private Datum datum(String sourceId, CentralChargePoint chargePoint,
			ChargePointSettings chargePointSettings, ChargeSession sess, SampledValue reading) {
		Datum d = new Datum(chargePointSettings);
		d.setNodeId(chargePoint.getNodeId());
		d.setSamples(new DatumSamples());
		populateProperty(d, reading.getMeasurand(), reading.getUnit(), reading.getPhase(),
				reading.getValue());
		if ( d.getSamples() != null && !d.getSamples().isEmpty() ) {
			d.setCreated(reading.getTimestamp());
			d.setSourceId(sourceId(chargePointSettings, chargePoint.getInfo().getId(),
					sess != null ? sess.getConnectorId() : 0, reading.getLocation()));
			if ( sess != null ) {
				d.getSamples().putSampleValue(DatumProperty.AuthorizationToken.getClassification(),
						DatumProperty.AuthorizationToken.getPropertyName(), sess.getAuthId());
				// TODO - implement support for reservation ID
				//d.getSamples().putSampleValue(DatumProperty.ReservationId.getClassification(),
				//		DatumProperty.ReservationId.getPropertyName(), sess.getReservationId());
				d.getSamples().putSampleValue(DatumProperty.SessionId.getClassification(),
						DatumProperty.SessionId.getPropertyName(), sess.getId().toString());
				d.getSamples().putSampleValue(DatumProperty.TransactionId.getClassification(),
						DatumProperty.TransactionId.getPropertyName(),
						String.valueOf(sess.getTransactionId()));
				if ( sess.getEnded() != null ) {
					d.getSamples().putSampleValue(DatumProperty.SessionEndDate.getClassification(),
							DatumProperty.SessionEndDate.getPropertyName(),
							sess.getEnded().toEpochMilli());
				}
				d.getSamples().putSampleValue(
						DatumProperty.SessionEndAuthorizationToken.getClassification(),
						DatumProperty.SessionEndAuthorizationToken.getPropertyName(),
						sess.getEndAuthId());
				if ( sess.getEndReason() != null ) {
					d.getSamples().putSampleValue(DatumProperty.SessionEndReason.getClassification(),
							DatumProperty.SessionEndReason.getPropertyName(),
							sess.getEndReason().toString());
				}
				if ( sess.getCreated() != null
						&& (sess.getEnded() != null || reading.getTimestamp() != null) ) {
					Duration dur = Duration.between(sess.getCreated(),
							sess.getEnded() != null ? sess.getEnded() : reading.getTimestamp());
					d.getSamples().putSampleValue(DatumProperty.SessionDuration.getClassification(),
							DatumProperty.SessionDuration.getPropertyName(), dur.getSeconds());
				}
			}
			return d;
		}
		return null;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<SampledValue> getChargingSessionReadings(UUID sessionId) {
		return chargeSessionDao.findReadingsForSession(sessionId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addChargingSessionReadings(ChargePointIdentity chargePointId,
			Iterable<SampledValue> readings) {
		addReadings(chargePointId, readings, new HashMap<>(2), new HashMap<>(2), new HashMap<>(2));
	}

	// NOTE that the Map implementations passed here MUST support null key and values,
	// in order to support meter values not associated with a charge session
	private void addReadings(ChargePointIdentity chargePointId, Iterable<SampledValue> readings,
			Map<UUID, ChargeSession> sessions, Map<ChargePointIdentity, CentralChargePoint> chargePoints,
			Map<Long, ChargePointSettings> settings) {
		if ( readings == null ) {
			return;
		}
		Map<UUID, Set<SampledValue>> currentReadings = new HashMap<>(2);
		List<SampledValue> sorted = StreamSupport.stream(readings.spliterator(), false).sorted()
				.collect(Collectors.toList());
		List<SampledValue> newReadings = new ArrayList<>();
		for ( SampledValue r : sorted ) {
			final UUID sessionId = r.getSessionId(); // may be null
			Set<SampledValue> current = currentReadings.get(sessionId);
			if ( current == null ) {
				if ( sessionId != null && !sessions.containsKey(sessionId) ) {
					sessions.put(sessionId, chargeSessionDao.get(sessionId));
				}
				current = (sessionId != null ? new HashSet<>(getChargingSessionReadings(sessionId))
						: new HashSet<>(4));
				currentReadings.put(sessionId, current);
			}
			if ( !current.contains(r) ) {
				newReadings.add(r);
			}
		}
		if ( !newReadings.isEmpty() ) {
			chargeSessionDao.addReadings(newReadings);
			// group readings by timestamp  and source ID into Datum
			Map<String, Datum> datumBySourceId = new LinkedHashMap<>(4);
			for ( SampledValue reading : newReadings ) {
				CentralChargePoint cp = chargePoints.get(chargePointId);
				if ( cp == null ) {
					cp = (CentralChargePoint) chargePointDao.getForIdentity(chargePointId);
					if ( cp == null ) {
						throw new AuthorizationException(
								String.format("ChargePoint %s not available.", chargePointId),
								new AuthorizationInfo(chargePointId.getIdentifier(),
										AuthorizationStatus.Invalid));
					}
					chargePoints.put(chargePointId, cp);
				}
				final UUID sessionId = reading.getSessionId(); // may be null
				final ChargeSession s = sessions.get(sessionId);

				ChargePointSettings cps = settings.get(cp.getId());
				if ( cps == null ) {
					cps = settingsForChargePoint(cp.getUserId(), cp.getId());
					settings.put(cp.getId(), cps);
				}
				final String sourceId = sourceId(cps, cp.getInfo().getId(),
						s != null ? s.getConnectorId() : 0, reading.getLocation());
				Datum d = datumBySourceId.get(sourceId);
				if ( d == null || !d.getCreated().equals(reading.getTimestamp()) ) {
					if ( d != null ) {
						publishDatum(d);
						datumBySourceId.remove(sourceId);
						d = null;
					}

					d = datum(sourceId, cp, cps, s, reading);
					datumBySourceId.put(sourceId, d);
				} else {
					populateProperty(d, reading.getMeasurand(), reading.getUnit(), reading.getPhase(),
							reading.getValue());
				}
			}
			for ( Datum d : datumBySourceId.values() ) {
				publishDatum(d);
			}
		}
	}

	/**
	 * Resolve settings for a charge point.
	 * 
	 * @param id
	 *        the charge point ID
	 * @return the settings, never {@literal null}
	 */
	private ChargePointSettings settingsForChargePoint(Long userId, Long id) {
		ChargePointSettings cps = chargePointSettingsDao.resolveSettings(userId, id);
		if ( cps == null ) {
			// use default fallback
			cps = new ChargePointSettings(id, userId, Instant.now());
			cps.setSourceIdTemplate(sourceIdTemplate);
		}
		return cps;
	}

	/**
	 * Get the source ID template to use.
	 * 
	 * @param chargePointSettings
	 *        the settings
	 * @return the template, never {@literal null}
	 */
	private String sourceIdTemplate(ChargePointSettings chargePointSettings) {
		return chargePointSettings.getSourceIdTemplate() != null
				? chargePointSettings.getSourceIdTemplate()
				: sourceIdTemplate;
	}

	/**
	 * Resolve a datum source ID from configurable properties.
	 * 
	 * @param chargePointSettings
	 *        the settings
	 * @param identifier
	 *        the charge point identifier
	 * @param connectorId
	 *        the connector ID
	 * @param location
	 *        the location
	 * @return the source ID, never {@literal null}
	 */
	private String sourceId(ChargePointSettings chargePointSettings, String identifier, int connectorId,
			Location location) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("chargerIdentifier", identifier);
		params.put("chargePointId", chargePointSettings.getId());
		params.put("connectorId", connectorId);
		params.put("location", location);
		return UserSettings.removeEmptySourceIdSegments(
				StringUtils.expandTemplateString(sourceIdTemplate(chargePointSettings), params));
	}

	private void populateProperty(GeneralNodeDatum datum, Measurand measurand, UnitOfMeasure unit,
			Phase phase, Object value) {
		if ( value == null ) {
			return;
		}
		BigDecimal num = null;
		if ( value instanceof Number ) {
			num = NumberUtils.bigDecimalForNumber((Number) value);
		} else {
			try {
				num = new BigDecimal(value.toString());
			} catch ( NumberFormatException e ) {
				log.debug("Error parsing OCPP {} sampled value [{}]: {}", measurand, value,
						e.getMessage());
				return;
			}
		}
		num = normalizedUnit(num, unit);
		String propName = propertyName(measurand, phase);
		if ( propName != null ) {
			datum.getSamples().putSampleValue(propertyType(measurand), propName, num);
		}
	}

	private BigDecimal normalizedUnit(BigDecimal num, UnitOfMeasure unit) {
		if ( unit == null ) {
			return num;
		}
		switch (unit) {
			case Fahrenheit: {
				// convert to C
				BigDecimal celsius = num.subtract(new BigDecimal("32")).multiply(new BigDecimal("5"))
						.divide(new BigDecimal("9"));
				if ( maxTemperatureScale >= 0 && celsius.scale() > maxTemperatureScale ) {
					celsius = celsius.setScale(maxTemperatureScale, RoundingMode.HALF_UP);
				}
				return celsius;
			}

			case K: {
				BigDecimal celsius = num.subtract(new BigDecimal("273.15"));
				if ( maxTemperatureScale >= 0 && celsius.scale() > maxTemperatureScale ) {
					celsius = celsius.setScale(maxTemperatureScale, RoundingMode.HALF_UP);
				}
				return celsius;
			}

			case kVA:
			case kvar:
			case kvarh:
			case kW:
			case kWh:
				return num.movePointRight(3);

			default:
				return num;
		}
	}

	private DatumSamplesType propertyType(Measurand measurand) {
		switch (measurand) {
			case EnergyActiveExportRegister:
			case EnergyActiveImportRegister:
			case EnergyReactiveExportRegister:
			case EnergyReactiveImportRegister:
			case PowerReactiveExport:
			case PowerReactiveImport:
				return DatumSamplesType.Accumulating;

			default:
				return DatumSamplesType.Instantaneous;
		}
	}

	private String propertyName(Measurand measurand, Phase phase) {
		if ( phase == null || phase == Phase.Unknown ) {
			return propertyName(measurand);
		}
		StringBuilder buf = new StringBuilder(propertyName(measurand));
		buf.append('_');
		switch (phase) {
			case N:
				buf.append('n');
				break;

			case L1:
			case L1N:
				buf.append('a');
				break;

			case L2:
			case L2N:
				buf.append('b');
				break;

			case L3:
			case L3N:
				buf.append('c');
				break;

			case L1L2:
				buf.append("ab");
				break;

			case L2L3:
				buf.append("bc");
				break;

			case L3L1:
				buf.append("ca");
				break;

			case Unknown:
				// unreachable
				break;
		}
		return buf.toString();
	}

	private String propertyName(Measurand measurand) {
		switch (measurand) {
			case CurrentExport:
				return AcEnergyDatum.CURRENT_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case CurrentImport:
				return AcEnergyDatum.CURRENT_KEY;

			case CurrentOffered:
				return AcEnergyDatum.CURRENT_KEY + "Offered";

			case EnergyActiveExportInterval:
				return AcEnergyDatum.WATT_HOUR_READING_KEY + "Diff" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyActiveExportRegister:
				return AcEnergyDatum.WATT_HOUR_READING_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyActiveImportInterval:
				return AcEnergyDatum.WATT_HOUR_READING_KEY + "Diff";

			case EnergyActiveImportRegister:
				return AcEnergyDatum.WATT_HOUR_READING_KEY;

			case EnergyReactiveExportInterval:
				return "reactiveEnergyDiff" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyReactiveExportRegister:
				return "reactiveEnergy" + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case EnergyReactiveImportInterval:
				return "reactiveEnergyDiff";

			case EnergyReactiveImportRegister:
				return "reactiveEnergy";

			case Frequency:
				return AcEnergyDatum.FREQUENCY_KEY;

			case PowerActiveExport:
				return AcEnergyDatum.WATTS_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case PowerActiveImport:
				return AcEnergyDatum.WATTS_KEY;

			case PowerFactor:
				return AcEnergyDatum.POWER_FACTOR_KEY;

			case PowerOffered:
				return AcEnergyDatum.WATTS_KEY + "Offered";

			case PowerReactiveExport:
				return AcEnergyDatum.REACTIVE_POWER_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY;

			case PowerReactiveImport:
				return AcEnergyDatum.REACTIVE_POWER_KEY;

			case RPM:
				return "rpm";

			case SoC:
				return "soc";

			case Temperature:
				return AtmosphericDatum.TEMPERATURE_KEY;

			case Voltage:
				return AcEnergyDatum.VOLTAGE_KEY;

			default:
				return null;
		}
	}

	/**
	 * Get the SolarFlux publisher.
	 * 
	 * @return the publisher, or {@literal null}
	 */
	public DatumProcessor getFluxPublisher() {
		return fluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 * 
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public void setFluxPublisher(DatumProcessor fluxPublisher) {
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @return the template; defaults to
	 *         {@link UserSettings#DEFAULT_SOURCE_ID_TEMPLATE}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * <p>
	 * This template string allows for these parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><code>{chargePointId}</code> - the Charge Point ID (number)</li>
	 * <li><code>{chargerIdentifier}</code> - the Charge Point info identifier
	 * (string)</li>
	 * <li><code>{connectorId}</code> - the connector ID (integer)</li>
	 * <li><code>{location}</code> - the location (string)</li>
	 * </ol>
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	/**
	 * Get the maximum temperature decimal scale.
	 * 
	 * @return the maximum scale; defaults to
	 *         {@link #DEFAULT_MAX_TEMPERATURE_SCALE}
	 */
	public int getMaxTemperatureScale() {
		return maxTemperatureScale;
	}

	/**
	 * Set the maximum temperature decimal scale.
	 * 
	 * <p>
	 * This sets the maximum number of decimal digits for normalized temperature
	 * values. Set to {@literal -1} for no maximum.
	 * </p>
	 * 
	 * @param maxTemperatureScale
	 *        the maximum scale to set
	 */
	public void setMaxTemperatureScale(int maxTemperatureScale) {
		this.maxTemperatureScale = maxTemperatureScale;
	}

	/**
	 * Get the task scheduler.
	 * 
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 * 
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the number of hours after which posted charge sessions may be purged
	 * (deleted).
	 * 
	 * @return the posted charge sessions expiration time, in hours
	 */
	public int getPurgePostedChargeSessionsExpirationHours() {
		return purgePostedTask.getExpirationHours();
	}

	/**
	 * Set the number of hours after which posted charge sessions may be purged
	 * (deleted).
	 * 
	 * @param hours
	 *        posted charge sessions expiration time, in hours
	 */
	public void setPurgePostedChargeSessionsExpirationHours(int hours) {
		purgePostedTask.setExpirationHours(hours);
	}

}
