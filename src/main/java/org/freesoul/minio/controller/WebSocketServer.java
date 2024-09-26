package org.freesoul.minio.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: wjg
 * @create: 2024-09-26 11:22
 * @description: websocket的服务端
 **/

@Component
@ServerEndpoint("/api/pushMessage/{userId}")
@Slf4j
public class WebSocketServer {
    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount=0;

    /**
     * concurrent包的线程安全Map，用来存放每个客户端对应的WebSocket对象。
     */
    private static ConcurrentHashMap<String,WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收userId
     */
    private String userId;


    /**
     * 连接建立成功调用的方法
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId){
        this.session = session;
        this.userId  = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            //加入set中
            webSocketMap.put(userId, this);
        } else {
            //加入set中
            webSocketMap.put(userId, this);
            //在线数加1
            addOnlineCount();
        }

        log.info("用户{}连接, 当前人数为{}",userId,getOnlineCount());
        try {
            sendMessage(userId + "连接成功");
        } catch (IOException e) {
            log.error("用户:{},网络异常!!!!!!", userId);
        }

    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(){
        if(webSocketMap.containsKey(userId)){
            // 从map中删除
            webSocketMap.remove(userId);
            subOnlineCount();
        }
        log.info("用户{}退出, 当前人数为{}",userId,getOnlineCount());
    }


    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     **/
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息:{},报文:{}", userId, message);
        // 可以群发消息
        // 消息保存到数据库、redis
        if (StringUtils.isNotBlank(message)) {
            try {
                //解析发送的报文
                JSONObject jsonObject = JSON.parseObject(message);
                System.out.println(jsonObject);
                //追加发送人(防止串改)
                jsonObject.put("fromUserId", this.userId);
                System.out.println(jsonObject);

                String toUserId = jsonObject.getString("toUserId");
                //传送给对应toUserId用户的websocket
                if (StringUtils.isNotBlank(toUserId) && webSocketMap.containsKey(toUserId)) {
                    webSocketMap.get(toUserId).sendMessage(message);
                } else {
                    //否则不在这个服务器上，发送到mysql或者redis
                    log.error("请求的userId:{}不在该服务器上", toUserId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:{},原因:{}", this.userId, error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     *
     * @param message
     */
    private void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount(){
        return WebSocketServer.onlineCount;
    }


    /**
     * 在线人数+1
     * @return
     */
    private static synchronized int addOnlineCount() {
        return WebSocketServer.onlineCount++;
    }

    /**
     * 在线人数-1
     * @return
     */
    private static synchronized int subOnlineCount() {
        return WebSocketServer.onlineCount--;
    }


}
