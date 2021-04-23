package org.jivesoftware.util.cache;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.openfire.muc.spi.MultiUserChatServiceImpl;
import org.jivesoftware.openfire.roster.Roster;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.roster.RosterItem.SubType;
import org.jivesoftware.openfire.roster.RosterItem.AskType;
import org.jivesoftware.openfire.roster.RosterItem.RecvType;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;

public class DefaultExternalizableUtilStrategy_cacheSerializationTest 
{
	@Before
	public void setUp()
	{
		ExternalizableUtil.getInstance().setStrategy(new DefaultExternalizableUtilStrategy());
	}
	
	@Test
	public void testSerializedDeserializeRoster() throws Exception
	{
		final List<String> groups = new LinkedList<>();
		groups.add("TestGroup");
		
		final RosterItem rosterItem = new RosterItem(12345, new JID("ah4626", "test.com", null),
				SubType.BOTH, AskType.SUBSCRIBE, RecvType.SUBSCRIBE, "nickname", groups);
		
		final ConcurrentMap<String, RosterItem> rosterItems = new ConcurrentHashMap<>();
		rosterItems.put("ah4626@test.com", rosterItem);
		
		
		final Roster roster = new Roster();
		roster.setUsername("gm2552");
		roster.setDomain("test.com");
		roster.setRosterItems(rosterItems);
		
		final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		final ObjectOutputStream obOutStream = new ObjectOutputStream(outStream);
		
		roster.writeExternal(obOutStream);
		obOutStream.flush();
		
		byte[] bytes = outStream.toByteArray();
		
		final ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
		final ObjectInputStream obInStream = new ObjectInputStream(inStream);
		
		final Roster readRoster = new Roster();
		
		readRoster.readExternal(obInStream);
		
		assertEquals("gm2552", readRoster.getUsername());
		final Collection<RosterItem> readRosterItems = readRoster.getRosterItems();
		assertEquals(1, readRosterItems.size());
		
		final RosterItem readRosterItem = readRosterItems.iterator().next();
		assertEquals(rosterItem.getID(), readRosterItem.getID());
		assertEquals(rosterItem.getJid(), readRosterItem.getJid());
		assertEquals(rosterItem.getAskStatus(), readRosterItem.getAskStatus());
		assertEquals(rosterItem.getSubStatus(), readRosterItem.getSubStatus());
		assertEquals(rosterItem.getRecvStatus(), readRosterItem.getRecvStatus());
		assertEquals(rosterItem.getNickname(), readRosterItem.getNickname());
		
		assertEquals(1, readRosterItem.getGroups().size());
		assertEquals("TestGroup", readRosterItem.getGroups().get(0));
	}
}
