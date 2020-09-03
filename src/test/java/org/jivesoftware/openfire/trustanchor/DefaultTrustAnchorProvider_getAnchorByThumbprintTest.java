package org.jivesoftware.openfire.trustanchor;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustAnchorProvider_getAnchorByThumbprintTest extends SpringDataBaseTest
{
	protected X509Certificate testAnchor;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		testAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		trustAnchorProv.addTrustAnchor(testAnchor);
	}
	
	@Test
	public void testGetAnchorByThumbprint_anchorExists_assertRetrieved() throws Exception
	{
		final TrustAnchor retAnchor = trustAnchorProv.getAnchorByThumbprint(Thumbprint.toThumbprint(testAnchor).toString());
		
		assertEquals(testAnchor, retAnchor.asX509Certificate());
		assertEquals(testAnchor.getSubjectDN().toString(), retAnchor.getDistinguishedName());
		assertEquals(testAnchor.getSerialNumber().toString(16),  retAnchor.getSerial());
		assertEquals(Thumbprint.toThumbprint(testAnchor).toString(), retAnchor.getThumbprint());
		assertEquals(testAnchor.getNotAfter().toInstant(), retAnchor.getValidEndDate());
		assertEquals(testAnchor.getNotBefore().toInstant(), retAnchor.getValidStartDate());
	}
	
	@Test(expected=TrustAnchorNotFoundException.class)
	public void testGetAnchorByThumbprint_anchorNotExists_assertException() throws Exception
	{
		trustAnchorProv.getAnchorByThumbprint("12345");
	}
}
