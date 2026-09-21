/* ==================================================================
 * TestTenant.java - 22/09/2026 8:39:25 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test.tenant;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.CommonDbUtils;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * A test user account with a standard set of nodes, sources, and tokens.
 *
 * <p>
 * Every tenant has:
 * </p>
 *
 * <ul>
 * <li>a user and a location</li>
 * <li>four nodes: a private node, another private node, a public node, and an
 * archived (private) node</li>
 * <li>three source IDs, in the form <code>/{name}/pwr/1</code>,
 * <code>/{name}/pwr/2</code>, and <code>/{name}/met/1</code>, with a datum
 * stream for every node and source ID combination</li>
 * <li>three active tokens: an unrestricted {@code User} token, a restricted
 * {@code User} token whose policy allows only the other private node and the
 * first source ID, and an unrestricted {@code ReadNodeData} token</li>
 * </ul>
 *
 * <p>
 * All IDs are random. Tests that target a tenant's data usually target its
 * private node, which the restricted token's policy does <b>not</b> allow.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class TestTenant {

	/** The country used for all tenant locations. */
	public static final String COUNTRY = "NZ";

	/** The time zone used for all tenant locations and streams. */
	public static final String TIME_ZONE = "Pacific/Auckland";

	/**
	 * A node datum stream.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 */
	public record NodeStream(UUID streamId, Long nodeId, String sourceId) {

	}

	private final String name;
	private final Long userId;
	private final String email;
	private final Long locationId;
	private final Long privateNodeId;
	private final Long otherPrivateNodeId;
	private final Long publicNodeId;
	private final Long archivedNodeId;
	private final List<Long> nodeIds;
	private final List<String> sourceIds;
	private final List<NodeStream> streams;
	private final Map<String, TestToken> tokens;
	private final TestToken userToken;
	private final TestToken restrictedUserToken;
	private final TestToken dataToken;
	private final TestActor userActor;
	private final TestActor tokenActor;
	private final TestActor restrictedTokenActor;
	private final TestActor dataTokenActor;
	private final TestActor nodeActor;

	/**
	 * Constructor.
	 *
	 * @param name
	 *        the tenant name, used in source IDs and actor names
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TestTenant(String name) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.userId = randomLong();
		this.email = "%s-%s@localhost".formatted(name.toLowerCase(Locale.ENGLISH), randomString());
		this.locationId = randomLong();
		this.privateNodeId = randomLong();
		this.otherPrivateNodeId = randomLong();
		this.publicNodeId = randomLong();
		this.archivedNodeId = randomLong();
		this.nodeIds = List.of(privateNodeId, otherPrivateNodeId, publicNodeId, archivedNodeId);
		this.sourceIds = List.of("/%s/pwr/1".formatted(name), "/%s/pwr/2".formatted(name),
				"/%s/met/1".formatted(name));

		final List<NodeStream> streams = new ArrayList<>(nodeIds.size() * sourceIds.size());
		for ( Long nodeId : nodeIds ) {
			for ( String sourceId : sourceIds ) {
				streams.add(new NodeStream(UUID.randomUUID(), nodeId, sourceId));
			}
		}
		this.streams = Collections.unmodifiableList(streams);

		this.tokens = Collections.synchronizedMap(new LinkedHashMap<>(8));
		this.userToken = newToken(SecurityTokenType.User, null);
		this.restrictedUserToken = newToken(SecurityTokenType.User,
				BasicSecurityPolicy.builder().withNodeIds(Set.of(otherPrivateNodeId))
						.withSourceIds(Set.of(sourceIds.getFirst())).build());
		this.dataToken = newToken(SecurityTokenType.ReadNodeData, null);

		this.userActor = TestActor.user(name + " user", userId, email);
		this.tokenActor = TestActor.token(name + " user token", userToken);
		this.restrictedTokenActor = TestActor.token(name + " restricted user token",
				restrictedUserToken);
		this.dataTokenActor = TestActor.token(name + " data token", dataToken);
		this.nodeActor = TestActor.node(name + " node", privateNodeId);
	}

	/**
	 * Create and register a new token for this tenant.
	 *
	 * <p>
	 * The token is not inserted into the database; call
	 * {@link TestToken#insert(JdbcOperations)} if needed.
	 * </p>
	 *
	 * @param type
	 *        the token type
	 * @param policy
	 *        the optional policy
	 * @return the new token
	 */
	public TestToken newToken(SecurityTokenType type, @Nullable SecurityPolicy policy) {
		final TestToken token = TestToken.randomToken(userId, type, policy);
		tokens.put(token.tokenId(), token);
		return token;
	}

	/**
	 * Insert the user, location, nodes, and all registered tokens into the
	 * database.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 */
	public void insert(JdbcOperations jdbcOps) {
		insertLocation(jdbcOps, locationId, COUNTRY, TIME_ZONE);
		insertUser(jdbcOps, userId, email, randomString(), name + " user");
		for ( Long nodeId : nodeIds ) {
			insertNode(jdbcOps, nodeId, locationId);
			insertUserNode(jdbcOps, userId, nodeId, "%s node %d".formatted(name, nodeId), null,
					isPrivate(nodeId), isArchived(nodeId));
		}
		for ( TestToken token : tokens() ) {
			token.insert(jdbcOps);
		}
	}

	/**
	 * Insert datum stream metadata for all node streams into the database.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 */
	public void insertStreams(JdbcOperations jdbcOps) {
		final List<ObjectDatumStreamMetadata> metas = streams.stream()
				.map(s -> (ObjectDatumStreamMetadata) new BasicObjectDatumStreamMetadata(s.streamId(),
						TIME_ZONE, ObjectDatumKind.Node, s.nodeId(), s.sourceId(),
						new String[] { "watts" }, new String[] { "wattHours" }, null))
				.toList();
		CommonDbUtils.insertObjectDatumStreamMetadata(null, jdbcOps, metas);
	}

	/**
	 * Test if this tenant owns a node.
	 *
	 * @param nodeId
	 *        the node ID
	 * @return {@code true} if this tenant owns the node
	 */
	public boolean ownsNode(@Nullable Long nodeId) {
		return nodeId != null && nodeIds.contains(nodeId);
	}

	/**
	 * Test if a node is private (requires authorization).
	 *
	 * @param nodeId
	 *        the node ID
	 * @return {@code true} if the node is private
	 */
	public boolean isPrivate(Long nodeId) {
		return !publicNodeId.equals(nodeId);
	}

	/**
	 * Test if a node is archived.
	 *
	 * @param nodeId
	 *        the node ID
	 * @return {@code true} if the node is archived
	 */
	public boolean isArchived(Long nodeId) {
		return archivedNodeId.equals(nodeId);
	}

	/**
	 * Get a stream for a node and source.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @return the stream
	 * @throws IllegalArgumentException
	 *         if the stream does not exist
	 */
	public NodeStream stream(Long nodeId, String sourceId) {
		for ( NodeStream s : streams ) {
			if ( s.nodeId().equals(nodeId) && s.sourceId().equals(sourceId) ) {
				return s;
			}
		}
		throw new IllegalArgumentException(
				"No stream for node %d source %s".formatted(nodeId, sourceId));
	}

	/**
	 * Get a registered token.
	 *
	 * @param tokenId
	 *        the token ID
	 * @return the token, or {@code null} if not registered
	 */
	public @Nullable TestToken token(String tokenId) {
		return tokens.get(tokenId);
	}

	/**
	 * Get all registered tokens.
	 *
	 * @return the tokens
	 */
	public Collection<TestToken> tokens() {
		synchronized ( tokens ) {
			return List.copyOf(tokens.values());
		}
	}

	/**
	 * Get the tenant name.
	 *
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * Get the user ID.
	 *
	 * @return the user ID
	 */
	public Long userId() {
		return userId;
	}

	/**
	 * Get the user email.
	 *
	 * @return the email
	 */
	public String email() {
		return email;
	}

	/**
	 * Get the location ID.
	 *
	 * @return the location ID
	 */
	public Long locationId() {
		return locationId;
	}

	/**
	 * Get the private node ID.
	 *
	 * <p>
	 * The restricted user token does not have access to this node.
	 * </p>
	 *
	 * @return the node ID
	 */
	public Long privateNodeId() {
		return privateNodeId;
	}

	/**
	 * Get the other private node ID.
	 *
	 * <p>
	 * The restricted user token has access to only this node.
	 * </p>
	 *
	 * @return the node ID
	 */
	public Long otherPrivateNodeId() {
		return otherPrivateNodeId;
	}

	/**
	 * Get the public node ID.
	 *
	 * @return the node ID
	 */
	public Long publicNodeId() {
		return publicNodeId;
	}

	/**
	 * Get the archived node ID.
	 *
	 * @return the node ID
	 */
	public Long archivedNodeId() {
		return archivedNodeId;
	}

	/**
	 * Get all node IDs.
	 *
	 * @return the node IDs, in private, other private, public, archived order
	 */
	public List<Long> nodeIds() {
		return nodeIds;
	}

	/**
	 * Get the source IDs used by every node.
	 *
	 * @return the source IDs
	 */
	public List<String> sourceIds() {
		return sourceIds;
	}

	/**
	 * Get all node streams.
	 *
	 * @return the streams
	 */
	public List<NodeStream> streams() {
		return streams;
	}

	/**
	 * Get the unrestricted {@code User} token.
	 *
	 * @return the token
	 */
	public TestToken userToken() {
		return userToken;
	}

	/**
	 * Get the restricted {@code User} token.
	 *
	 * <p>
	 * The token policy allows only the {@link #otherPrivateNodeId()} node and
	 * the first source ID.
	 * </p>
	 *
	 * @return the token
	 */
	public TestToken restrictedUserToken() {
		return restrictedUserToken;
	}

	/**
	 * Get the unrestricted {@code ReadNodeData} token.
	 *
	 * @return the token
	 */
	public TestToken dataToken() {
		return dataToken;
	}

	/**
	 * Get the user actor.
	 *
	 * @return the actor
	 */
	public TestActor userActor() {
		return userActor;
	}

	/**
	 * Get the unrestricted {@code User} token actor.
	 *
	 * @return the actor
	 */
	public TestActor tokenActor() {
		return tokenActor;
	}

	/**
	 * Get the restricted {@code User} token actor.
	 *
	 * @return the actor
	 */
	public TestActor restrictedTokenActor() {
		return restrictedTokenActor;
	}

	/**
	 * Get the unrestricted {@code ReadNodeData} token actor.
	 *
	 * @return the actor
	 */
	public TestActor dataTokenActor() {
		return dataTokenActor;
	}

	/**
	 * Get the private node actor.
	 *
	 * @return the actor
	 */
	public TestActor nodeActor() {
		return nodeActor;
	}

	@Override
	public String toString() {
		return "TestTenant{" + name + ",userId=" + userId + "}";
	}

}
