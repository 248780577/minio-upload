package org.freesoul.minio.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @author: wjg
 * @create: 2024-09-26 11:14
 * @description: websocket配置
 **/

@Configuration
public class WebSocketConfig {
    /**
     * 自动注册使用了@ServerEndpoint注解声明的Websocket endpoint
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 通信文本消息和二进制缓存区大小
     * 避免对接 第三方 报文过大时，Websocket 1009 错误
     *
     * @return
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 在此处设置bufferSize
        container.setMaxTextMessageBufferSize(10240000);
        container.setMaxBinaryMessageBufferSize(10240000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }
}
