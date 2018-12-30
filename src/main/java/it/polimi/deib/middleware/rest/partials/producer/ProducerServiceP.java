package it.polimi.deib.middleware.rest.partials.producer;

import it.polimi.deib.middleware.rest.commons.AbstractService;
import it.polimi.deib.middleware.rest.commons.resources.Resource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

import static spark.Spark.*;

public class ProducerServiceP extends AbstractService {

    private static KafkaProducer<String, Resource> producer;
    private static final String topic = "resources";

    public static void main(String[] args) {

        Properties props = new Properties();
        //TODO SET UP PRODUCER PROPERTIES (server, serialization...

        //TODO create producer and subscribe to topic

        logger = LoggerFactory.getLogger(ProducerServiceP.class);
        port(4242);
        path("/producer", () -> {
            before("/*", (q, a) -> logger.info("ProducerDAO Received api call"));
            path("/resources", () -> {
                //TODO post a message to the topic, if well done

            });
        });
    }


    public static String place(Resource oder) {
        String s = UUID.randomUUID().toString().split("-")[0];
        oder.setId(s);

        //TODO create the record and push it to kafka
        return s;
    }


}
