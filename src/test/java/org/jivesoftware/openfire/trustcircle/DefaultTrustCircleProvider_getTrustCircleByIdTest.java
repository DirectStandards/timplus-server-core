package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.junit.Test;

public class DefaultTrustCircleProvider_getTrustCircleByIdTest extends TrustCircleBaseTest
{
	@Test
	public void testGetTrustCircle_idExists_noLoadAnchors_assertRetrieved() throws Exception
	{
		final TrustCircle circle = trustCircleProv.getTrustCircleById(testCircle.getId(), false, false);
		
		assertEquals("TestCircle", circle.getName());
		assertNotNull(circle.getCreationDate());
		assertEquals(0, circle.getAnchors().size());
		assertEquals(0, circle.getTrustBundles().size());
	}
	
	@Test
	public void testGetTrustCircle_idExists_loadAnchors_assertRetrieved() throws Exception
	{
		final TrustCircle circle = trustCircleProv.getTrustCircleById(testCircle.getId(), true, true);
		
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
	public void testGetTrustCircle_idNotExists_assertException() throws Exception
	{
		trustCircleProv.getTrustCircleById("12345", true, true);
	}
}
