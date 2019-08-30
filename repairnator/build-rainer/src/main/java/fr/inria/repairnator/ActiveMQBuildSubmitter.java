package fr.inria.repairnator;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
/*For sender*/
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

/*For consumer*/
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;

/*For broker to check queue size*/
import org.apache.activemq.broker.jmx.*;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.*;
import org.apache.activemq.broker.jmx.DestinationViewMBean;

/*
 * This class is used to send a text message to the ActiveMQ queue.
 */
public class ActiveMQBuildSubmitter implements BuildSubmitter {
    private static Logger LOGGER = LoggerFactory.getLogger(ActiveMQBuildSubmitter.class);
    private DestinationViewMBean mbView;
    private long mostRecentQueueSize = 0;

    // This is for getting queueSize
    public void initBroker() {
        try {
            String url = "service:jmx:rmi:///jndi/rmi://" + RepairnatorConfig.getInstance().getJmxHostName() + ":1099/jmxrmi";
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            // get queue size
            ObjectName nameConsumers = new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=" + RepairnatorConfig.getInstance().getActiveMQSubmitQueueName());
            this.mbView = MBeanServerInvocationHandler.newProxyInstance(connection, nameConsumers, DestinationViewMBean.class, true);
        } catch(Exception e){
		    throw new RuntimeException(e);
        }
    }

	public void submit(String str_message) {
		try {
			if (mostRecentQueueSize < RepairnatorConfig.getInstance().getQueueLimit()) {
				/*
				* Getting JMS connection from the JMS server and starting it
				*/
				ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(RepairnatorConfig.getInstance().getActiveMQUrl());
				Connection connection = connectionFactory.createConnection();
				connection.start();

				/*
				* Creating a non transactional session to send/receive JMS message.
				*/
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

				/*
				* The queue will be created automatically on the server.
				*/
				Destination destination = session.createQueue(RepairnatorConfig.getInstance().getActiveMQSubmitQueueName());

				/*
				* Destination represents here our queue 'MESSAGE_QUEUE' on the JMS server.
				* 
				* MessageProducer is used for sending messages to the queue.
				*/
				MessageProducer producer = session.createProducer(destination);
				TextMessage message = session.createTextMessage(str_message);
				producer.send(message);

				LOGGER.info("Message " + str_message + " was sent successfully");
				connection.close();

				/*Update info about queue size*/
				mostRecentQueueSize = this.mbView.getQueueSize();
			} else {
				LOGGER.warn("No Submission, Reason: queue reached limit= " + RepairnatorConfig.getInstance().getQueueLimit() + " , currentQueueSize: " + mostRecentQueueSize);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * This is used to test the submit method.
	 */
	public String receiveFromQueue() {
		try {
			// Create a ConnectionFactory
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(RepairnatorConfig.getInstance().getActiveMQUrl());

			// Create a Connection
			Connection connection = connectionFactory.createConnection();
			connection.start();

			// Create a Session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(RepairnatorConfig.getInstance().getActiveMQSubmitQueueName());

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
}
