package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultTrustCircleProvider_deleteTrustCircleTest extends TrustCircleBaseTest
{
	@Test
	public void testDeleteTrustCircle_cirecleExists_assertDeleted() throws Exception
	{
		trustCircleProv.deleteCircle("TestCircle");
		
		assertEquals(0, trustCircleProv.getTrustCircles(false, false).size());
	}
}
