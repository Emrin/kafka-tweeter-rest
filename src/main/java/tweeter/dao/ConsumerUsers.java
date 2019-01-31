package tweeter.dao;

import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.User;
import tweeter.resources.serizalization.UserDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ConsumerUsers {
    // Args
    private Logger logger = LoggerFactory.getLogger(ConsumerUsers.class);
    private KafkaConsumer<String, User> consumer;

    // Constructor
    public ConsumerUsers(String topic){ // lazy load ??? no cause exec at start...
        Properties propsCons = new Properties();
        propsCons.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        propsCons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        propsCons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsCons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsCons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserDeserializer.class);
        consumer = new KafkaConsumer<>(propsCons);
        consumer.subscribe(Collections.singletonList(topic));


        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToBeginning(assignment);
        consumer.poll(Duration.ofMillis(100));
    }

    // Methods

    // Poll to see if user is already present.
    public boolean poll(String username) {
        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToBeginning(assignment);
        ConsumerRecords<String, User> results = consumer.poll(Duration.ofMillis(100));
        boolean result = false;
        for (ConsumerRecord<String, User> consumerRecord : results) {
            if(consumerRecord.key().equals(username)) {
                result = true;
            }
        }
        return result;
    }


}