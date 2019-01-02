package tweeter.resources.serizalization;

import org.apache.kafka.common.serialization.Deserializer;
import org.codehaus.jackson.map.ObjectMapper;
import tweeter.resources.Tweet;

import java.util.Map;

public class TweetDeserializer implements Deserializer<Tweet> {
    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public Tweet deserialize(String topic, byte[] data) {
        ObjectMapper mapper = new ObjectMapper();
        Tweet request = null;
        try {
            request = mapper.readValue(data, Tweet.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return request;
    }

    @Override
    public void close() {

    }
}
