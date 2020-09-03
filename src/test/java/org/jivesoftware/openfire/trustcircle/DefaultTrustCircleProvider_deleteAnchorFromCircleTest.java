package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.junit.Test;

public class DefaultTrustCircleProvider_deleteAnchorFromCircleTest extends TrustCircleBaseTest
{

	@Test
	public void testDeleteAnchorFromCircle_circleAndAnchorExist_assertDeleted() throws Exception
	{
		trustCircleProv.deleteAnchorFromCircle("TestCircle", Thumbprint.toThumbprint(testAnchor).toString());
		
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", false, true);
		
		assertEquals(0, circle.getAnchors().size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testDeleteAnchorFromCircle_circleNotExist_assertExeption() throws Exception
	{
		trustCircleProv.deleteAnchorFromCircle("TestCircle2", Thumbprint.toThumbprint(testAnchor).toString());
	}
}
