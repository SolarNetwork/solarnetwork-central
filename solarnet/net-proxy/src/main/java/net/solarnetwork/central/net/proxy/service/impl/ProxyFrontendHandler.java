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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import net.solarnetwork.central.net.proxy.domain.ProxyConfiguration;
import net.solarnetwork.util.ObjectUtils;

/**
 * Proxy frontend handler.
 * 
 * @author matt
 * @version 1.0
 */
public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

	private final ProxyConfiguration config;

	private Channel outboundChannel;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ProxyFrontendHandler(ProxyConfiguration config) {
		super();
		this.config = ObjectUtils.requireNonNullArgument(config, "config");
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		final Channel inboundChannel = ctx.channel();

		// connect to destination server
		Bootstrap b = new Bootstrap();
		// @formatter:off
		b.group(inboundChannel.eventLoop())
			.channel(ctx.channel().getClass())
			.handler(new ProxyBackendHandler(inboundChannel))
			.option(ChannelOption.AUTO_READ, false);
		// @formatter:off
		ChannelFuture f = b.connect(config.destinationHost(), config.destinationPort());
		outboundChannel = f.channel();
		f.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) {
				if ( future.isSuccess() ) {
					// connection complete start to read first data
					inboundChannel.read();
				} else {
					// Close the connection if the connection attempt has failed.
					inboundChannel.close();
				}
			}
		});
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		if ( outboundChannel.isActive() ) {
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
		cause.printStackTrace();
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
