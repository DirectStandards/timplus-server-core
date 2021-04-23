package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class DefaultTrustCircleProvider_deleteTrustBundleFromCircleTest extends TrustCircleBaseTest
{

	@Test
	public void deleteTrustBundleFromCircle_circleAndBundleExists_assertDeleted() throws Exception
	{
		trustCircleProv.deleteTrustBundlesFromCircle("TestCircle", Collections.singleton("JUnit Bundle"));
		
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", false, true);
		
		assertEquals(0, circle.getTrustBundles().size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void deleteTrustBundleFromCircle_circleNotExists_assertExeption() throws Exception
	{
		trustCircleProv.deleteTrustBundlesFromCircle("TestCircle2", Collections.singleton("JUnit Bundle"));
		
	}
}
