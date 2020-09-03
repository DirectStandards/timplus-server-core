package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.jivesoftware.SpringDataBaseTest;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.junit.Before;
import org.junit.Test;

public class DomainManager_deleteDomainTest extends SpringDataBaseTest
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
	}
	
	@Test
	public void testDeleteDomain_domainExists_assertDeleted() throws Exception
	{
		domainManager.deleteDomain("domain.com");
		
		assertEquals(0, domainManager.getDomainCount());
	}
	
	@Test(expected=DomainNotFoundException.class)
	public void testDeleteDomain_domainNotExists_assertException() throws Exception
	{
		domainManager.deleteDomain("domain2.com");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeleteDomain_emptyDomainName_assertException() throws Exception
	{
		domainManager.deleteDomain("");
	}
	
	@Test
	public void testDeleteDomain_domainExistsWithCircles_assertDeletedAndTrustCirclesCleaned() throws Exception
	{
		trustCircleProv.addTrustCircle("TestCircle", null);
		
		trustCircleProv.addCirclesToDomain("domain.com", Collections.singleton("TestCircle"));
		
		assertEquals(1, trustCircleProv.getCirclesByDomain("domain.com", false, false).size());
		
		domainManager.deleteDomain("domain.com");
		
		assertEquals(0, domainManager.getDomainCount());
		
		assertEquals(0, trustCircleProv.getCirclesByDomain("domain.com", false, false).size());
	}
}
