package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class DefaultProxyServerCredentialProviderTest
{
	@Test
	public void testNewCredential() throws Exception
	{
		DefaultProxyServerCredentialProvider prov = new DefaultProxyServerCredentialProvider();
		ProxyServerCredential cred = prov.newCredential();
		
		assertNotNull(cred);
		
		assertEquals(26, cred.getSubject().length());
		assertEquals(26, cred.getSecret().length());
		assertNotNull(cred.getSecretHash());
		assertNotNull(cred.getCreationDate());
	}
}
