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

public class DefaultTrustAnchorProvider_getAnchorsByThumbprintsTest extends SpringDataBaseTest
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
	public void testGetAnchorsByThumbprints_anchorExists_assertRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByThumbprints(Collections.singleton(Thumbprint.toThumbprint(testAnchor).toString()));
		
		assertEquals(1, anchors.size());
		
		final TrustAnchor retAnchor = anchors.iterator().next();
		
		assertEquals(testAnchor, retAnchor.asX509Certificate());
		assertEquals(testAnchor.getSubjectDN().toString(), retAnchor.getDistinguishedName());
		assertEquals(testAnchor.getSerialNumber().toString(16),  retAnchor.getSerial());
		assertEquals(Thumbprint.toThumbprint(testAnchor).toString(), retAnchor.getThumbprint());
		assertEquals(testAnchor.getNotAfter().toInstant(), retAnchor.getValidEndDate());
		assertEquals(testAnchor.getNotBefore().toInstant(), retAnchor.getValidStartDate());
	}
	
	@Test
	public void testGetAnchorsByThumbprints_anchorNotExists_assertNotRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByThumbprints(Collections.singleton("12345"));
		
		assertEquals(0, anchors.size());
	}
	
	@Test
	public void testGetAnchorsByThumbprints_multipleTPs_assertRetrieved() throws Exception
	{
		final Collection<String> tps = new ArrayList<>();
		tps.add(Thumbprint.toThumbprint(testAnchor).toString());
		tps.add("12345");
		
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByThumbprints(tps);
		
		assertEquals(1, anchors.size());
	}
	
	@Test
	public void testGetAnchorsByThumbprints_emptyTPs_assertNotRetrieved() throws Exception
	{
		
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByThumbprints(Collections.emptyList());
		
		assertEquals(0, anchors.size());
	}
}
