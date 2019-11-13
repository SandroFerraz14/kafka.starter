package kafka.starter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KConsumerWithThread {
    public static void main(String[] args) {
        new KConsumerWithThread().run();
    }
    private KConsumerWithThread(){

    }

    private void run(){
        Logger logger = LoggerFactory.getLogger(KConsumerWithThread.class.getName());

        String bootstrapServer = "<hostname>:9092";
        String groupId = "my-sixth-application";
        String topic = "first_topic";

        //latch for dealing with multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        //create the consumer runnnable
        logger.info("Creating the consumer thread");
        Runnable myConsumerRunnable = new ConsumerRunnable(
                bootstrapServer,
                groupId,
                topic,
                latch
        );

        //start the thread
        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        //add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing");
        }
    }

    public class ConsumerRunnable implements Runnable {

        private CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        public ConsumerRunnable(String bootstrapServer,
                              String groupId,
                              String topic,
                              CountDownLatch latch){
            this.latch = latch;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); //read from beginning of topic
                                                                                        //latest ---> read only new messages of topic
            //create consumer
            consumer = new KafkaConsumer<String, String>(properties);
            //subscribe consumer to our topic(s)
            consumer.subscribe(Arrays.asList(topic));
        }

        @Override
        public void run() {
            //poll for new data
            try{
                while(true){
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); //new in Kafka 2.0.0

                    for (ConsumerRecord<String, String> record : records){
                        logger.info("Key: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch (WakeupException e){
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                //tell our main code we're done with consumer
                latch.countDown();
            }

        }

        public void shutdown(){
            //the wakeup() method is a special method to interrupt consumer.poll()
            //it will throw the exception WakeUpException
            consumer.wakeup();
        }
    }
}
