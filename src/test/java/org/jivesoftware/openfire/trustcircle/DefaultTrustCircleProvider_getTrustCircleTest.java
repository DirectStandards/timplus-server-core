package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.junit.Test;

public class DefaultTrustCircleProvider_getTrustCircleTest extends TrustCircleBaseTest
{
	@Test
	public void testGetTrustCircle_cirecleExists_noLoadAnchors_assertRetrieved() throws Exception
	{
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", false, false);
		
		assertEquals("TestCircle", circle.getName());
		assertNotNull(circle.getCreationDate());
		assertEquals(0, circle.getAnchors().size());
		assertEquals(0, circle.getTrustBundles().size());
	}
	
	@Test
	public void testGetTrustCircle_cirecleExists_loadAnchors_assertRetrieved() throws Exception
	{
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", true, true);
		
		assertEquals("TestCircle", circle.getName());
		assertNotNull(circle.getCreationDate());
		assertEquals(1, circle.getAnchors().size());
		assertEquals(1, circle.getTrustBundles().size());
		
		final TrustAnchor anchor = circle.getAnchors().iterator().next();
		assertEquals(testAnchor, anchor.asX509Certificate());
		
		final TrustBundle bundle = circle.getTrustBundles().iterator().next();
		
		assertEquals("JUnit Bundle", bundle.getBundleName());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testGetTrustCircle_cirecleNotExists_assertException() throws Exception
	{
		trustCircleProv.getTrustCircle("TestCircle2", true, true);
	}
}
