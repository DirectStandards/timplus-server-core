package org.jivesoftware.openfire.trustanchor;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustAnchorProvider_getAnchorsTest extends SpringDataBaseTest
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
	public void testGetAnchors_assertAnchorsRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchors();
		
		assertEquals(1, anchors.size());
		
		final TrustAnchor retAnchor = anchors.iterator().next();
		
		assertEquals(testAnchor, retAnchor.asX509Certificate());
		assertEquals(testAnchor.getSubjectDN().toString(), retAnchor.getDistinguishedName());
		assertEquals(testAnchor.getSerialNumber().toString(16),  retAnchor.getSerial());
		assertEquals(Thumbprint.toThumbprint(testAnchor).toString(), retAnchor.getThumbprint());
		assertEquals(testAnchor.getNotAfter().toInstant(), retAnchor.getValidEndDate());
		assertEquals(testAnchor.getNotBefore().toInstant(), retAnchor.getValidStartDate());
	}
}
