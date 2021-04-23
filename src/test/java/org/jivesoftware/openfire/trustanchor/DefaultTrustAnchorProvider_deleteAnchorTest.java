package org.jivesoftware.openfire.trustanchor;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustAnchorProvider_deleteAnchorTest extends SpringDataBaseTest
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
	public void testDeleteTrustAnchor_anchorExists_assertDeleted() throws Exception
	{
		trustAnchorProv.deleteTrustAnchor(Thumbprint.toThumbprint(testAnchor).toString());
		
		assertEquals(0, trustAnchorProv.getAnchors().size());
	}
	
	@Test
	public void testDeleteTrustAnchors_anchorNotExists_assertNotDeleted() throws Exception
	{
		trustAnchorProv.deleteTrustAnchor("12345");
		
		assertEquals(1, trustAnchorProv.getAnchors().size());
	}
}
