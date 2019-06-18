package com.linkedin.venice.router;

import com.linkedin.venice.router.api.VenicePathParser;
import com.linkedin.venice.router.api.VenicePathParserHelper;
import com.linkedin.venice.router.stats.HealthCheckStats;
import com.linkedin.venice.utils.RedundantExceptionFilter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import org.apache.log4j.Logger;

import static com.linkedin.venice.router.api.VenicePathParser.*;
import static com.linkedin.venice.utils.NettyUtils.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;


@ChannelHandler.Sharable
public class HealthCheckHandler extends SimpleChannelInboundHandler<HttpRequest> {
  private static final Logger logger = Logger.getLogger(HealthCheckHandler.class);
  private static final AsciiString ALLOWED_METHODS = AsciiString.of("GET,POST,OPTIONS");
  private static final byte[] EMPTY_BYTES = new byte[0];
  private static RedundantExceptionFilter filter = RedundantExceptionFilter.getRedundantExceptionFilter();

  private final HealthCheckStats healthCheckStats;

  public HealthCheckHandler(HealthCheckStats healthCheckStats) {
    this.healthCheckStats = healthCheckStats;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
    // Check whether it's a health check request
    boolean isHealthCheck = false;
    if (msg.method().equals(HttpMethod.OPTIONS)) {
      isHealthCheck = true;
    } else if (msg.method().equals(HttpMethod.GET)) {
      VenicePathParserHelper helper = new VenicePathParserHelper(msg.uri());
      if (TYPE_HEALTH_CHECK.equals(helper.getResourceType())) {
        isHealthCheck = true;
      }
    }

    if (isHealthCheck) {
      healthCheckStats.recordHealthCheck();
      setupResponseAndFlush(OK, EMPTY_BYTES, false, ctx);
    } else {
      // Pass request to the next channel if it's not a health check
      ReferenceCountUtil.retain(msg);
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
    healthCheckStats.recordErrorHealthCheck();
    InetSocketAddress sockAddr = (InetSocketAddress)(ctx.channel().remoteAddress());
    String remoteAddr = sockAddr.getHostName() + ":" + sockAddr.getPort();
    if (!filter.isRedundantException(sockAddr.getHostName(), e)) {
      logger.error("Got exception while handling health check request from " + remoteAddr + ": ", e);
    }
  }
}