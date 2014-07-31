package org.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;

/**
 * Simple unit tests that illustrates the AMQP message publishing to a RabbitMQ queue
 * and message consumption from the same queue using JMS
 * 
 */
public class JmsRabbitTest {
	static Logger log = LoggerFactory.getLogger(JmsRabbitTest.class);
	final static int NUM_MESSAGES = 10;
	
	private ConnectionFactory 		amqpConnectionFactory;
	private RMQConnectionFactory 	jmsConnectionFactory;
	private RMQDestination 			jmsDestination;

	private Properties props;
	private CountDownLatch latch = new CountDownLatch(NUM_MESSAGES);
	
	@Before
	public void setUp() throws FileNotFoundException, IOException {
		props = new Properties();
		// load configuration from the 'src/test/resources/application.properties' file
		props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
		
		jmsConnectionFactory = new RMQConnectionFactory();
		jmsConnectionFactory.setUsername(getUsername());
		jmsConnectionFactory.setPassword(getPassword());
		jmsConnectionFactory.setVirtualHost(getVhost());
		jmsConnectionFactory.setHost(getHostname());

		jmsDestination = new RMQDestination();
		jmsDestination.setDestinationName(getQueuename());
		jmsDestination.setAmqp(true);
		jmsDestination.setAmqpQueueName(getQueuename());
		
		amqpConnectionFactory = new ConnectionFactory();
		amqpConnectionFactory.setUsername(getUsername());
		amqpConnectionFactory.setPassword(getPassword());
		amqpConnectionFactory.setVirtualHost(getVhost());
		amqpConnectionFactory.setHost(getHostname());
		amqpConnectionFactory.setPort(getPort());
	}
	
	/**
	 * Main test entry point
	 * @throws Exception
	 */
    @Test
    public void testJmsRabbitConsume() throws Exception {
    	log.info("Starting the test client");
    	
        Connection conn = amqpConnectionFactory.newConnection();    
        Channel channel = conn.createChannel();
        try {
        	
        	// The queue by default is bound to a default exchange
        	channel.queueDeclare(getQueuename(), true, false, false, null);

        	// The queue is ready, start the parallel JMS consumer 
        	Thread consumerThread = startJmsConsumer();
        	
        	// Publish several text messages to the AMQP queue
        	for( int i=0; i<NUM_MESSAGES; i++ ) {
        		publishAmqpMessage(channel, "Hello " + i);
        	}
        	
        	// The last one is a marker for JMS consumer to stop 
        	publishAmqpMessage(channel, "END");
        	
        	// wait for the JMS consumer to finish gracefully
        	consumerThread.join();
        	
        	assertTrue(latch.await(3, TimeUnit.SECONDS));
        }
        finally {
        	// cleanup
        	channel.close();
        	conn.close();
        }        
    	
    }

    /**
     * Publish a text message to the AMQP queue.
     * The message is published to the default exchange with the routing key equal
     * to the queue name.
     * 'JMSType' header must be set to 'TextMessage' in order to be mapped to the 
     * same JMS header indicating the type of the JMS message. 
     * 
     * @param channel
     * @param payload
     * @throws IOException
     */
	@SuppressWarnings("deprecation")
	private void publishAmqpMessage(Channel channel, String payload) throws IOException {
		
		BasicProperties amqpProperties = MessageProperties.TEXT_PLAIN;
		Map<String,Object> headers = new HashMap<String,Object>(1);
		headers.put("JMSType", "TextMessage");
		amqpProperties.setHeaders(headers);
		
		channel.basicPublish("", getQueuename(), amqpProperties, payload.getBytes());
	}

    /**
     * Helper method to start JMS consumer thread
     * @return
     */
    private Thread startJmsConsumer() {

    	Thread consumerThread = new Thread(new JmsReceiver());
    	consumerThread.start();
        return consumerThread;
    }
    
    
    /**
     * The {@link Runnable} task to be executed in parallel
     * with JMS consumer waiting and processing text JMS messages
     * coming from the Rabbit queue 
     *
     */
    class JmsReceiver implements Runnable {

		public void run() {
			try {
				
				QueueConnection conn = jmsConnectionFactory.createQueueConnection();
				// Create new non-transactional JMS QueueSession that will auto acknowledge
				// received messages
				QueueSession qsess = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		    	try {
		    		
			    	MessageConsumer mconsumer = qsess.createConsumer(jmsDestination);
			    	conn.start();
			    	
			    	String text;
			    	do {
			    		// The message is a JMS TextMessage as was indicated by the 'JMSType' header
			    		// in the initial AMQP message
			    		TextMessage msg = (TextMessage) mconsumer.receive();
			    		text = msg.getText();
						log.info("Received Message: " + text);
						latch.countDown();
			    	}
			    	while(!"END".equals(text));		//  <-- stop when a marker has been seen
			    	
		    	}
		    	finally {
		    		qsess.close();
		    		conn.close();
		    	}
		    	
			} 
			catch (JMSException ex) {
	    		log.error(ex.toString());
			}

		}
    	
    }

    
	String getUsername() {
		return props.getProperty("username");
	}
	
	String getPassword() {
		return props.getProperty("password");
	}
	
	String getVhost() {
		return props.getProperty("vhost");
	}
	
	String getHostname() {
		return props.getProperty("hostname");
	}
	
	String getQueuename() {
		return props.getProperty("queuename");
	}
	
	int getPort() {
		return Integer.valueOf(props.getProperty("port"));
	}

}
