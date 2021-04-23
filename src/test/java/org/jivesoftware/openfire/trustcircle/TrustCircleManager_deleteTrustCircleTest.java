package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class TrustCircleManager_deleteTrustCircleTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
			
		prov.createDomain("TestDomain", true);
		
		trustCircleProv.addCirclesToDomain("TestDomain", Collections.singleton("TestCircle"));
	}
	
	@Test
	public void testDeleteTrustCircle_circleHasBundlesAnchorsAndDomains_assertDeleted() throws Exception
	{
		TrustCircleManager.getInstance().deleteTrustCircle("TestCircle");
		
		assertEquals(0, trustCircleProv.getCirclesByDomain("TestDomain", false, false).size());
	}
}
