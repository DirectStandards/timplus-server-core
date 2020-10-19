package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class TrustBundleManager_updateBundleAttributesTest extends SpringDataBaseTest
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
	public void testUpdateBundleAttributes_bundleExist_assertUpdated() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/signedbundle.p7b");
		
		final TrustBundle updateBundle = trustBundleProv.getTrustBundle("JUnit Bundle");
		updateBundle.setBundleName("Junit Bundle 2");
		updateBundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		
		mgr.updateTrustBundleAttributes("JUnit Bundle", updateBundle, true);
		
		final TrustBundle bundle = trustBundleProv.getTrustBundle("Junit Bundle 2");
		
		assertEquals(1, bundle.getTrustBundleAnchors().size());
		
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
		
		mgr.updateTrustBundleAttributes("JUnit Bundle999", updateBundle, true);
	}
}
