package com.jpmc.midascore.component;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // 1. Decoupling Configuration: topics = "${general.kafka-topic}"
    // The Concept:
    // When you write ${some.value}, you are using Spring Expression Language. You
    // are telling the application,
    // "Do not use a fixed string here; go look in the application.yml file and find
    // the value attached to this key."
    /*
     * The Concept: Kafka uses the groupId to organize listeners into "teams."
     * 
     * Why it matters:
     * If a trading engine is blasting 10,000 updates a second into the
     * trader-updates topic,
     * a single instance of your application might crash trying to process them all.
     * To fix this,
     * you would boot up three identical instances of your application on different
     * servers.
     * 
     * Scenario A (Same Group ID): Because all three instances share the groupId =
     * "midas-core",
     * Kafka recognizes them as a single team. Kafka will load-balance the work.
     * Instance 1 handles message A, Instance 2 handles message B, and Instance 3
     * handles message C.
     * The workload is divided, making the system highly scalable.
     * 
     * Scenario B (Different Group IDs): If you gave them different group IDs (e.g.,
     * "midas-core-1", "analytics-engine", "audit-logger"),
     * Kafka treats them as entirely separate, unrelated applications. Kafka would
     * send a copy of every single message to every single instance.
     */
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core")
    public void transactionMessage(Transaction t) {
        long senderId = t.getSenderId();
        long recipientId = t.getRecipientId();
        UserRecord sender = userRepository.findById(senderId);
        UserRecord recipient = userRepository.findById(recipientId);

        if (sender == null || recipient == null)
            return;
        if (sender.getBalance() < t.getAmount())
            return;

        RestTemplate restTemplate = new RestTemplate();
        Incentive incentiveObj = restTemplate.postForObject("http://localhost:8080/incentive", t, Incentive.class);

        sender.setBalance(sender.getBalance() - t.getAmount());
        recipient.setBalance(recipient.getBalance() + t.getAmount() + incentiveObj.getAmount());

        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord newTransaction = new TransactionRecord(sender, recipient, t.getAmount(),
                incentiveObj.getAmount());
        transactionRepository.save(newTransaction);
    }
    // Because of YAML config, Spring automatically intercepts the JSON from
    // "trader-updates",
    // checks the trusted packages, maps the JSON keys to the fields in the
    // Transaction class,
    // and passes the fully built object right into this method.
}