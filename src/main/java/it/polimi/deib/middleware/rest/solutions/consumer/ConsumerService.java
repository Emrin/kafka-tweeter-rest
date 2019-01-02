package it.polimi.deib.middleware.rest.solutions.consumer;

import it.polimi.deib.middleware.rest.commons.AbstractService;
import it.polimi.deib.middleware.rest.commons.Resp;
import it.polimi.deib.middleware.rest.commons.resources.Resource;
import it.polimi.deib.middleware.rest.commons.resources.serizalization.ResourceDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static spark.Spark.*;

public class ConsumerService extends AbstractService {

    private static KafkaConsumer<String, Resource> consumer;

    private static Map<String, Resource> resources = new HashMap<>();

    public static void main(String[] args) {

        logger = LoggerFactory.getLogger(ConsumerService.class);

        Properties props = new Properties();

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResourceDeserializer.class);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("resources"));

        port(3939);
        consumer.poll(Duration.ofMillis(100));
        Set<TopicPartition> assignment = consumer.assignment();

        path("/consumer", () -> {
            before("/*", (q, a) -> logger.info("ProducerDAO Received api call"));
            path("/resources", () -> {
                get("/:id", (request, response) -> {
                    String id = request.params(":id");
                    if (resources.containsKey(id)) {
                        return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.get(id))));

                    }
                    return gson.toJson(new Resp(CLIENT_ERROR + 4, "Tweet not found"));
                });

                get("", (request, response) -> {
                    response.type("application/json");
                    response.status(SUCCESS);
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
                });

                delete("/:id", (request, response) -> {
                    String id = request.params(":id");
                    Resource remove = resources.remove(id);
                    if (remove != null) {
                        return gson.toJson(new Resp(SUCCESS, "Tweet [" +
                                id +
                                "] successfully removed."));
                    }

                    return gson.toJson(new Resp(CLIENT_ERROR + 4, "Tweet not found"));
                });

            });
        });
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

    private static void poll() {
        ConsumerRecords<String, Resource> orders = consumer.poll(Duration.ofMillis(500));
        orders.forEach(pr -> resources.put(pr.key(), pr.value()));
    }

}
