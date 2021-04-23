package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_deleteTrustBundlesTest extends SpringDataBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("JUnit Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		bundle.setRefreshInterval(24);

		trustBundleProv.addTrustBundle(bundle);
	}
	
	@Test
	public void testDeleteTrustBundle_bundleExists_assertDeleted() throws Exception
	{
		trustBundleProv.deleteTrustBundle("JUnit Bundle");
		
		assertEquals(0, trustBundleProv.getTrustBundles(true).size());
	}
}
