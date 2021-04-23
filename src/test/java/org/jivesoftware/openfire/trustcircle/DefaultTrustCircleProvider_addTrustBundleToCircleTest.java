package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_addTrustBundleToCircleTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		final File bundleLocation = new File("./src/test/resources/bundles/signedBundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("JUnit Signed Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		bundle.setRefreshInterval(24);

		trustBundleProv.addTrustBundle(bundle);
	}
	
	@Test
	public void testAddTrustBundleToCircle_circleAndBundleExists_assertAdded() throws Exception
	{
		trustCircleProv.addTrustBundleToCircle("TestCircle", "JUnit Signed Bundle");
		
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", true, false);
		
		assertEquals(2, circle.getTrustBundles().size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testAddTrustBundleToCircle_circleNotExists_assertException() throws Exception
	{
		trustCircleProv.addTrustBundleToCircle("TestCircle2", "JUnit Signed Bundle");
	}
	
	@Test(expected=TrustCircleException.class)
	public void testAddTrustBundleToCircle_bundleNotExists_assertException() throws Exception
	{
		trustCircleProv.addTrustBundleToCircle("TestCircle", "Bogus Bundle");
	}
	
	@Test(expected=TrustCircleException.class)
	public void testAddTrustBundleToCircle_bundleAlreadyAdded_assertException() throws Exception
	{
		trustCircleProv.addTrustBundleToCircle("TestCircle", "JUnit Signed Bundle");
		
		trustCircleProv.addTrustBundleToCircle("TestCircle", "JUnit Signed Bundle");
	}
}
