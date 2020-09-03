package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_getTrustBundlesTest extends SpringDataBaseTest
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
	public void testGetTrustBundles_bundlesExist_assertRetrieved() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final Collection<TrustBundle> bundles = trustBundleProv.getTrustBundles(true);
		
		assertEquals(1, bundles.size());
		
		final TrustBundle bundle = bundles.iterator().next();
		
		final Collection<TrustBundleAnchor> anchors = bundle.getTrustBundleAnchors();
		assertEquals(0, anchors.size());
		
		assertEquals("JUnit Bundle", bundle.getBundleName());
		assertEquals(filePrefix + bundleLocation.getAbsolutePath(), bundle.getBundleURL());
		assertNull(bundle.getCheckSum());
		assertNotNull(bundle.getCreateTime());
		assertNull(bundle.getLastRefreshAttempt());
		assertNull(bundle.getLastRefreshError());
		assertNull(bundle.getLastSuccessfulRefresh());
		assertEquals(24, bundle.getRefreshInterval());
		assertNull(bundle.getSigningCertificateData());
	}
}
