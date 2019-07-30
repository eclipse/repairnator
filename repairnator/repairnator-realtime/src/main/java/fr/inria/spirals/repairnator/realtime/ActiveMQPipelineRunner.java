package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.MessageListener;

/*For reciever*/
import javax.jms.Message;
import javax.jms.MessageConsumer;

import org.apache.activemq.ActiveMQConnectionFactory;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import java.util.concurrent.TimeUnit;
/**
 * This class will take the builds qualified by the inspectBuild
 * and submit to an ActiveMQ queue for the repairnator-worker
 * to run on Kubernetes.
 */
public class ActiveMQPipelineRunner implements PipelineRunner,MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQPipelineRunner.class);
    private static final int DELAY_BETWEEN_DOCKER_IMAGE_REFRESH = 60; // in minutes
    private static final RepairnatorConfig config = RepairnatorConfig.getInstance();

    public ActiveMQPipelineRunner(){}

    public Boolean testConnection() {
        try {
            /*
             * Getting JMS connection from the JMS server and starting it
             */
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl());
            Connection connection = connectionFactory.createConnection();
            connection.start();

            /*
             * Creating a non transactional session to send/receive JMS message.
             */
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            /*
             * The queue will be created automatically on the server.
             */
            Destination destination = session.createQueue("Con_Test");

            /*
             * Destination represents here our queue 'MESSAGE_QUEUE' on the JMS server.
             * 
             * MessageProducer is used for sending messages to the queue.
             */
            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage("Testing");

            producer.send(message);
            connection.close();
            LOGGER.warn("Connection to activemq Succeeded"); 
            LOGGER.warn("Connected to url: " + config.getActiveMQUrl() + " and queueName: " + "Con_test");

            return true;
        }catch(JMSException e){
            LOGGER.warn("Tried to connect to url " + config.getActiveMQUrl());
            LOGGER.warn("Connection to activemq failed, please double check the ActiveMQ server"); 
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a build object, produce am ActiveMQ message and send to queue
     * @param build object containing a build id.
     */
    public void submitBuild(Build build) {
        try {
            /*
             * Getting JMS connection from the JMS server and starting it
             */
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl());
            Connection connection = connectionFactory.createConnection();
            connection.start();

            /*
             * Creating a non transactional session to send/receive JMS message.
             */
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            /*
            * The queue will be created automatically on the server.
            */
            Destination destination = session.createQueue(config.getActiveMQQueueName());

            /*
             * Destination represents here our queue 'MESSAGE_QUEUE' on the JMS server.
             * 
             * MessageProducer is used for sending messages to the queue.
             */
            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage(Long.toString(build.getId()));

            producer.send(message);

            LOGGER.info("Build id '" + message.getText() + ", Sent Successfully to the Queue " + config.getActiveMQQueueName());
            connection.close();
        }catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initRunner() {
        // so far, nothing to set up the connection
    }

    /**
     * Fetch one message from queue and done
     * 
     * @return a String message
     */
    public String receiveBuildFromQueue() {
        try {
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl());

            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue(config.getActiveMQQueueName());

            // Create a MessageConsumer from the Session to the Topic or Queue
            MessageConsumer consumer = session.createConsumer(destination);

            // Wait for a message
            Message message = consumer.receive(1000);

            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();

            consumer.close();
            session.close();
            connection.close();
            return text;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run this as a listener server and fetch one message as a time
     */
    public void runAsConsumerServer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl() + "?jms.prefetchPolicy.all=1");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false,Session.CLIENT_ACKNOWLEDGE);
            Destination queue = session.createQueue(config.getActiveMQQueueName());

            MessageConsumer responseConsumer = session.createConsumer(queue);
            responseConsumer.setMessageListener(this);
            LOGGER.warn("Listening");
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method implemented from MessageListener and is called 
     * each time this is done with the previous message
     *
     * @param message ActiveMQ message object containing a string message.
     */
    public void onMessage(Message message) {
        String messageText = null;
        try {
            message.acknowledge();
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch(java.lang.InterruptedException e) {

            }
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                messageText = textMessage.getText();
                System.out.println("messageText = " + messageText);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
