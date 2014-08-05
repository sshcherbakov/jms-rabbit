package org.test.app.controller;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController implements InitializingBean {
	static Logger log = LoggerFactory.getLogger(TestController.class);
	
	@Resource
	private ConnectionFactory jmsConnectionFactory;
	
	@Resource
	private Queue jmsQueue;

	@Autowired
	private TaskExecutor taskExecutor;
	
	private StringBuffer messages = new StringBuffer();
	
	
	@RequestMapping(value="/send", method = RequestMethod.GET)
    public String sendMessage(@RequestParam("payload") String payload) throws Exception {
		log.debug("Payload: {}", payload);

		Session session = jmsConnectionFactory
			.createConnection()
			.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		TextMessage message = session.createTextMessage(payload);
		
		MessageProducer producer = session
			.createProducer( unwrapProxy( jmsQueue ) );
		
		producer.send(message);
		
		return "index.html";
    }


	@RequestMapping(value="/read", method = RequestMethod.GET)
	@ResponseBody
    public String read() {
		
		return messages.toString();
	}

	
	@Override
	public void afterPropertiesSet() throws Exception {
		taskExecutor.execute(new JmsReceiver());
	}

	
    class JmsReceiver implements Runnable {

		public void run() {
			try {
				
				Connection conn = jmsConnectionFactory.createConnection();
				Session qsess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		    	try {
		    		
			    	MessageConsumer mconsumer = qsess.createConsumer( unwrapProxy( jmsQueue ) );
			    	conn.start();
			    	
			    	String text;
			    	do {
			    		// The message is a JMS TextMessage as was indicated by the 'JMSType' header
			    		// in the initial AMQP message
			    		TextMessage msg = (TextMessage) mconsumer.receive();
			    		text = msg.getText();
						log.info("Received Message: " + text);
					
						messages.append(text).append("\n");
			    	}
			    	while(!"END".equals(text));		//  <-- stop when a marker has been seen
			    	
		    	}
		    	finally {
		    		qsess.close();
		    		conn.close();
		    	}
		    	
			} 
			catch (Exception ex) {
	    		log.error("Error in Receiver", ex);
			}

		}
    	
    }

	public Queue unwrapProxy(Object proxy) throws Exception {
		
		Advised advised = (Advised) proxy;
		return (Queue) advised.getTargetSource().getTarget();
		
	}

}
