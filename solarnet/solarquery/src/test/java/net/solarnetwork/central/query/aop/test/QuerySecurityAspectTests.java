/* ==================================================================
 * QuerySecurityAspectTests.java - Mar 5, 2014 7:46:12 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.aop.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.privateOwnershipFor;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.query.aop.QuerySecurityAspect;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Unit tests for the {@link QuerySecurityAspect} class.
 * 
 * @author matt
 * @version 2.1
 */
public class QuerySecurityAspectTests {

	private static final Long TEST_USER_ID = -9999L;

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private DatumStreamMetadataDao streamMetadataDao;
	private QuerySecurityAspect service;

	@Before
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		streamMetadataDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		service = new QuerySecurityAspect(nodeOwnershipDao, streamMetadataDao);
		service.setNodeIdNotRequiredSet(new HashSet<>(Arrays.asList("price", "weather")));
	}

	@After
	public void teardown() {
		EasyMock.verify(nodeOwnershipDao, streamMetadataDao);
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private void replayAll(Object... others) {
		EasyMock.replay(nodeOwnershipDao, streamMetadataDao);
		if ( others != null ) {
			EasyMock.replay(others);
		}
	}

	private void setUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private AuthenticatedNode setAuthenticatedNode(final Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, null, false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(node, "foobar", "ROLE_NODE");
		setUser(auth);
		return node;
	}

	private SecurityToken setAuthenticatedUserToken(final Long userId, final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setUser(auth);
		return token;
	}

	private SecurityToken setAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.ReadNodeData, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setUser(auth);
		return token;
	}

	@Test
	public void datumFilterPublicNodeAsAuthenticatedNode() {
		AuthenticatedNode node = setAuthenticatedNode(-1L);
		SolarNodeOwnership ownership = ownershipFor(node.getNodeId(), TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(node.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(node.getNodeId());
		Filter result = service.userNodeAccessCheck(criteria);
		Assert.assertSame(criteria, result);
	}

	@Test
	public void datumFilterPublicNodeAsAnonymous() {
		SolarNodeOwnership ownership = ownershipFor(-1L, TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(ownership.getNodeId());
		Filter result = service.userNodeAccessCheck(criteria);
		Assert.assertSame(criteria, result);
	}

	@Test
	public void datumFilterPublicNodeAsSomeOtherNode() {
		setAuthenticatedNode(-2L);
		SolarNodeOwnership ownership = ownershipFor(-1L, TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(ownership.getNodeId());
		Filter result = service.userNodeAccessCheck(criteria);
		Assert.assertSame(criteria, result);
	}

	@Test
	public void datumFilterPrivateNodeAsAuthenticatedNode() {
		AuthenticatedNode node = setAuthenticatedNode(-1L);
		SolarNodeOwnership ownership = privateOwnershipFor(node.getNodeId(), TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(node.getNodeId());
		Filter result = service.userNodeAccessCheck(criteria);
		Assert.assertSame(criteria, result);
	}

	@Test
	public void datumFilterPrivateNodeAsAnonymous() {
		SolarNodeOwnership ownership = privateOwnershipFor(-1L, TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(ownership.getNodeId());
		try {
			service.userNodeAccessCheck(criteria);
			Assert.fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumFilterPrivateNodeAsSomeOtherNode() {
		setAuthenticatedNode(-2L);
		SolarNodeOwnership ownership = privateOwnershipFor(-1L, TEST_USER_ID);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(ownership.getNodeId());
		try {
			service.userNodeAccessCheck(criteria);
			Assert.fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumFilterPrivateNodeAsUserToken() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(nodeId)).build();
		setAuthenticatedUserToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		GeneralNodeDatumFilter result = service.userNodeAccessCheck(criteria);
		Assert.assertEquals(nodeId, result.getNodeId());
	}

	@Test
	public void datumFilterPrivateNodeAsSomeOtherUserToken() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(nodeId)).build();
		setAuthenticatedUserToken(-200L, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		try {
			service.userNodeAccessCheck(criteria);
			Assert.fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataToken() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(nodeId)).build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		GeneralNodeDatumFilter result = service.userNodeAccessCheck(criteria);
		Assert.assertEquals(nodeId, result.getNodeId());
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataTokenWithoutNodeRestriction() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		GeneralNodeDatumFilter result = service.userNodeAccessCheck(criteria);
		Assert.assertEquals(nodeId, result.getNodeId());
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataTokenSomeOtherUser() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(nodeId)).build();
		// note the actor is not the owner of the node
		setAuthenticatedReadNodeDataToken(-200L, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		GeneralNodeDatumFilter result = service.userNodeAccessCheck(criteria);
		Assert.assertEquals(nodeId, result.getNodeId());
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataTokenSomeOtherUserNonMatchingNode() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(-2L)).build();
		// note the actor is not the owner of the node, and the token is not granted access to the node ID
		setAuthenticatedReadNodeDataToken(-200L, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		try {
			service.userNodeAccessCheck(criteria);
			Assert.fail("Should have thrown SecurityException for anonymous user");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataTokenWithoutNodeRestrictionNonMatchingNode() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		// note the actor is not the owner of the node, and the token is not granted access to the node ID
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, -200L);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setType("Consumption");
		criteria.setNodeId(nodeId);
		try {
			service.userNodeAccessCheck(criteria);
			Assert.fail("Should have thrown SecurityException for node ID not owned by owner of token");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumFilterPrivateNodeAsReadNodeDataTokenWithFilledInSourceIdsPolicy() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "One", "Two" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(Collections.singleton(nodeId)).build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(nodeId);
		GeneralNodeDatumFilter result = service.userNodeAccessCheck(criteria);
		Assert.assertEquals(nodeId, result.getNodeId());
		Assert.assertArrayEquals("Filled in source IDs", policySourceIds, result.getSourceIds());
	}

	@Test
	public void availableSourceIdsFilteredFromPattern() throws Throwable {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "/A/**/watts" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(Collections.singleton(nodeId)).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		final Set<String> availableSourceIds = new LinkedHashSet<String>(
				Arrays.asList("/A/B/watts", "/A/C/watts", "/B/B/watts", "Foo bar"));
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		EasyMock.expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		EasyMock.expect(pjp.proceed()).andReturn(availableSourceIds);

		EasyMock.replay(pjp);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(nodeId);
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) service.reportableSourcesAccessCheck(pjp, nodeId);
		Assert.assertEquals("Filtered source IDs",
				new LinkedHashSet<String>(Arrays.asList("/A/B/watts", "/A/C/watts")), result);
	}

	@Test
	public void reportableSourceIdsFilter() throws Throwable {
		// GIVEN
		final Long nodeId = -1L;
		final Long nodeId2 = -2L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "/A/**/watts" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(new HashSet<Long>(Arrays.asList(nodeId, nodeId2))).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		final Set<String> availableSourceIds = new LinkedHashSet<String>(
				Arrays.asList("/A/B/watts", "/A/C/watts", "/B/B/watts", "Foo bar"));
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		SolarNodeOwnership ownership2 = privateOwnershipFor(nodeId2, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId2)).andReturn(ownership2);

		expect(pjp.proceed()).andReturn(availableSourceIds);

		// WHEN
		replay(pjp);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { nodeId, nodeId2 });
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) service.reportableSourcesFilterAccessCheck(pjp, criteria);

		// THEN
		then(result).as("Source IDs filtered by policy").contains("/A/B/watts", "/A/C/watts");
	}

	@Test
	public void reportableSourceIdsFilter_noNodeIds() throws Throwable {
		// GIVEN
		final ProceedingJoinPoint pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		final Set<String> availableSourceIds = new LinkedHashSet<String>(
				Arrays.asList("/A/B/watts", "/A/C/watts", "/B/B/watts", "Foo bar"));
		expect(pjp.proceed()).andReturn(availableSourceIds);

		// WHEN
		replay(pjp);
		replayAll();

		DatumFilterCommand filter = new DatumFilterCommand();
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) service.reportableSourcesFilterAccessCheck(pjp, filter);

		// THEN
		then(result).as("All source IDs returned").isSameAs(availableSourceIds);
	}

	@Test
	public void availableSourceIdsFilter() throws Throwable {
		// GIVEN
		final Long nodeId = -1L;
		final Long nodeId2 = -2L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "/A/**/watts" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(new HashSet<Long>(Arrays.asList(nodeId, nodeId2))).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		final Set<NodeSourcePK> availableSourceIds = new LinkedHashSet<NodeSourcePK>(Arrays.asList(
				new NodeSourcePK(nodeId, "/A/B/watts"), new NodeSourcePK(nodeId, "/A/C/watts"),
				new NodeSourcePK(nodeId, "/B/B/watts"), new NodeSourcePK(nodeId2, "/A/B/watts")));
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		SolarNodeOwnership ownership2 = privateOwnershipFor(nodeId2, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId2)).andReturn(ownership2);

		expect(pjp.proceed()).andReturn(availableSourceIds);

		// WHEN
		replay(pjp);
		replayAll();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { nodeId, nodeId2 });
		@SuppressWarnings("unchecked")
		Set<NodeSourcePK> result = (Set<NodeSourcePK>) service.availableSourcesFilterAccessCheck(pjp,
				criteria);

		// THEN
		then(result).as("Filtered source IDs to policy").contains(new NodeSourcePK(nodeId, "/A/B/watts"),
				new NodeSourcePK(nodeId, "/A/C/watts"), new NodeSourcePK(nodeId2, "/A/B/watts"));
	}

	@Test
	public void availableSourceIdsFilter_noNodeIds() throws Throwable {
		// GIVEN
		final Long nodeId = -1L;
		final Long nodeId2 = -2L;
		final ProceedingJoinPoint pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		final Set<NodeSourcePK> availableSourceIds = new LinkedHashSet<NodeSourcePK>(Arrays.asList(
				new NodeSourcePK(nodeId, "/A/B/watts"), new NodeSourcePK(nodeId, "/A/C/watts"),
				new NodeSourcePK(nodeId, "/B/B/watts"), new NodeSourcePK(nodeId2, "/A/B/watts")));
		expect(pjp.proceed()).andReturn(availableSourceIds);

		// WHEN
		replay(pjp);
		replayAll();

		DatumFilterCommand filter = new DatumFilterCommand();
		@SuppressWarnings("unchecked")
		Set<NodeSourcePK> result = (Set<NodeSourcePK>) service.availableSourcesFilterAccessCheck(pjp,
				filter);

		// THEN
		then(result).as("All source IDs returned").isSameAs(availableSourceIds);
	}

	@Test
	public void weatherFilterAsAnonymous() {
		replayAll();

		SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("Pacific/Auckland");
		DatumFilterCommand criteria = new DatumFilterCommand(loc);
		criteria.setType("Weather");
		Filter result = service.userNodeAccessCheck(criteria);
		Assert.assertSame(criteria, result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findFilteredGeneralNodeDatumRedirectMinAggregateEnforement() throws Throwable {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final Aggregation policyMinAgg = Aggregation.Day;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(nodeId)).withMinAggregation(policyMinAgg).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		setAuthenticatedReadNodeDataToken(userId, policy);

		final DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(nodeId);

		final QueryBiz queryBiz = EasyMock.createMock(QueryBiz.class);
		final Signature methodSig = EasyMock.createMock(Signature.class);

		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		// setup join point conditions to mimic call to findFilteredGeneralNodeDatum()
		expect(pjp.getTarget()).andReturn(queryBiz).anyTimes();
		expect(pjp.getSignature()).andReturn(methodSig).anyTimes();
		expect(methodSig.getName()).andReturn("findFilteredGeneralNodeDatum").anyTimes();
		expect(pjp.getArgs()).andReturn(new Object[] { criteria, null, null, null }).anyTimes();

		// findFilteredGeneralNodeDatum should be redirected to findFilteredAggregateGeneralNodeDatum()
		final Capture<AggregateGeneralNodeDatumFilter> filterCapture = new Capture<AggregateGeneralNodeDatumFilter>();
		final FilterResults<ReportingGeneralNodeDatumMatch> filterResults = new BasicFilterResults<ReportingGeneralNodeDatumMatch>(
				Collections.<ReportingGeneralNodeDatumMatch> emptyList(), Long.valueOf(0L),
				Integer.valueOf(0), Integer.valueOf(0));
		expect(queryBiz.findFilteredAggregateGeneralNodeDatum(EasyMock.capture(filterCapture),
				EasyMock.isNull(List.class), EasyMock.isNull(Integer.class),
				EasyMock.isNull(Integer.class))).andReturn(filterResults);

		// WHEN
		replayAll(pjp, methodSig, queryBiz);

		Object result = service.userNodeFilterAccessCheck(pjp, criteria);
		assertSame("Filtered results", filterResults, result);
		AggregateGeneralNodeDatumFilter redirectedFilter = filterCapture.getValue();
		Assert.assertEquals("Redirected filter node ID", nodeId, redirectedFilter.getNodeId());
		Assert.assertEquals("Redirected filter aggregation", policyMinAgg,
				redirectedFilter.getAggregation());

		// THEN
		verify(pjp, methodSig, queryBiz);
	}

	@Test
	public void injectAvailableSourceIdsWhenNoneProvidedButPolicyRestricts() throws Throwable {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "/A/**/watts" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(Collections.singleton(nodeId)).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		final QueryBiz queryBiz = EasyMock.createMock(QueryBiz.class);
		final Signature methodSig = EasyMock.createMock(Signature.class);
		final Set<String> availableSourceIds = new LinkedHashSet<String>(
				Arrays.asList("/A/B/watts", "/A/C/watts", "/B/B/watts", "Foo bar"));
		setAuthenticatedReadNodeDataToken(userId, policy);

		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		final DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(nodeId);
		criteria.setMostRecent(true);

		// setup join point conditions to handle call to findFilteredGeneralNodeDatum()
		expect(pjp.getTarget()).andReturn(queryBiz).anyTimes();
		expect(pjp.getSignature()).andReturn(methodSig).anyTimes();
		expect(methodSig.getName()).andReturn("findFilteredGeneralNodeDatum").anyTimes();
		expect(pjp.getArgs()).andReturn(new Object[] { criteria, null, null, null }).anyTimes();

		// aspect should call QueryBiz.getAvailableSources(nodeId, start, end)
		Capture<DatumFilterCommand> filterCapture = new Capture<DatumFilterCommand>();
		expect(queryBiz.getAvailableSources(EasyMock.capture(filterCapture)))
				.andReturn(availableSourceIds);

		// join point should proceed with custom arguments list
		final Capture<Object[]> proceedArgsCapture = new Capture<Object[]>();
		final FilterResults<GeneralNodeDatumFilterMatch> filterResults = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				Collections.<GeneralNodeDatumFilterMatch> emptyList(), Long.valueOf(0L),
				Integer.valueOf(0), Integer.valueOf(0));
		expect(pjp.proceed(EasyMock.capture(proceedArgsCapture))).andReturn(filterResults);

		// WHEN
		replayAll(pjp, methodSig, queryBiz);

		Object result = service.userNodeFilterAccessCheck(pjp, criteria);
		assertSame("Filtered results", filterResults, result);
		Object[] findFilteredArgs = proceedArgsCapture.getValue();
		Assert.assertNotNull(findFilteredArgs);
		Assert.assertEquals("findFilteredGeneralNodeDatum argument length", 4, findFilteredArgs.length);
		Assert.assertNotSame("findFilteredGeneralNodeDatum filter argument changed", criteria,
				findFilteredArgs[0]);
		Assert.assertTrue("findFilteredGeneralNodeDatum filter",
				findFilteredArgs[0] instanceof GeneralNodeDatumFilter);
		GeneralNodeDatumFilter injectedFilter = (GeneralNodeDatumFilter) findFilteredArgs[0];
		Assert.assertArrayEquals("Filtered source IDs", new String[] { "/A/B/watts", "/A/C/watts" },
				injectedFilter.getSourceIds());

		// verify captured source ID filter
		assertThat("Source ID filter node IDs", filterCapture.getValue().getNodeIds(),
				arrayContaining(nodeId));
		assertThat("Source ID filter start date", filterCapture.getValue().getStartDate(), nullValue());
		assertThat("Source ID filter end date", filterCapture.getValue().getEndDate(), nullValue());

		// THEN
		verify(pjp, methodSig, queryBiz);
	}

	@Test
	public void streamDatumFilter_privateNodeAsAnonymous() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		SolarNodeOwnership ownership = privateOwnershipFor(-1L, TEST_USER_ID);

		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId, new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, -1L, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		try {
			service.userNodeAccessCheck(criteria);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertThat("Denied", e.getReason(), is(Reason.ACCESS_DENIED));
		}
	}

	@Test
	public void streamDatumFilter_privateNode_readNodeDataToken_noNodePolicy_allowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		service.userNodeAccessCheck(criteria);
	}

	@Test
	public void streamDatumFilter_privateNode_readNodeDataToken_noNodePolicy_notAllowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		setAuthenticatedReadNodeDataToken(-101L, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		try {
			service.userNodeAccessCheck(criteria);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertThat("Denied becauase not node owner", e.getReason(), is(Reason.ACCESS_DENIED));
		}
	}

	@Test
	public void streamDatumFilter_privateNode_readNodeDataToken_nodePolicy_allowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(singleton(nodeId))
				.build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		service.userNodeAccessCheck(criteria);
	}

	@Test
	public void streamDatumFilter_privateNode_readNodeDataToken_nodePolicy_notAllowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(singleton(-2L))
				.build();
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		try {
			service.userNodeAccessCheck(criteria);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertThat("Denied because of node ID policy", e.getReason(), is(Reason.ACCESS_DENIED));
		}
	}

	@Test
	public void streamDatumFilter_privateNode_userToken_noNodePolicy_allowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		setAuthenticatedUserToken(userId, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		service.userNodeAccessCheck(criteria);
	}

	@Test
	public void streamDatumFilter_privateNode_userToken_noNodePolicy_notAllowed() {
		// GIVEN
		final Long nodeId = -1L;
		final Long userId = -100L;
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
		setAuthenticatedUserToken(-101L, policy);
		SolarNodeOwnership ownership = privateOwnershipFor(nodeId, userId);

		UUID streamId = UUID.randomUUID();
		Map<UUID, ObjectDatumStreamMetadataId> idMap = new LinkedHashMap<>();
		idMap.put(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, "foo"));
		expect(streamMetadataDao.getDatumStreamMetadataIds(streamId)).andReturn(idMap);

		expect(nodeOwnershipDao.ownershipForNodeId(ownership.getNodeId())).andReturn(ownership);

		// WHEN
		replayAll();

		StreamDatumFilterCommand criteria = new StreamDatumFilterCommand();
		criteria.setStreamIds(new UUID[] { streamId });
		try {
			service.userNodeAccessCheck(criteria);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertThat("Denied because not node owner", e.getReason(), is(Reason.ACCESS_DENIED));
		}
	}

}
