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

public class DefaultTrustAnchorProvider_getAnchorsIdsTest extends SpringDataBaseTest
{
	protected X509Certificate testAnchor;
	
	protected TrustAnchor addedAnchor;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		testAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		addedAnchor = trustAnchorProv.addTrustAnchor(testAnchor);
	}
	
	@Test
	public void testGetAnchorsByIds_idExists_assertRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByIds(Collections.singleton(addedAnchor.getId()));
		
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
	public void testGetAnchorsByIds_idNotExists_assertNotRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByIds(Collections.singleton("1234"));
		
		assertEquals(0, anchors.size());
	}
	
	@Test
	public void testGetAnchorsByIds_emptyIds_assertNotRetrieved() throws Exception
	{
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByIds(Collections.emptyList());
		
		assertEquals(0, anchors.size());
	}
	
	@Test
	public void testGetAnchorsByIds_multipleIdss_assertRetrieved() throws Exception
	{
		final Collection<String> ids = new ArrayList<>();
		ids.add(addedAnchor.getId());
		ids.add("12345");
		
		final Collection<TrustAnchor> anchors = trustAnchorProv.getAnchorsByIds(ids);
		
		assertEquals(1, anchors.size());
	}	
}
