package net.yourhome.server.net.rest;

import java.util.Arrays;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.yourhome.server.net.rest.rules.Rules;
import net.yourhome.server.net.rest.rules.Scenes;
import net.yourhome.server.net.rest.view.Images;
import net.yourhome.server.net.rest.view.Views;
import net.yourhome.server.net.rest.zwave.Commands;
import net.yourhome.server.net.rest.zwave.Nodes;

@Configuration
public class AppConfig {
	@Bean(destroyMethod = "shutdown")
	public SpringBus cxf() {
		return new SpringBus();
	}

	@Bean
	public Server jaxRsServer() {
		cxf();
		JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance().createEndpoint(jaxRsApiApplication(), JAXRSServerFactoryBean.class);
		factory.setBus(cxf());
		factory.setServiceBeans(Arrays.<Object> asList(new MessageHandler(),
				new Images(), 
				new Views(), 
				new Nodes(), 
				new Scenes(), 
				new Rules(), 
				new Commands(), 
				new Project(),
				new Radio(),
				new Logs(), 
				new HttpCommands(), 
				new IPCameras(), 
				new Controllers(), 
				new Info()));
		factory.setAddress('/' + factory.getAddress());
		factory.setProviders(Arrays.<Object> asList(jsonProvider()));
		return factory.create();
	}

	@Bean
	public JaxRsApiApplication jaxRsApiApplication() {
		return new JaxRsApiApplication();
	}

	@Bean
	public JacksonJsonProvider jsonProvider() {
		return new JacksonJsonProvider();
	}
	@ApplicationPath("")
	public class JaxRsApiApplication extends Application {
	}

}
