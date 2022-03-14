/* ==================================================================
 * PkiDevConfig.java - 7/10/2021 12:46:45 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.central.user.pki.dev.DevNodePKIBiz;
import net.solarnetwork.service.CertificateService;
import net.solarnetwork.service.CertificationAuthorityService;

/**
 * Development PKI configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("!dogtag")
public class PkiDevConfig {

	@Value("${app.node.pki.dev.base-dir:var/DeveloperCA}")
	public Path baseDir = Paths.get("var/DeveloperCA");

	@Value("${app.node.pki.dev.ca-dn:CN=Developer CA, O=SolarDev}")
	public String caDn = "CN=Developer CA, O=SolarDev";

	@Autowired
	private CertificateService certificateService;

	@Autowired
	CertificationAuthorityService certificationAuthorityService;

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public NodePKIBiz pkiBiz() {
		DevNodePKIBiz biz = new DevNodePKIBiz();
		biz.setCaService(certificationAuthorityService);
		biz.setCertificateService(certificateService);
		biz.setBaseDir(baseDir.toFile());
		biz.setCaDN(caDn);
		return biz;
	}

}
