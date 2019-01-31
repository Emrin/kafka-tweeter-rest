package tweeter.dao;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.User;
import tweeter.resources.serizalization.UserSerializer;

import java.util.Properties;
import java.util.UUID;

public class ProducerUsers {
    // Args
    private Logger logger = LoggerFactory.getLogger(ProducerUsers.class);
    private KafkaProducer<String, User> producer;
    private final String topic;

    // Constructor
    public ProducerUsers(String topic){
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    // Methods
    public User register(User user, String username) {
        user.setId(username);
        ProducerRecord<String, User> record = new ProducerRecord<>(this.topic, username, user); // topic, id, object
        this.producer.send(record);
        logger.info("Created new record.");
        return user;
    }

    public void close() {
        this.producer.close();
    }
}
