package org.jivesoftware.openfire.trustanchor;

import static org.junit.Assert.assertNotNull;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustAnchorProvider_addAnchorTest extends SpringDataBaseTest
{
	protected X509Certificate testAnchor;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	@Test
	public void testAddAnchor_newAnchor_assertAdded() throws Exception
	{
		final X509Certificate testAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		trustAnchorProv.addTrustAnchor(testAnchor);
		
		final TrustAnchor retAnchor = trustAnchorProv.getAnchorByThumbprint(Thumbprint.toThumbprint(testAnchor).toString());
		
		assertNotNull(retAnchor);
	}
	
	@Test(expected=TrustAnchorAlreadyExistsException.class)
	public void testAddAnchor_existingAnchor_assertAdded() throws Exception
	{
		final X509Certificate testAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		trustAnchorProv.addTrustAnchor(testAnchor);
		
		trustAnchorProv.addTrustAnchor(testAnchor);
		
	}
}
