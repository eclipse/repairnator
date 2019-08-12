package fr.inria.spirals.repairnator.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.Listener;

import javax.jms.Message;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;

import org.apache.activemq.ActiveMQConnectionFactory;
/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public class ScannerBuildListener implements Listener,MessageListener{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerBuildListener.class);
    private static final RepairnatorConfig config = RepairnatorConfig.getInstance();
    private static Launcher launcher;

    public ScannerBuildListener (Launcher launcher) {
        this.launcher = launcher;
    }
    
    public void submitBuild(String buildStr) {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl());
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Destination destination = session.createQueue(config.getActiveMQQueueName());

            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage(buildStr);

            producer.send(message);

            LOGGER.info("Build id '" + message.getText() + ", Sent Successfully to the Queue " + config.getActiveMQQueueName());
            connection.close();
        }catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void runListenerServer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl() + "?jms.prefetchPolicy.all=1");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false,Session.CLIENT_ACKNOWLEDGE);
            Destination queue = session.createQueue(config.getActiveMQListenQueueName());

            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
            LOGGER.warn("Server is now listening for build ids");
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
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                messageText = textMessage.getText();
                LOGGER.info("A new slug has arrived: " + messageText);
                this.launcher.kubernetesProcess(messageText);
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] data = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(data);
                messageText = new String(data);
                LOGGER.info("A new slug has arrived: " + messageText);
                this.launcher.kubernetesProcess(messageText);
            } 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
