package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class TrustBundleManager_deleteTrustBundleTest extends SpringDataBaseTest
{
	protected TrustBundleManager mgr;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		mgr = TrustBundleManager.getInstance();
		
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("JUnit Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		bundle.setRefreshInterval(24);

		mgr.addTrustBundle(bundle);
	}
	
	@Test
	public void testDeleteTrustBundle_bundleExists_assertDeleted() throws Exception
	{
		mgr.deleteTrustBundle("JUnit Bundle");
		
		assertEquals(0, mgr.getTrustBundles(false).size());
	}
	
	@Test
	public void testDeleteTrustBundle_assertExistsWithCircles_assertDeletedAndCirclesCleaned() throws Exception
	{
		trustCircleProv.addTrustCircle("TestCircle", null);
		
		trustCircleProv.addTrustBundleToCircle("TestCircle", "JUnit Bundle");
		
		assertEquals(1, trustCircleProv.getTrustCircle("TestCircle", true, false).getTrustBundles().size());
		
		mgr.deleteTrustBundle("JUnit Bundle");
		
		assertEquals(0, mgr.getTrustBundles(false).size());
		
		assertEquals(0, trustCircleProv.getTrustCircle("TestCircle", true, false).getTrustBundles().size());
	}
}
