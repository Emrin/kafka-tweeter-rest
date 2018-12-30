package it.polimi.deib.middleware.rest.stub.consumer;

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

public class ConsumerServiceWSStub extends AbstractService {

}
