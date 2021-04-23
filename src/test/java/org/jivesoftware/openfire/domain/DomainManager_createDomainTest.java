package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jivesoftware.SpringDataBaseTest;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.junit.Before;
import org.junit.Test;

public class DomainManager_createDomainTest extends SpringDataBaseTest
{
	protected DomainManager domainManager;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		XMPPServer mockServer = mock(XMPPServer.class);
		
		final MultiUserChatManager chatManager = mock(MultiUserChatManager.class);
		when(mockServer.getMultiUserChatManager()).thenReturn(chatManager);
		
		domainManager = DomainManager.getInstance();
		
		domainManager.setXMPPServer(mockServer);
		
		prov.createDomain("domain.com", true);
		prov.createDomain("healthcare.com", false);
	}
	
	@Test
	public void testCreateDomain_newDomain_assertCreated() throws Exception
	{
		domainManager.createDomain("domain2.com", true);
		
		final Domain dom = domainManager.getDomain("domain2.com");
		
		assertEquals("domain2.com", dom.getDomainName());
		assertTrue(dom.isEnabled());
		assertNotNull(dom.getCreationDate());
		assertNotNull(dom.getModificationDate());
	}
	
	@Test
	public void testCreateDomain_existingDomain_assertException() throws Exception
	{
		domainManager.createDomain("domain2.com", true);
		
		final Domain dom = domainManager.getDomain("domain2.com");
		
		assertEquals("domain2.com", dom.getDomainName());
		assertTrue(dom.isEnabled());
		assertNotNull(dom.getCreationDate());
		assertNotNull(dom.getModificationDate());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateDomain_emptyDomainName_assertException() throws Exception
	{
		domainManager.createDomain("", true);
	}
}
