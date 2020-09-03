package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_createDomainsTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain.com", true);
		prov.createDomain("healthcare.com", false);
	}
	
	@Test
	public void testCreateDomain_domainDoesNotExist_assertCreated() throws Exception
	{
		final Domain dom = prov.createDomain("domain2.com", true);
		
		assertEquals("domain2.com", dom.getDomainName());
		assertTrue(dom.isEnabled());
		assertNotNull(dom.getCreationDate());
		assertNotNull(dom.getModificationDate());
	}
	
	@Test(expected=DomainAlreadyExistsException.class)
	public void testCreateDomain_domainExists_assertException() throws Exception
	{
		prov.createDomain("domain.com", true);
	}
}
