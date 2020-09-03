package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.junit.Test;

public class DefaultTrustCircleProvider_getTrustCirclesTest extends TrustCircleBaseTest
{
	@Test
	public void getTestCircles_circleExists_noLoadAnchors_assertRetrieved() throws Exception
	{
		final Collection<TrustCircle> circles = trustCircleProv.getTrustCircles(false, false);
		
		assertEquals(1, circles.size());
		
		final TrustCircle circle = circles.iterator().next();
		
		assertEquals("TestCircle", circle.getName());
		assertNotNull(circle.getCreationDate());
		assertEquals(0, circle.getAnchors().size());
		assertEquals(0, circle.getTrustBundles().size());
	}
	
	@Test
	public void getTestCircles_circleExists_loadAnchors_assertRetrieved() throws Exception
	{
		final Collection<TrustCircle> circles = trustCircleProv.getTrustCircles(true, true);
		
		assertEquals(1, circles.size());
		
		final TrustCircle circle = circles.iterator().next();
		
		assertEquals("TestCircle", circle.getName());
		assertNotNull(circle.getCreationDate());
		assertEquals(1, circle.getAnchors().size());
		assertEquals(1, circle.getTrustBundles().size());
		
		final TrustAnchor anchor = circle.getAnchors().iterator().next();
		assertEquals(testAnchor, anchor.asX509Certificate());
		
		final TrustBundle bundle = circle.getTrustBundles().iterator().next();
		
		assertEquals("JUnit Bundle", bundle.getBundleName());
	}
}
