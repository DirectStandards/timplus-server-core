package org.eclipse.jetty.util.ssl;

import java.lang.reflect.Constructor;

import javax.net.ssl.SNIMatcher;

public class SSLObjectFactory
{
	public static SNIMatcher createAliasMatcher() 
	{
		try
		{
			@SuppressWarnings("deprecation")
			final SslContextFactory factory =  new SslContextFactory();
			final Class<?> innerClazz = Class.forName("org.eclipse.jetty.util.ssl.SslContextFactory$AliasSNIMatcher");
			
			Constructor<?> constructor = innerClazz.getDeclaredConstructor(SslContextFactory.class);
			
	        constructor.setAccessible(true);

	        //and pass instance of Outer class as first argument
	        Object o = constructor.newInstance(factory);
	        
	        return (SNIMatcher)o;
		}
		catch (Exception e) 
		{
			return null;
		}
	}
}
