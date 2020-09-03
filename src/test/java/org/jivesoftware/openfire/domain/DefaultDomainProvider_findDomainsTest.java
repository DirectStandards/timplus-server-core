package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_findDomainsTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain.com", true);
		prov.createDomain("healthcare.com", false);
	}

	@Test()
	public void testFindDomains_partialSearchAndEnabled_assertSingleDomainRetrieved() throws Exception
	{
		final Collection<Domain> doms = prov.findDomains("dom", true);
		
		assertEquals(1, doms.size());
		
		final Domain dom = doms.iterator().next();
		
		assertEquals("domain.com", dom.getDomainName());
		assertTrue(dom.isEnabled());
		assertNotNull(dom.getCreationDate());
		assertNotNull(dom.getModificationDate());
	}
	
	@Test()
	public void testFindDomains_partialSearchAndEnabled_assertNoneRetrieved() throws Exception
	{
		final Collection<Domain> doms = prov.findDomains("health", true);
		
		assertEquals(0, doms.size());
	}
	
	@Test()
	public void testFindDomains_partialSearchAllDomains_assertAllDomainsRetrieved() throws Exception
	{
		final Collection<Domain> doms = prov.findDomains(".com", false);
		
		assertEquals(2, doms.size());
	}
}
