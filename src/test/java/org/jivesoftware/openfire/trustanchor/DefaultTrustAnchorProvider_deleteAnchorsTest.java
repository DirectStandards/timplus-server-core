package org.jivesoftware.openfire.trustanchor;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustAnchorProvider_deleteAnchorsTest extends SpringDataBaseTest
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
	public void testDeleteTrustAnchors_anchorExists_assertDeleted() throws Exception
	{
		trustAnchorProv.deleteTrustAnchors(Collections.singleton(Thumbprint.toThumbprint(testAnchor).toString()));
		
		assertEquals(0, trustAnchorProv.getAnchors().size());
	}
	
	@Test
	public void testDeleteTrustAnchors_anchorNotExists_assertNotDeleted() throws Exception
	{
		trustAnchorProv.deleteTrustAnchors(Collections.singleton("12345"));
		
		assertEquals(1, trustAnchorProv.getAnchors().size());
	}
	
	@Test
	public void testDeleteTrustAnchors_emptyList_assertNotDeleted() throws Exception
	{
		trustAnchorProv.deleteTrustAnchors(Collections.emptyList());
		
		assertEquals(1, trustAnchorProv.getAnchors().size());
	}
	
	@Test
	public void testDeleteTrustAnchors_multipleThumbprints_assertDeleted() throws Exception
	{
		final Collection<String> tps = new ArrayList<>();
		tps.add(Thumbprint.toThumbprint(testAnchor).toString());
		tps.add("12345");
		
		trustAnchorProv.deleteTrustAnchors(tps);
		
		assertEquals(0, trustAnchorProv.getAnchors().size());
	}
}
