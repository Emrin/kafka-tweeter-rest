package it.polimi.deib.middleware.rest.commons2.resources.serizalization;

import it.polimi.deib.middleware.rest.commons2.resources.Resource;
import org.apache.kafka.common.serialization.Deserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Map;

public class ResourceDeserializer implements Deserializer<Resource> {
    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public Resource deserialize(String topic, byte[] data) {
        ObjectMapper mapper = new ObjectMapper();
        Resource request = null;
        try {
            request = mapper.readValue(data, Resource.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return request;
    }

    @Override
    public void close() {

    }
}
