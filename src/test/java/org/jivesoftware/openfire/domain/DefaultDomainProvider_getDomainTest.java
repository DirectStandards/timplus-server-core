package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_getDomainTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain.com", true);
	}
	
	
	
	@Test(expected=DomainNotFoundException.class)
	public void testGetDomainByName_noDomains_assertException() throws Exception
	{
		prov.getDomain("test.com");
	}
	
	@Test()
	public void testGetDomainByName_domainExists_assertDomainRetrieved() throws Exception
	{
		final Domain dom = prov.getDomain("domain.com");
		
		assertEquals("domain.com", dom.getDomainName());
		assertTrue(dom.isEnabled());
		assertNotNull(dom.getCreationDate());
		assertNotNull(dom.getModificationDate());
	}
}
