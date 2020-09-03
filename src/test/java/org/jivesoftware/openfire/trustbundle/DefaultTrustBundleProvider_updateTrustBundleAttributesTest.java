package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_updateTrustBundleAttributesTest extends SpringDataBaseTest
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
	public void testUpdateBundleAttributes_bundleExist_assertUpdated() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/signedBundle.p7b");
		
		final TrustBundle updateBundle = trustBundleProv.getTrustBundle("JUnit Bundle");
		updateBundle.setBundleName("Junit Bundle 2");
		updateBundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		
		trustBundleProv.updateTrustBundleAttributes("JUnit Bundle", updateBundle);
		
		final TrustBundle bundle = trustBundleProv.getTrustBundle("Junit Bundle 2");
		
		assertEquals("Junit Bundle 2", bundle.getBundleName());
		assertEquals(filePrefix + bundleLocation.getAbsolutePath(), bundle.getBundleURL());
		assertNull(bundle.getCheckSum());
		assertNotNull(bundle.getCreateTime());
		assertNull(bundle.getLastRefreshAttempt());
		assertNull(bundle.getLastRefreshError());
		assertNull(bundle.getLastSuccessfulRefresh());
		assertEquals(24, bundle.getRefreshInterval());
		assertNull(bundle.getSigningCertificateData());
	}
	
	@Test(expected=TrustBundleNotFoundException.class)
	public void testUpdateBundleAttributes_bundleNotExist_assertException() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/signedBundle.p7b");
		
		final TrustBundle updateBundle = trustBundleProv.getTrustBundle("JUnit Bundle");
		updateBundle.setBundleName("Junit Bundle 2");
		updateBundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		
		trustBundleProv.updateTrustBundleAttributes("JUnit Bundle999", updateBundle);
	}
}
