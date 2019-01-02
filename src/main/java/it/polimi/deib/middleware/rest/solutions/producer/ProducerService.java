package it.polimi.deib.middleware.rest.solutions.producer;

import it.polimi.deib.middleware.rest.commons.AbstractService;
import it.polimi.deib.middleware.rest.commons.Resp;
import it.polimi.deib.middleware.rest.commons.resources.Resource;
import it.polimi.deib.middleware.rest.commons.resources.serizalization.ResourceSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

import static spark.Spark.*;


public class ProducerService extends AbstractService {

    private static KafkaProducer<String, Resource> producer;
    private static final String topic = "resources";

    public static void main(String[] args) {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResourceSerializer.class.getName());
        producer = new KafkaProducer<>(props);


        logger = LoggerFactory.getLogger(ProducerService.class);
        port(4242);
        path("/producer", () -> {
            before("/*", (q, a) -> logger.info("ProducerDAO Received api call"));
            path("/resources", () -> {
                post("", (request, response) -> {
                    response.type("application/json");
                    response.status(SUCCESS);
                    String body = request.body();
                    if (body != null && !body.isEmpty()) {
                        String id = place(gson.fromJson(body, Resource.class));
                        return gson.toJson(new Resp(SUCCESS, "Tweet Created with id [" + id + "]"));
                    } else
                        return gson.toJson(new Resp(400, "Bad Request"));
                });

                //What about PUT? Can I modify a record in kafka?
                //But I can invalidate it
            });
        });
    }


    public static String place(Resource oder) {
        String s = UUID.randomUUID().toString().split("-")[0];
        oder.setId(s);
        ProducerRecord<String, Resource> record = new ProducerRecord<>(topic, s, oder);
        producer.send(record);
        return s;
    }


}
