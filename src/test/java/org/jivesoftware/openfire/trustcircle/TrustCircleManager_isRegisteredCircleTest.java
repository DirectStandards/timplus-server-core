package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrustCircleManager_isRegisteredCircleTest extends TrustCircleBaseTest
{
	@Test
	public void testIsRegisteredCircle_circleExists_assertTrue()
	{
		assertTrue(TrustCircleManager.getInstance().isRegisteredTrustCircle("TestCircle"));
	}
	
	@Test
	public void testIsRegisteredCircle_circleNotExists_assertFalse()
	{
		assertFalse(TrustCircleManager.getInstance().isRegisteredTrustCircle("TestCircle2"));
	}
}
