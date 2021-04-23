package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_getTrustBundleTest extends SpringDataBaseTest
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
	public void testGetTrustBundle_nameExist_assertRetrieved() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = trustBundleProv.getTrustBundle("JUnit Bundle" );
		
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
	
	@Test(expected=TrustBundleNotFoundException.class)
	public void testGetTrustBundle_nameNotExist_assertException() throws Exception
	{
		trustBundleProv.getTrustBundle("JUnit Bundle2" );
	}
}
