package it.polimi.deib.middleware.rest.partials.consumer;

import it.polimi.deib.middleware.rest.commons.AbstractService;
import it.polimi.deib.middleware.rest.commons.resources.Resource;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.LoggerFactory;

import java.util.*;

import static spark.Spark.*;

public class ConsumerServiceP extends AbstractService {

    private static KafkaConsumer<String, Resource> consumer;

    private static Map<String, Resource> resources = new HashMap<>();
    private static final String topic = "resources";

    public static void main(String[] args) {

        logger = LoggerFactory.getLogger(ConsumerServiceP.class);

        Properties props = new Properties();

        //TODO SET UP CONSUMER PROPERTIES (gid, serve, offset, deserialization...

        //TODO create consumer and subscribe to topic

        port(3939);

        //TODO complete, instantiate a WebSocketHandler class

        path("/consumer", () -> {
            before("/*", (q, a) -> logger.info("ProducerDAO Received api call"));
            path("/resources", () -> {
                //TODO get resource by ID

                //TODO get resources

                //TODO delete resource by ID

            });
        });

        Thread t = new Thread(() -> {
            //TODO poll the beginning of the topic and set up a periodic poll
        });

        t.start();

    }

    private static void poll() {
        //TODO save polled messages
    }

}
