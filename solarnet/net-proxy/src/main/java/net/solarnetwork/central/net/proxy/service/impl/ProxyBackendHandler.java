/* ==================================================================
 * ProxyBackendHandler.java - 4/08/2023 10:03:07 am
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Proxy backend handler.
 * 
 * @author matt
 * @version 1.0
 */
public class ProxyBackendHandler extends ChannelInboundHandlerAdapter {

	private final Channel inboundChannel;

	public ProxyBackendHandler(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		if ( !inboundChannel.isActive() ) {
			ProxyFrontendHandler.closeOnFlush(ctx.channel());
		} else {
			ctx.read();
		}
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) {
				if ( future.isSuccess() ) {
					ctx.channel().read();
				} else {
					future.channel().close();
				}
			}
		});
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		ProxyFrontendHandler.closeOnFlush(inboundChannel);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ProxyFrontendHandler.closeOnFlush(ctx.channel());
	}
}
