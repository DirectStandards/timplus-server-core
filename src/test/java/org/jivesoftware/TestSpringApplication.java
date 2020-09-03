package org.jivesoftware;

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@SpringBootApplication
public class TestSpringApplication implements ApplicationContextAware
{
	protected static ApplicationContext ctx;
	
    public static void main(String[] args) 
    {
        SpringApplication.run(TestSpringApplication.class, args);
    }  
    
    public static ApplicationContext getApplicationContext()
    {
    	return ctx;
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		ctx = applicationContext;
	}
}
