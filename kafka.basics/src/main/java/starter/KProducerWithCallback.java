package kafka.starter;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KProducerWithCallback {
    public static void main(String[] args) {
        final Logger logger = LoggerFactory.getLogger(KProducerWithCallback.class);

        String bootstrapServer = "<hostname>:9092";

        //craete Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        for (int i=0; i<10; i++) {
            //create a producer record
            ProducerRecord<String, String> record = new ProducerRecord<String, String>("first_topic", "hello topic " + Integer.toString(i));

            //send data - asynchronous
            producer.send(record, new Callback() {
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    //executes every time a record is successfully sent or exception is thrown
                    if (e == null) {
                        //the record was successfully sent
                        logger.info("Received new metadata. \n" +
                                "Topic: " + recordMetadata.topic() + "\n" +
                                "Partition: " + recordMetadata.partition() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Timestamp: " + recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing", e);
                    }
                }
            });
        }
        //flush data
        producer.flush();
        //flush and close producer
        producer.close();
    }
}
