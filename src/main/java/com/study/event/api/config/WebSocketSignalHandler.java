package com.study.event.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSocketSignalHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSignalHandler.class);

    private final List<WebSocketSession> sessions = new ArrayList<>();

    @PostConstruct
    public void init() {
        logger.info("[Server] SignalWebSocketHandler initialized.");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("[WebSocket] Connection established: {}", session.getId());
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("[WebSocket] Connection closed: {}", session.getId());
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("[WebSocket] Transport error in session {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("[WebSocket] Message received from {}: {}", session.getId(), message.getPayload());

        for (WebSocketSession ws : sessions) {
            if (ws.isOpen() && !ws.getId().equals(session.getId())) {
                try {
                    ws.sendMessage(new TextMessage(message.getPayload()));
                    logger.info("[WebSocket] Message forwarded to {}: {}", ws.getId(), message.getPayload());
                } catch (IOException e) {
                    logger.error("[WebSocket] Error sending message to {}: {}", ws.getId(), e.getMessage());
                }
            }
        }
    }
}
