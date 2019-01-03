package tweeter.api;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.api.commons.AbstractService;
import tweeter.resources.Tweet;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;





// deprecated.....




public class ConsumerServiceWS extends AbstractService {
    private Logger logger = LoggerFactory.getLogger(ConsumerServiceWS.class);

    // Args
    private KafkaConsumer<String, Tweet> consumer;
    private int portNum;
    private String topic;
    private List<String> filter;

    // Constructor
    public ConsumerServiceWS(KafkaConsumer<String, Tweet> consumer, int portNum, String topic) {
        this.consumer = consumer;
        this.portNum = portNum;
        this.topic = topic;
    }

    // Getters and setters
    public List<String> getFilter() {
        return filter;
    }
    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    // Methods
    public void start() {
//        port(portNum);
        consumer.subscribe(Collections.singletonList(topic));
        consumer.poll(Duration.ofMillis(100));
        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToBeginning(assignment);

//        WebSocketHandler wsh = new WebSocketHandler(consumer, filter.get(0), filter.get(1), filter.get(2));
//        webSocket("/ws", wsh);
//        init();

        Thread t = new Thread(() -> {
            while (true) {
                logger.info("Polling...");
//                wsh.poll();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

}


