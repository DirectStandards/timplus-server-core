package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_addTrustBundleTest extends SpringDataBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	@Test
	public void testAddTrustBundle_newBundle_assertAdded() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle addBundle = new TrustBundle();
		addBundle.setBundleName("JUnit Bundle");
		addBundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		addBundle.setRefreshInterval(24);

		trustBundleProv.addTrustBundle(addBundle);
		
		final TrustBundle bundle = trustBundleProv.getTrustBundle("Junit Bundle");
		
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
	
	@Test(expected=TrustBundleAlreadyExistsException.class)
	public void testAddTrustBundle_existingBundle_assertException() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle addBundle = new TrustBundle();
		addBundle.setBundleName("JUnit Bundle");
		addBundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		addBundle.setRefreshInterval(24);

		trustBundleProv.addTrustBundle(addBundle);
		
		trustBundleProv.addTrustBundle(addBundle);
	}
}
