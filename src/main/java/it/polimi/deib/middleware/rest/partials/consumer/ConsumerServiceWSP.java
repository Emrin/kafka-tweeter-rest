package it.polimi.deib.middleware.rest.partials.consumer;

import it.polimi.deib.middleware.rest.commons.AbstractService;
import it.polimi.deib.middleware.rest.commons.resources.Resource;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static spark.Spark.init;
import static spark.Spark.port;

public class ConsumerServiceWSP extends AbstractService {

    private static KafkaConsumer<String, Resource> consumer;
    private static final String topic = "resources";

    public static void main(String[] args) {

        logger = LoggerFactory.getLogger(ConsumerServiceWSP.class);

        Properties props = new Properties();

        //TODO SET UP CONSUMER PROPERTIES (gid, serve, offset, deserialization...

        //TODO create consumer and subscribe to topic

        port(4040);

        //TODO complete, instantiate a WebSocketHandler class


        init();

        Thread t = new Thread(() -> {
            //TODO poll the beginning of the topic and set up a periodic poll
        });

        t.start();

    }

    @WebSocket
    public static class WebSocketHandler {

        private final KafkaConsumer<String, Resource> consumer;
        List<Session> users = new ArrayList<>();

        public WebSocketHandler(KafkaConsumer<String, Resource> consumer) {
            this.consumer = consumer;
        }

        public void poll() {
            //TODO broadcast polled messages
        }

        @OnWebSocketConnect
        public void onConnect(Session user) throws Exception {
            //TODO
            logger.info("A user joined the chat");
        }

        @OnWebSocketClose
        public void onClose(Session user, int statusCode, String reason) {
            //TODO
            logger.info("A user left the chat");
        }

        @OnWebSocketMessage
        public void onMessage(Session user, String message) {
            //DO nothings
        }

        private void broadcast(String message) {
            //TODO
        }
    }
}
