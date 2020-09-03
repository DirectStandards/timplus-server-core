package org.jivesoftware.openfire.trustbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustBundleProvider_getTrustBundleByIdsTest extends SpringDataBaseTest
{
	protected TrustBundle addedBundle;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("JUnit Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		bundle.setRefreshInterval(24);

		addedBundle = trustBundleProv.addTrustBundle(bundle);
	}
	
	@Test
	public void testGetTrustBundlesById_idExists_assertsRetrieved() throws Exception
	{
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final Collection<TrustBundle> bundles = trustBundleProv.getTrustBundlesByIds(Collections.singleton(addedBundle.getId()), true);
		
		assertEquals(1, bundles.size());
		
		final TrustBundle bundle = bundles.iterator().next();
		
		final Collection<TrustBundleAnchor> anchors = bundle.getTrustBundleAnchors();
		
		assertEquals("JUnit Bundle", bundle.getBundleName());
		assertEquals(filePrefix + bundleLocation.getAbsolutePath(), bundle.getBundleURL());
		assertNull(bundle.getCheckSum());
		assertNotNull(bundle.getCreateTime());
		assertNull(bundle.getLastRefreshAttempt());
		assertNull(bundle.getLastRefreshError());
		assertNull(bundle.getLastSuccessfulRefresh());
		assertEquals(24, bundle.getRefreshInterval());
		assertNull(bundle.getSigningCertificateData());
		
		assertEquals(0, anchors.size());
	}
	
	@Test
	public void testGetTrustBundlesById_idNotExists_assertsNotRetrieved() throws Exception
	{	
		final Collection<TrustBundle> bundles = trustBundleProv.getTrustBundlesByIds(Collections.singleton("12345"), true);
		
		assertEquals(0, bundles.size());
	}	
	
	@Test
	public void testGetTrustBundlesById_emptyIds_assertsNotRetrieved() throws Exception
	{	
		final Collection<TrustBundle> bundles = trustBundleProv.getTrustBundlesByIds(Collections.emptyList(), true);
		
		assertEquals(0, bundles.size());
	}	
	
	@Test
	public void testGetTrustBundlesById_multipleIds_assertsRetrieved() throws Exception
	{	
		final Collection<String> ids = new ArrayList<>();
		ids.add(addedBundle.getId());
		ids.add("12345");
		
		final Collection<TrustBundle> bundles = trustBundleProv.getTrustBundlesByIds(ids, true);
		
		assertEquals(1, bundles.size());
	}
}
