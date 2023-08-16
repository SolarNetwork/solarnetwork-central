/* ==================================================================
 * ProxyFrontendHandler.java - 4/08/2023 9:58:56 am
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

package net.solarnetwork.central.net.proxy.service.impl;

import static net.solarnetwork.central.net.proxy.service.impl.NettyDynamicProxyServer.SSL_SESSION_PROXY_SETTINGS_KEY;
import java.io.IOException;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Proxy frontend handler.
 * 
 * <p>
 * This handler expects a {@link SslHandler} to be available in the channel
 * context's pipeline, and for that handler's engine's {@code SSLSession} to
 * have a {@link ProxyConnectionSettings} instance available on the
 * {@link NettyDynamicProxyServer#SSL_SESSION_PROXY_SETTINGS_KEY
 * SSL_SESSION_PROXY_SETTINGS_KEY} key. If the settings instance also implements
 * {@link ServiceLifecycleObserver}, then
 * {@link ServiceLifecycleObserver#serviceDidStartup() serviceDidStartup()} will
 * be called <b>before</b> the destination server connection is attempted. This
 * gives the settings a change to dynamically allocate a server. The
 * {@link ServiceLifecycleObserver#serviceDidShutdown() serviceDidShutdown()}
 * method will be called <b>after</b> the connection is closed.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(ProxyFrontendHandler.class);

	private Bootstrap b;
	private Channel outboundChannel;

	/**
	 * Constructor.
	 */
	public ProxyFrontendHandler() {
		super();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if ( evt instanceof SslHandshakeCompletionEvent hs && hs.isSuccess() ) {
			// get proxy settings from SSL session
			final Channel inboundChannel = ctx.channel();
			final SslHandler ssl = inboundChannel.pipeline().get(SslHandler.class);
			ProxyConnectionSettings settings = (ssl != null
					? (ProxyConnectionSettings) ssl.engine().getSession()
							.getValue(SSL_SESSION_PROXY_SETTINGS_KEY)
					: null);
			if ( settings == null ) {
				// TODO: freak out
				return;
			}

			if ( settings instanceof ServiceLifecycleObserver obs ) {
				obs.serviceDidStartup();
			}
			ChannelFuture f = b.connect(settings.destinationHost(), settings.destinationPort());
			outboundChannel = f.channel();
			f.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) {
					if ( !future.isSuccess() ) {
						// Close the connection if the destination connection attempt failed
						inboundChannel.close();
					}
				}
			});
			if ( settings instanceof ServiceLifecycleObserver obs ) {
				outboundChannel.closeFuture().addListener((close) -> {
					obs.serviceDidShutdown();
				});
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		final Channel inboundChannel = ctx.channel();

		b = new Bootstrap();
		// @formatter:off
		b.group(inboundChannel.eventLoop())
			.channel(ctx.channel().getClass())
			.handler(new ProxyBackendHandler(inboundChannel))
			.option(ChannelOption.AUTO_READ, false);
		// @formatter:off
		
		inboundChannel.read(); // to start TLS handshake
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		if ( outboundChannel != null && outboundChannel.isActive() ) {
			outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) {
					if ( future.isSuccess() ) {
						// was able to flush out data, start to read the next chunk
						ctx.channel().read();
					} else {
						future.channel().close();
					}
				}
			});
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if ( outboundChannel != null ) {
			closeOnFlush(outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		Throwable root = cause;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		if (root instanceof io.netty.handler.ssl.NotSslRecordException e ) {
			String msg = e.getMessage();
			final int max = 64;
			if ( msg.length() > max ) {
				msg = msg.substring(0, max) + "... and " +(msg.length() - max) +" more";
			}
			log.debug("Non-TLS client message; dropping connection: {}", msg);
		} else if ( root instanceof SSLException || root.getClass().getName().startsWith("javax.crypto.") ) {
			log.debug("TLS client error; dropping connection: {}", root.toString());
		} else if ( root instanceof IOException e) {
			log.debug("Client IO error; dropping connection: {}", e.toString());
		} else {
			log.error("Unexpected client connection exception: {}", root.getMessage(), root);
		}
		closeOnFlush(ctx.channel());
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if ( ch.isActive() ) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
