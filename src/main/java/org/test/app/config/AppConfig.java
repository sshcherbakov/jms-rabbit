package org.test.app.config;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {
	
	@Bean
	public JndiObjectFactoryBean jmsConnectionFactory() {
	
		JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
		factoryBean.setJndiName("java:comp/env/jms/myrabbit");
		factoryBean.setExpectedType(ConnectionFactory.class);
		factoryBean.setLookupOnStartup(false);
		
		return factoryBean;
	}

	@Bean
	public JndiObjectFactoryBean jmsJndiQueue() {
		
		JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
		factoryBean.setJndiName("java:comp/env/jms/test_queue");
		factoryBean.setExpectedType(Queue.class);
		factoryBean.setLookupOnStartup(false);
		factoryBean.setProxyInterface(Queue.class);
		
		return factoryBean;
		
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);
		return executor;
	}
}
