/* ==================================================================
 * ManagedJavaMailSender.java - 6/05/2019 9:55:06 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.mail.javamail;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import javax.activation.FileTypeMap;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import net.solarnetwork.util.ClassUtils;

/**
 * {@link JavaMailSender} to expose hooks for managing the settings of a
 * {@link JavaMailSenderImpl} dynamically.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class ManagedJavaMailSender implements JavaMailSender {

	private JavaMailSenderImpl delegate = new JavaMailSenderImpl();

	/**
	 * Callback after properties have been changed.
	 * 
	 * @param properties
	 *        the changed properties
	 */
	public synchronized void configurationChanged(Map<String, Object> properties) {
		// JavaMailSenderImpl does not expose a way for us to re-create the mail Session, so we
		// re-create a whole new JavaMailSenderImpl from scratch here
		Map<String, Object> settings = ClassUtils.getSimpleBeanProperties(delegate, null);
		Properties props = delegate.getJavaMailProperties();
		JavaMailSenderImpl updated = new JavaMailSenderImpl();
		ClassUtils.setBeanProperties(updated, settings, true);
		updated.setJavaMailProperties(props);
		this.delegate = updated;
	}

	/**
	 * Configuration hook for backwards compatibility.
	 * 
	 * <p>
	 * This method exists for backwards compatability, so the
	 * <code>mail.host</code> setting can be correctly resolved to setting the
	 * {@link #setHost(String)} property.
	 * </p>
	 * 
	 * @return this
	 */
	public ManagedJavaMailSender getMail() {
		return this;
	}

	/* JavaMailSenderImpl delegate */

	public Properties getJavaMailProperties() {
		return delegate.getJavaMailProperties();
	}

	public void setJavaMailProperties(Properties javaMailProperties) {
		delegate.setJavaMailProperties(javaMailProperties);
	}

	public void setProtocol(String protocol) {
		delegate.setProtocol(protocol);
	}

	public void setHost(String host) {
		delegate.setHost(host);
	}

	public void setPort(int port) {
		delegate.setPort(port);
	}

	public void setUsername(String username) {
		delegate.setUsername(username);
	}

	public void setPassword(String password) {
		delegate.setPassword(password);
	}

	public void setDefaultEncoding(String defaultEncoding) {
		delegate.setDefaultEncoding(defaultEncoding);
	}

	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		delegate.setDefaultFileTypeMap(defaultFileTypeMap);
	}

	/* JavaMailSender API */

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		delegate.send(simpleMessage);
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		delegate.send(simpleMessages);
	}

	@Override
	public MimeMessage createMimeMessage() {
		return delegate.createMimeMessage();
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		return delegate.createMimeMessage(contentStream);
	}

	private void doWithSender(Consumer<JavaMailSender> handler) {
		ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(javax.mail.Message.class.getClassLoader());
		try {
			handler.accept(delegate);
		} finally {
			if ( oldCCL != null ) {
				Thread.currentThread().setContextClassLoader(oldCCL);
			}
		}
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		doWithSender(new Consumer<JavaMailSender>() {

			@Override
			public void accept(JavaMailSender sender) {
				sender.send(mimeMessage);
			}
		});
	}

	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		doWithSender(new Consumer<JavaMailSender>() {

			@Override
			public void accept(JavaMailSender sender) {
				sender.send(mimeMessages);
			}
		});
	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		doWithSender(new Consumer<JavaMailSender>() {

			@Override
			public void accept(JavaMailSender sender) {
				sender.send(mimeMessagePreparator);
			}
		});
	}

	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		doWithSender(new Consumer<JavaMailSender>() {

			@Override
			public void accept(JavaMailSender sender) {
				sender.send(mimeMessagePreparators);
			}
		});
	}

}
