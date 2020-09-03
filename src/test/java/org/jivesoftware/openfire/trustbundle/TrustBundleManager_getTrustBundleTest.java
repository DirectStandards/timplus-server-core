package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Collection;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class TrustBundleManager_getTrustBundleTest extends SpringDataBaseTest
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
	public void testGetTrustBundle_nameExists_assertRetrieved() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = mgr.getTrustBundle("JUnit Bundle");
				
		final Collection<TrustBundleAnchor> anchors = bundle.getTrustBundleAnchors();
		assertEquals(6, anchors.size());
		for (TrustBundleAnchor anchor : anchors)
		{
			assertNotNull(anchor.getAnchorData());
		}
		
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
	public void testGetTrustBundle_nameNotExists_assertExecption() throws Exception
	{
		mgr.getTrustBundle("JUnit Bundle2");
	}
}
