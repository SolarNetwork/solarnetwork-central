/* ==================================================================
 * DaoUserInstructionInputBizTests.java - 24/09/2026 11:04:37 am
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

package net.solarnetwork.central.user.inin.biz.impl.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.biz.TransformConstants;
import net.solarnetwork.central.inin.dao.CredentialConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.user.inin.biz.impl.DaoUserInstructionInputBiz;
import net.solarnetwork.central.user.inin.domain.TransformOutput;

/**
 * Test cases for the {@link DaoUserInstructionInputBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserInstructionInputBizTests {

	@Mock
	private CredentialConfigurationDao credentialDao;

	@Mock
	private TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;

	@Mock
	private TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;

	@Mock
	private EndpointConfigurationDao endpointDao;

	@Mock
	private EndpointAuthConfigurationDao endpointAuthDao;

	@Mock
	private RequestTransformService requestTransformService;

	@Mock
	private ResponseTransformService responseTransformService;

	private DaoUserInstructionInputBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserInstructionInputBiz(credentialDao, requestTransformDao, responseTransformDao,
				endpointDao, endpointAuthDao, Set.of(requestTransformService),
				Set.of(responseTransformService));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void previewTransform_cacheKeysIncludeModificationDate() throws Exception {
		// GIVEN
		final Long userId = randomLong();
		final UUID endpointId = randomUUID();
		final UserUuidPK endpointPk = new UserUuidPK(userId, endpointId);
		final Long reqTransformId = randomLong();
		final Long resTransformId = randomLong();
		final UserLongCompositePK reqXformPk = new UserLongCompositePK(userId, reqTransformId);
		final UserLongCompositePK resXformPk = new UserLongCompositePK(userId, resTransformId);
		final String reqServiceId = randomString();
		final String resServiceId = randomString();

		final EndpointConfiguration endpoint = new EndpointConfiguration(endpointPk, now(),
				randomString());
		endpoint.setRequestTransformId(reqTransformId);
		endpoint.setResponseTransformId(resTransformId);

		final var reqXform = new RequestTransformConfiguration(reqXformPk, now(), randomString(),
				reqServiceId);
		reqXform.setModified(now().truncatedTo(ChronoUnit.SECONDS));

		final var resXform = new ResponseTransformConfiguration(resXformPk, now(), randomString(),
				resServiceId);
		resXform.setModified(now().truncatedTo(ChronoUnit.SECONDS));

		given(endpointDao.get(endpointPk)).willReturn(endpoint);
		given(requestTransformDao.get(reqXformPk)).willReturn(reqXform);
		given(responseTransformDao.get(resXformPk)).willReturn(resXform);

		// the biz reuses and mutates one parameter map for both transforms, so snapshot
		// what each service actually receives at the time it is called
		final Map<String, Object> reqParams = new HashMap<>(8);
		final Map<String, Object> resParams = new HashMap<>(8);

		given(requestTransformService.getId()).willReturn(reqServiceId);
		given(requestTransformService.supportsInput(any(), any())).willReturn(true);
		given(requestTransformService.transformInput(any(), any(), any(), any())).willAnswer(inv -> {
			reqParams.putAll(inv.getArgument(3, Map.class));
			return List.of();
		});

		given(responseTransformService.getId()).willReturn(resServiceId);
		given(responseTransformService.supportsOutputType(any())).willReturn(true);
		willAnswer(inv -> {
			resParams.putAll(inv.getArgument(3, Map.class));
			return null;
		}).given(responseTransformService).transformOutput(any(), any(), any(), any(), any());

		final MimeType contentType = MimeType.valueOf("application/json");
		final MimeType outputType = MimeType.valueOf("application/json");
		final InputStream in = new ByteArrayInputStream("{}".getBytes(UTF_8));

		// WHEN
		TransformOutput result = biz.previewTransform(endpointPk, contentType, in, outputType, null,
				null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			;

		and.then(reqParams)
			.as("Request cache key is the entity ident, which includes the modification date, so "
					+ "that editing the XSLT does not serve a stale cached stylesheet")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, reqXform.ident())
			.as("Request entity ident differs from the primary key ident")
			.doesNotContainEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, reqXformPk.ident())
			;

		and.then(resParams)
			.as("Response transform receives the assembled parameters, including its own cache key")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, resXform.ident())
			.as("Response entity ident differs from the primary key ident")
			.doesNotContainEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, resXformPk.ident())
			.as("Response transform ID provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, resTransformId)
			;
		// @formatter:on
	}

}
