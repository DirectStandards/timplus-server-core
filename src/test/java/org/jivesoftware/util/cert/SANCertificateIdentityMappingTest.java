package org.jivesoftware.util.cert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.Test;

public class SANCertificateIdentityMappingTest
{
	@Test
	public void testMapSANCertificateIdentities() throws Exception
	{
		try(final InputStream inStr = this.getClass().getResourceAsStream("/certs/direct.securehealthemail.com.cer"))
		{
		
			final X509Certificate cert = (X509Certificate)CertificateFactory.getInstance("X509").generateCertificate(inStr);
			
			final SANCertificateIdentityMapping idenMap = new SANCertificateIdentityMapping();
			
			final List<String> idens = idenMap.mapIdentity(cert);
			
			assertEquals(5, idens.size());
			
			assertTrue(idens.contains("direct.securehealthemail.com"));
			assertTrue(idens.contains("groupchat.direct.securehealthemail.com"));
			assertTrue(idens.contains("ftproxystream.direct.securehealthemail.com"));
			
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			
		}
		
	}
}
