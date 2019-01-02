package it.polimi.deib.middleware.rest.tweeter.producer;

import it.polimi.deib.middleware.rest.commons2.AbstractService;
import it.polimi.deib.middleware.rest.commons2.Resp;
import it.polimi.deib.middleware.rest.commons2.resources.Resource;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceDeserializer;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceSerializer;
import it.polimi.deib.middleware.rest.tweeter.consumer.ConsumerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static spark.Spark.*;


public class ProducerService extends AbstractService {
    private static KafkaProducer<String, Resource> producer;
    private static KafkaConsumer<String, Resource> consumer;
    private static final String topic = "tweets1";

    private static Map<String, Resource> resources = new HashMap<>();

    public static void main(String[] args) {
        port(4242);
        logger = LoggerFactory.getLogger(ProducerService.class);

        // Producer
        Properties propsProd = new Properties();
        propsProd.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsProd.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        propsProd.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResourceSerializer.class.getName());
        producer = new KafkaProducer<>(propsProd);

        // Consumer
        Properties propsCons = new Properties();
        propsCons.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        propsCons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        propsCons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsCons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsCons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResourceDeserializer.class);
        consumer = new KafkaConsumer<>(propsCons);
        consumer.subscribe(Collections.singletonList("tweets1"));
        consumer.poll(Duration.ofMillis(100));        // get /tweets/filter
        Set<TopicPartition> assignment = consumer.assignment();


        // REST UTILITY
        path("/tweets", () -> {
            before("/*", (q, a) -> logger.info("Received api call to /tweets"));

            get("", (request, response) -> {
                System.out.print("get req with no args");
                response.type("application/json");
                response.status(SUCCESS);
//                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
            });

            get("/:filter", (request, response) -> {
                String filter = request.params(":filter"); // location=X, tag=X, mention=X
                System.out.print("Get request with filter = " + filter);
                if (resources.containsKey(filter)) { // if filter is legit
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.get(filter))));

                }
                return gson.toJson(new Resp(CLIENT_ERROR + 4, "Tweet not found"));
            });

            post("", (request, response) -> {
                response.type("application/json");
                response.status(SUCCESS);
                String body = request.body();
                if (body != null && !body.isEmpty()) { // optionally regex type checking
                    String id = tweet(gson.fromJson(body, Resource.class));
                    return gson.toJson(new Resp(SUCCESS, "Tweet Created with id [" + id + "]"));
                } else
                    return gson.toJson(new Resp(400, "Bad Request"));
            });

        });

        // Start consumer polling
        Thread t = new Thread(() -> {
            consumer.seekToBeginning(assignment);
            while (true) {
                logger.info("Polling...");
                poll();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        });
        t.start();
    }


    // Pushes tweet to kafka.
    public static String tweet(Resource message) {
        String s = UUID.randomUUID().toString().split("-")[0];
        message.setId(s);
        ProducerRecord<String, Resource> record = new ProducerRecord<>(topic, s, message);
        producer.send(record);
        return s;
    }

    // Polling the consumer to see if new tweets are present.
    private static void poll() {
        ConsumerRecords<String, Resource> tweets = consumer.poll(Duration.ofMillis(500));
        if (!tweets.isEmpty()) {System.out.print("new tweeet\n");}
        tweets.forEach(pr -> resources.put(pr.key(), pr.value()));
        System.out.print(resources.values().size());
    }
}
