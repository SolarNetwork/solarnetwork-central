/* ==================================================================
 * Dnp3ProxyConfigurationProviderTests.java - 14/08/2023 11:01:03 am
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

package net.solarnetwork.central.dnp3.app.server.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.Outstation;
import net.solarnetwork.central.biz.NodeEventObservationRegistrar;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.dnp3.app.service.Dnp3ProxyConfigurationProvider;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.domain.SimpleProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Test cases for the {@link Dnp3ProxyConfigurationProvider} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class Dnp3ProxyConfigurationProviderTests {

	@Mock
	private DNP3Manager manager;

	@Mock
	private InstructorBiz instructorBiz;

	@Mock
	private DynamicPortRegistrar portRegistrar;

	@Mock
	private TrustedIssuerCertificateDao trustedCertDao;

	@Mock
	private ServerAuthConfigurationDao serverAuthDao;

	@Mock
	private ServerMeasurementConfigurationDao serverMeasurementDao;

	@Mock
	private ServerControlConfigurationDao serverControlDao;

	@Mock
	private NodeEventObservationRegistrar<ObjectDatum> datumObserver;

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private Channel channel;

	@Mock
	private Outstation outstation;

	@Captor
	private ArgumentCaptor<CertificateFilter> certificateFilterCaptor;

	private static final String TEST_CA_DN = "CN=Test CA, O=Solar Test CA";
	private static final String TEST_DN = "CN=Test Client, O=Test Org";

	private KeyPairGenerator keyGen;
	private BCCertificateService certService;
	private KeyPair caKey;
	private X509Certificate caCert;
	private KeyPair clientKey;
	private X509Certificate clientCert;

	private Dnp3ProxyConfigurationProvider service;

	@BeforeEach
	public void setup() throws Exception {
		keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, SecureRandom.getInstanceStrong());
		caKey = keyGen.generateKeyPair();
		certService = new BCCertificateService();
		caCert = certService.generateCertificationAuthorityCertificate(TEST_CA_DN, caKey.getPublic(),
				caKey.getPrivate());

		clientKey = keyGen.generateKeyPair();
		X509Certificate clientSelfCert = certService.generateCertificate(TEST_DN, clientKey.getPublic(),
				clientKey.getPrivate());
		String clientCsr = certService.generatePKCS10CertificateRequestString(clientSelfCert,
				clientKey.getPrivate());
		clientCert = certService.signCertificate(clientCsr, caCert, caKey.getPrivate());

		service = new Dnp3ProxyConfigurationProvider(manager, instructorBiz, portRegistrar,
				trustedCertDao, serverAuthDao, serverMeasurementDao, serverControlDao, datumObserver,
				datumDao, userEventAppenderBiz);
	}

	@Test
	public void authorize() throws Exception {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final Long serverId = UUID.randomUUID().getMostSignificantBits();
		final String clientSubjectDn = CertificateUtils.canonicalSubjectDn(clientCert);

		// check auth
		final ServerAuthConfiguration auth = new ServerAuthConfiguration(userId, serverId,
				clientSubjectDn, now());
		auth.setEnabled(true);
		given(serverAuthDao.findForIdentifier(clientSubjectDn)).willReturn(auth);

		// load trust store
		final TrustedIssuerCertificate issuer = new TrustedIssuerCertificate(userId, caCert, now());
		given(trustedCertDao.findFiltered(any())).willReturn(new BasicFilterResults<>(asList(issuer)));

		// load measurements
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();

		final ServerMeasurementConfiguration meas = new ServerMeasurementConfiguration(userId, serverId,
				0, now());
		meas.setNodeId(nodeId);
		meas.setSourceId("meter/1");
		meas.setProperty("watts");
		meas.setType(MeasurementType.AnalogInput);
		meas.setEnabled(true);
		given(serverMeasurementDao.findFiltered(any()))
				.willReturn(new BasicFilterResults<>(asList(meas)));

		// load controls
		final ServerControlConfiguration ctrl = new ServerControlConfiguration(userId, serverId, 0,
				now());
		ctrl.setNodeId(nodeId);
		ctrl.setSourceId("switch/1");
		ctrl.setType(ControlType.Binary);
		ctrl.setEnabled(true);
		given(serverControlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(asList(ctrl)));

		// allocate port
		final int port = new SecureRandom().nextInt(50000, 60000);
		given(portRegistrar.reserveNewPort()).willReturn(port);

		// create channel
		given(manager.addTCPServer(any(), anyInt(), any(), any(), eq(port), any())).willReturn(channel);

		// create Outstation
		given(channel.addOutstation(any(), any(), any(), any())).willReturn(outstation);

		// WHEN
		SimpleProxyConnectionRequest req = new SimpleProxyConnectionRequest(
				canonicalSubjectDn(clientCert), new X509Certificate[] { clientCert, caCert });
		ProxyConnectionSettings result = service.authorize(req);
		if ( result instanceof ServiceLifecycleObserver obs ) {
			obs.serviceDidStartup();
		}

		// THEN
		verify(trustedCertDao).findFiltered(certificateFilterCaptor.capture());

		// @formatter:off
		then(certificateFilterCaptor.getValue())
			.as("Trusted cert filter included user ID")
			.returns(userId, CertificateFilter::getUserId)
			.as("Trusted cert filter included enabled flag")
			.returns(true, CertificateFilter::getEnabled)
			;
		
		then(result)
			.as("Settings created")
			.isNotNull()
			.as("Result supports lifecycle API")
			.isInstanceOf(ServiceLifecycleObserver.class)
			.as("Port from registrar")
			.returns(port, ProxyConnectionSettings::destinationPort)
			;
		
		then(result.connectionRequest())
			.as("Connection request provided")
			.isSameAs(req)
			;
		
		verify(outstation).enable();
		// @formatter:on
	}

}
