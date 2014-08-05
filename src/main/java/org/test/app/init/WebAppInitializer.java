package org.test.app.init;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import org.test.app.config.AppConfig;
import org.test.app.config.WebConfig;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
    public Class<?>[] getRootConfigClasses() {
        return new Class<?>[] { AppConfig.class };
    }

	@Override
    public Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebConfig.class };
    }

	@Override
    public String[] getServletMappings() {
        return new String[] { "/" };
    }
	
}