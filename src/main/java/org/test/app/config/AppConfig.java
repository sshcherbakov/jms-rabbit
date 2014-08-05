package org.test.app.config;

import java.io.IOException;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {
	 
	@Bean
	public Properties jndiEnvironment() throws IOException {
		Properties properties = new Properties();
		properties.load(
				Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("/jndi.properties"));
		return properties;
	}
	
	@Bean
	public JndiTemplate jndiTemplate() throws IOException {
		JndiTemplate jndiTemplate = new JndiTemplate(jndiEnvironment());
		return jndiTemplate;
	}
	
	@Bean
	public JndiObjectFactoryBean jmsConnectionFactory(JndiTemplate jndiTemplate) {
	
		JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
		factoryBean.setJndiName("java:comp/env/jms/myrabbit");
		factoryBean.setExpectedType(ConnectionFactory.class);
		factoryBean.setLookupOnStartup(false);
		factoryBean.setJndiTemplate(jndiTemplate);
		
		return factoryBean;
	}

	@Bean
	public JndiObjectFactoryBean jmsJndiQueue(JndiTemplate jndiTemplate) {
		
		JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
		factoryBean.setJndiName("java:comp/env/jms/test_queue");
		factoryBean.setExpectedType(Queue.class);
		factoryBean.setLookupOnStartup(false);
		factoryBean.setProxyInterface(Queue.class);
		factoryBean.setJndiTemplate(jndiTemplate);
		
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
