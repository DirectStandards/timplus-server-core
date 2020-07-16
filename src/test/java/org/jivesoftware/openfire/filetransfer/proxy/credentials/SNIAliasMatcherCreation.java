package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import static org.junit.Assert.assertNotNull;


import org.eclipse.jetty.util.ssl.SSLObjectFactory;
import org.junit.Test;

public class SNIAliasMatcherCreation
{
	@Test
	public void testCreateAliasMatcher() throws Exception 
	{
		assertNotNull(SSLObjectFactory.createAliasMatcher());
		
	}
}
