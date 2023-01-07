package io.conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());

    public static void main(String[] args) {
        log.info("Hello,Im a consumer");
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-third-application";
        String topic = "demo";
        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        // properties.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, ""); //for static ID assignment.
        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();

        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();
                // join the main thread to allow the execution of the code in the main thread.
                try{
                    mainThread.join();
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        try{
            // subscribe consumer to topics
            consumer.subscribe(Arrays.asList(topic));
            // poll for new data
            while(true){
                log.info("Polling");
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(100)); // how long are we willing to wait before next
                for(ConsumerRecord<String, String> record: records){
                    log.info("key: " + record.key() + ", Value: " + record.value());
                    log.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }
            }
        } catch(WakeupException e){
            log.info("Wake up exception!");
        } catch(Exception e){
            log.error("Unexpected exception");
        } finally {
            consumer.close(); //this will commit the offsets if need be
            log.info("The consumer is now gracefully closed");
        }


    }
}
