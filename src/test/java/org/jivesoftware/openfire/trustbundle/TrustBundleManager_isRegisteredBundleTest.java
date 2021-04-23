package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class TrustBundleManager_isRegisteredBundleTest extends SpringDataBaseTest
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
	public void testIsRegisteredBundle_bundlesExist_assertTrue() throws Exception
	{
		assertTrue(mgr.isRegisteredTrustBundle("JUnit Bundle"));
	}
	
	@Test
	public void testIsRegisteredBundle_bundlesNotExist_assertFalse() throws Exception
	{
		assertFalse(mgr.isRegisteredTrustBundle("JUnit Bundle2"));
	}
}
