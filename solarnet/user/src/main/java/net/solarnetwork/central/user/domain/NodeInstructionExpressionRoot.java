/* ==================================================================
 * NodeInstructionExpressionRoot.java - 19/11/2025 7:34:38 am
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

package net.solarnetwork.central.user.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.domain.DatumCollectionFunctions;
import net.solarnetwork.central.datum.domain.DatumExpressionRoot;
import net.solarnetwork.central.datum.domain.DatumHttpFunctions;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumDateFunctions;
import net.solarnetwork.domain.datum.DatumMathFunctions;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumStringFunctions;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import tools.jackson.databind.JsonNode;

/**
 * Expression root object for {@link NodeInstruction} processing.
 * 
 * @author matt
 * @version 2.0
 */
public class NodeInstructionExpressionRoot implements DatumCollectionFunctions, DatumDateFunctions,
		DatumHttpFunctions, DatumMathFunctions, DatumStringFunctions {

	private final SolarNodeOwnership owner;
	private final NodeInstruction instruction;
	private final Map<String, Object> parameters;
	private final DatumStreamsAccessor datumStreamsAccessor;
	private final HttpOperations httpOperations;

	// a function to lookup user metadata based on user ID
	private final Function<Long, DatumMetadataOperations> userMetadataProvider;

	// a function to lookup node or datum stream metadata based on an object ID
	private final Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider;

	// a function to parse a metadata tariff schedule at a path
	private final BiFunction<DatumMetadataOperations, String, TariffSchedule> tariffScheduleProvider;

	// a function to return decrypted user secrets based on a user ID and key
	private final BiFunction<Long, String, byte[]> secretProvider;

	// dynamic runtime data to pass to runtime services
	private Map<String, Object> runtimeData;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *        the owner
	 * @param instruction
	 *        the instruction
	 * @param parameters
	 *        the parameters; a copy of the map will be created, or a new empty
	 *        map if {@code null}
	 * @param datumStreamsAccessor
	 *        the datum streams accessor
	 * @param httpOperations
	 *        optional HTTP operations
	 * @param userMetadataProvider
	 *        function that resolves user metadata; it will be passed the user
	 *        ID provided by {@code owner}
	 * @param metadataProvider
	 *        function that resolves either node or datum stream metadata based
	 *        on an ID; the {@code kind} component will always be {@code Node},
	 *        the {@code objectId} component will be the node ID provided by
	 *        {@code owner}, and the {@code sourceId} component will either be
	 *        {@code null} to represent node metadata or a source ID to
	 *        represent datum stream metadata
	 * @param tariffScheduleProvider
	 *        function that resolves a {@link TariffSchedule} from metadata
	 *        located at a path; if not provided a default resolver will be used
	 * @param secretProvider
	 *        function that resolves a decrypted user secret; it will be passed
	 *        the user ID provided by {@code owner}
	 * @throws IllegalArgumentException
	 *         if {@code owner} or {@code instruction} are {@code null}
	 */
	public NodeInstructionExpressionRoot(SolarNodeOwnership owner, NodeInstruction instruction,
			Map<String, ?> parameters, DatumStreamsAccessor datumStreamsAccessor,
			HttpOperations httpOperations, Function<Long, DatumMetadataOperations> userMetadataProvider,
			Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider,
			BiFunction<DatumMetadataOperations, String, TariffSchedule> tariffScheduleProvider,
			BiFunction<Long, String, byte[]> secretProvider) {
		super();
		this.owner = requireNonNullArgument(owner, "owner");
		this.instruction = requireNonNullArgument(instruction, "instruction");
		this.parameters = (parameters != null ? new LinkedHashMap<>(parameters)
				: new LinkedHashMap<>(4));
		this.datumStreamsAccessor = datumStreamsAccessor;
		this.httpOperations = httpOperations;
		this.userMetadataProvider = userMetadataProvider;
		this.metadataProvider = metadataProvider;
		this.tariffScheduleProvider = tariffScheduleProvider;
		this.secretProvider = secretProvider;
	}

	/*
	 * ========================================================================
	 * Properties
	 * ========================================================================
	 */

	/**
	 * Get the user ID.
	 *
	 * @return the user ID
	 */
	public Long getUserId() {
		return owner.getUserId();
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return owner.getNodeId();
	}

	/**
	 * Get the node's ISO 3166-1 alpha-2 character country code.
	 * 
	 * @return 2-character country code
	 */
	public String getCountry() {
		return owner.getCountry();
	}

	/**
	 * Get the node's time zone.
	 * 
	 * @return the time zone
	 */
	public ZoneId getZone() {
		return owner.getZone();
	}

	/**
	 * Get the instruction ID.
	 * 
	 * @return the instruction ID
	 */
	public Long getInstructionId() {
		return instruction.getId();
	}

	/**
	 * Get the instruction.
	 * 
	 * @return the instruction
	 */
	public Instruction getInstruction() {
		return instruction.getInstruction();
	}

	/**
	 * Get the parameters.
	 * 
	 * @return the parameters, never {@code null}
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	/*
	 * ========================================================================
	 * Metadata
	 * ========================================================================
	 */

	/**
	 * Get the user metadata.
	 *
	 * @return the user metadata, or {@code null} if none available
	 */
	public DatumMetadataOperations userMetadata() {
		return (userMetadataProvider != null ? userMetadataProvider.apply(owner.getUserId()) : null);
	}

	/**
	 * Extract a value from the user metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object userMetadata(final String path) {
		DatumMetadataOperations metadata = userMetadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Get the node metadata.
	 *
	 * @return the node metadata, or {@code null} if none available
	 */
	public DatumMetadataOperations nodeMetadata() {
		return (metadataProvider != null
				? metadataProvider.apply(new ObjectDatumStreamMetadataId(Node, owner.getNodeId(), null))
				: null);
	}

	/**
	 * Extract a value from the node metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object nodeMetadata(final String path) {
		DatumMetadataOperations metadata = nodeMetadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Get datum source metadata.
	 *
	 * @return the source metadata, or {@code null} if none available
	 */
	public DatumMetadataOperations sourceMetadata(final String sourceId) {
		if ( sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		return (metadataProvider != null
				? metadataProvider
						.apply(new ObjectDatumStreamMetadataId(Node, owner.getNodeId(), sourceId))
				: null);
	}

	/**
	 * Extract a value from source metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object sourceMetadata(final String sourceId, final String path) {
		DatumMetadataOperations metadata = sourceMetadata(sourceId);
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/*
	 * ========================================================================
	 * Locale
	 * ========================================================================
	 */

	/**
	 * Resolve a locale from a metadata path.
	 * 
	 * @param meta
	 *        the metadata ({@code null} allowed)
	 * @param path
	 *        the path to resolve the locale for
	 * @return a resolved locale, never {@code null}; defaults to English if no
	 *         other locale can be resolved (that includes a non-empty country
	 *         or language)
	 * @see DatumMetadataOperations#resolveLocale(String, Locale)
	 */
	public Locale resolveLocale(final DatumMetadataOperations meta, final String path) {
		if ( meta == null ) {
			return Locale.ENGLISH;
		}
		return meta.resolveLocale(path, Locale.ENGLISH);
	}

	/*
	 * ========================================================================
	 * Tariff Schedule
	 * ========================================================================
	 */

	/**
	 * Resolve a tariff schedule from metadata at a given path.
	 *
	 * @param meta
	 *        the metadata to resolve the tariff schedule from
	 * @param path
	 *        the metadata path to resolve the schedule at; the schedule can be
	 *        a CSV string or list of string arrays
	 * @return the schedule, or {@code node} if none available
	 */
	public TariffSchedule tariffSchedule(final DatumMetadataOperations meta, final String path) {
		if ( meta == null || meta.isEmpty() || path == null || path.isEmpty() ) {
			return null;
		}
		if ( tariffScheduleProvider != null ) {
			return tariffScheduleProvider.apply(meta, path);
		}

		TariffSchedule result = null;
		if ( result == null ) {
			Object tariffData = meta.metadataAtPath(path);
			if ( tariffData != null ) {
				Locale locale = resolveLocale(meta, path);
				try {
					result = TariffUtils.parseCsvTemporalRangeSchedule(locale, true, true, null,
							tariffData);
				} catch ( Exception e ) {
					String msg = "Error parsing tariff schedule at metadata path [%s]: %s"
							.formatted(path, e.getMessage());
					throw new IllegalArgumentException(msg);
				}
			}
		}
		return result;
	}

	/**
	 * Resolve the first available tariff schedule rate for "now" from metadata
	 * at a given path.
	 *
	 * @param meta
	 *        the metadata to resolve the tariff schedule from
	 * @param path
	 *        the metadata path to resolve the schedule at; the schedule can be
	 *        a CSV string or list of string arrays
	 * @return the first available rate for the current time, or {@code null} if
	 *         not available
	 */
	public BigDecimal resolveTariffScheduleRate(final DatumMetadataOperations meta, final String path) {
		return resolveTariffScheduleRate(meta, path, LocalDateTime.now(UTC), null);
	}

	/**
	 * Resolve the first available tariff schedule rate from metadata at a given
	 * path.
	 *
	 * @param meta
	 *        the metadata to resolve the tariff schedule from
	 * @param path
	 *        the metadata path to resolve the schedule at; the schedule can be
	 *        a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @return the first available rate, or {@code null} if not available
	 */
	public BigDecimal resolveTariffScheduleRate(final DatumMetadataOperations meta, final String path,
			final LocalDateTime date) {
		return resolveTariffScheduleRate(meta, path, date, null);
	}

	/**
	 * Resolve a tariff schedule rate from metadata at a given path.
	 *
	 * @param meta
	 *        the metadata to resolve the tariff schedule from
	 * @param path
	 *        the metadata path to resolve the schedule at; the schedule can be
	 *        a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @param rateName
	 *        the name of the rate to return, or {@code null} to return the
	 *        first available rate
	 * @return the rate, or {@code null} if not available
	 */
	public BigDecimal resolveTariffScheduleRate(final DatumMetadataOperations meta, final String path,
			final LocalDateTime date, final String rateName) {
		BigDecimal result = null;
		TariffSchedule schedule = tariffSchedule(meta, path);
		if ( schedule != null ) {
			Tariff t = schedule.resolveTariff(date, null);
			if ( t != null ) {
				Map<String, Tariff.Rate> rates = t.getRates();
				if ( !rates.isEmpty() ) {
					Tariff.Rate r = (rateName != null ? rates.get(rateName)
							: rates.values().iterator().next());
					if ( r != null ) {
						result = r.getAmount();
					}
				}

			}
		}
		return result;
	}

	/*
	 * ========================================================================
	 * User secrets
	 * ========================================================================
	 */

	/**
	 * Get a user-configured secret value as a string.
	 *
	 * @param key
	 *        the key of the secret to retrieve
	 * @return the secret value as a string, or {@code null}
	 */
	public String secret(String key) {
		byte[] secret = secretData(key);
		if ( secret == null ) {
			return null;
		}
		return new String(secret, UTF_8);
	}

	/**
	 * Get a user-configured secret value.
	 *
	 * <p>
	 * The {@code secretProvider} must have been provided to the constructor for
	 * this to look up the secret values.
	 * </p>
	 *
	 * @param key
	 *        the key of the secret to retrieve
	 * @return the secret value, or {@code null}
	 */
	public byte[] secretData(String key) {
		return (secretProvider != null ? secretProvider.apply(owner.getUserId(), key) : null);
	}

	/*
	 * ========================================================================
	 * HTTP
	 * ========================================================================
	 */

	/**
	 * Make an HTTP GET request for a JSON object and return the result as a
	 * map.
	 *
	 * @param uri
	 *        the URL to request
	 * @return the result, never {@literal null}
	 */
	public Result<Map<String, Object>> httpGet(String uri) {
		return httpGet(uri, null, null);
	}

	/**
	 * Make an HTTP GET request for a JSON object and return the result as a
	 * map.
	 *
	 * @param uri
	 *        the URL to request
	 * @param parameters
	 *        optional query parameters to include in the URL
	 * @return the result, never {@literal null}
	 */
	public Result<Map<String, Object>> httpGet(String uri, Map<String, ?> parameters) {
		return httpGet(uri, parameters, null);
	}

	/**
	 * Make an HTTP GET request for a JSON object and return the result as a
	 * map.
	 *
	 * @param uri
	 *        the URL to request
	 * @param parameters
	 *        optional query parameters to include in the URL
	 * @param headers
	 *        optional HTTP headers to include
	 * @return the result, never {@literal null}
	 */
	public Result<Map<String, Object>> httpGet(String uri, Map<String, ?> parameters,
			Map<String, ?> headers) {
		if ( httpOperations == null ) {
			return Result.error("IXR.00001", "HTTP not supported");
		}
		Result<JsonNode> res = httpOperations.httpGet(uri, parameters, headers, JsonNode.class, owner,
				runtimeData);
		if ( res == null ) {
			return Result.error();
		}
		Map<String, Object> data = null;
		if ( res.getData() != null ) {
			data = JsonUtils.getStringMapFromTree(res.getData());
		}
		return new Result<>(res.getSuccess(), res.getCode(), res.getMessage(), res.getErrors(), data);
	}

	/*
	 * ========================================================================
	 * Datum access
	 * ========================================================================
	 */

	/**
	 * Create a datum expression root with a given datum.
	 *
	 * @param datum
	 *        the datum
	 * @return the new expression root instance using {@code datum}
	 */
	private DatumExpressionRoot datumRoot(Datum datum) {
		return datumRoot(datum, null, null);
	}

	/**
	 * Create a datum expression root with given datum, samples, and parameters
	 * values.
	 *
	 * @param datum
	 *        the datum
	 * @param samples
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @return the new expression root instance using {@code datum},
	 *         {@code samples}, and {@code parameters}
	 */
	private DatumExpressionRoot datumRoot(Datum datum, DatumSamplesOperations samples,
			Map<String, ?> parameters) {
		return new DatumExpressionRoot(getUserId(), datum, samples, parameters, nodeMetadata(),
				datumStreamsAccessor, metadataProvider, (meta, id) -> {
					return tariffScheduleProvider != null
							? tariffScheduleProvider.apply(meta, id.getSourceId())
							: null;
				}, httpOperations, secretProvider);
	}

	/**
	 * Find datum streams matching a general query, source ID pattern, and
	 * optional tags.
	 *
	 * @param kind
	 *        the datum kind
	 * @param query
	 *        the general query, to match the stream name, location, etc.
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param tags
	 *        optional tags to match
	 * @return the matching datum stream metadata, never {@code null}
	 */
	public Collection<ObjectDatumStreamMetadata> findDatumStreams(ObjectDatumKind kind, String query,
			String sourceIdPattern, String... tags) {
		if ( datumStreamsAccessor == null ) {
			return null;
		}
		return datumStreamsAccessor.findStreams(kind, query, sourceIdPattern, tags);
	}

	/**
	 * Find the first available datum stream matching a general query, source ID
	 * pattern, and optional tags.
	 *
	 * @param kind
	 *        the datum kind
	 * @param query
	 *        the general query, to match the stream name, location, etc.
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param tags
	 *        optional tags to match
	 * @return the first matching datum stream metadata, or {@code null} if not
	 *         available
	 */
	public ObjectDatumStreamMetadata findDatumStream(ObjectDatumKind kind, String query,
			String sourceIdPattern, String... tags) {
		Collection<ObjectDatumStreamMetadata> result = findDatumStreams(kind, query, sourceIdPattern,
				tags);
		if ( result.isEmpty() ) {
			return null;
		}
		if ( result instanceof SequencedCollection<ObjectDatumStreamMetadata> l ) {
			return l.getFirst();
		}
		return result.iterator().next();
	}

	/**
	 * Find the first available location datum stream matching a general query,
	 * source ID pattern, and optional tags.
	 *
	 * @param query
	 *        the general query, to match the stream name, location, etc.
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param tags
	 *        optional tags to match
	 * @return the first matching datum stream metadata, or {@code null} if not
	 *         available
	 */
	public ObjectDatumStreamMetadata findLocDatumStream(String query, String sourceIdPattern,
			String... tags) {
		Collection<ObjectDatumStreamMetadata> result = findDatumStreams(ObjectDatumKind.Location, query,
				sourceIdPattern, tags);
		if ( result.isEmpty() ) {
			return null;
		}
		if ( result instanceof SequencedCollection<ObjectDatumStreamMetadata> l ) {
			return l.getFirst();
		}
		return result.iterator().next();
	}

	/**
	 * Get a datum matching the owner node ID and a specific source ID at a
	 * specific timestamp.
	 *
	 * @param sourceId
	 *        the source ID to find the datum for
	 * @param timestamp
	 *        the timestamp to find the datum for
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot datumAt(String sourceId, Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceId == null || sourceId.isEmpty()
				|| timestamp == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.at(Node, getNodeId(), sourceId, timestamp);
		return (d != null ? datumRoot(d) : null);
	}

	/**
	 * Get a datum matching a specific stream at a specific timestamp.
	 * 
	 * @param streamMeta
	 *        the stream metadata to find the datum for
	 * @param timestamp
	 *        the timestamp to find the datum for
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot datumAt(ObjectDatumStreamMetadata streamMeta, Instant timestamp) {
		if ( streamMeta == null ) {
			return null;
		}
		if ( datumStreamsAccessor == null || streamMeta == null || timestamp == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.at(streamMeta.getKind(), streamMeta.getObjectId(),
				streamMeta.getSourceId(), timestamp);
		return (d != null ? datumRoot(d) : null);
	}

	/**
	 * Get an offset from the latest available datum matching the owner node ID
	 * and a specific source ID.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot datumOffset(String sourceId, int offset, Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceId == null || timestamp == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.offset(Node, getNodeId(), sourceId, timestamp, offset);
		return (d != null ? datumRoot(d) : null);
	}

	/**
	 * Get an offset from the latest available datum matching a specific stream.
	 *
	 * @param streamMeta
	 *        the stream metadata to find the datum for
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot datumOffset(ObjectDatumStreamMetadata streamMeta, int offset,
			Instant timestamp) {
		if ( datumStreamsAccessor == null || streamMeta == null || timestamp == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.offset(streamMeta.getKind(), streamMeta.getObjectId(),
				streamMeta.getSourceId(), timestamp, offset);
		return (d != null ? datumRoot(d) : null);
	}

	/**
	 * Get the latest available datum matching a specific stream.
	 *
	 * @param streamMeta
	 *        the stream metadata to find the datum for
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot datumNear(ObjectDatumStreamMetadata streamMeta, Instant timestamp) {
		return datumOffset(streamMeta, 0, timestamp);
	}

	/**
	 * Get a datum matching the owner node ID and a specific source ID over a
	 * time range.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param from
	 *        the minimum datum timestamp (inclusive)
	 * @param to
	 *        the maximum datum timestamp (exclusive)
	 * @return the matching datum, never {@literal null}
	 */
	public Collection<DatumExpressionRoot> datumRange(String sourceIdPattern, Instant from, Instant to) {
		if ( datumStreamsAccessor == null || sourceIdPattern == null || sourceIdPattern.isEmpty()
				|| from == null || to == null ) {
			return null;
		}
		Collection<Datum> result = datumStreamsAccessor.rangeMatching(Node, getNodeId(), sourceIdPattern,
				from, to);
		return result.stream().map(this::datumRoot).toList();
	}

	/**
	 * Get a datum matching a specific stream over a time range.
	 *
	 * @param streamMeta
	 *        the stream metadata to find the datum for
	 * @param from
	 *        the minimum datum timestamp (inclusive)
	 * @param to
	 *        the maximum datum timestamp (exclusive)
	 * @return the matching datum, never {@literal null}
	 */
	public Collection<DatumExpressionRoot> datumRange(ObjectDatumStreamMetadata streamMeta, Instant from,
			Instant to) {
		if ( datumStreamsAccessor == null || streamMeta == null || from == null || to == null ) {
			return null;
		}
		Collection<Datum> result = datumStreamsAccessor.rangeMatching(streamMeta.getKind(),
				streamMeta.getObjectId(), streamMeta.getSourceId(), from, to);
		return result.stream().map(this::datumRoot).toList();
	}

	/**
	 * Get the runtime data.
	 * 
	 * @return the data
	 */
	public Map<String, Object> getRuntimeData() {
		return runtimeData;
	}

	/**
	 * Set the runtime data.
	 * 
	 * @param runtimeData
	 *        the data to set
	 */
	public void setRuntimeData(Map<String, Object> runtimeData) {
		this.runtimeData = runtimeData;
	}

}
