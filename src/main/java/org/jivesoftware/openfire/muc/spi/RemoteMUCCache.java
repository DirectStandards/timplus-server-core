package org.jivesoftware.openfire.muc.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.domain.DomainManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketExtension;
import org.xmpp.packet.Presence;

/**
 * Simple implementation to cache nick names of users from group chats hosted in remote servers.  Because
 * the mapping of a nickname to a real JID is handled in the group chat presence message, it is more efficient
 * to pick off presence messages then query a server for room information.  This can also lead to cache coherence issues,
 * so care must be taken in the cache.
 */
public class RemoteMUCCache implements PacketInterceptor
{
	public final static String MUC_NICK_JID_CACHE_NAME = "MUC Nick JID Map Cache";
	
	public final static String MUC_OCCUPANT_CACHE_NAME = "MUC Occupant Map Cache";	
	
	static RemoteMUCCache INSTANCE = null;
	
	static
	{
		INSTANCE = new RemoteMUCCache();
	}
	
	protected Cache<String, String> groupNickJIDMap;
	
	protected Cache<String, Map<String, String>> roomOccupantsMap;
	
	public static RemoteMUCCache getInstance()
	{
		return INSTANCE;
	}
	
	private RemoteMUCCache()
	{
		super();
		
		groupNickJIDMap = CacheFactory.createCache(MUC_NICK_JID_CACHE_NAME, false);
		roomOccupantsMap = CacheFactory.createCache(MUC_OCCUPANT_CACHE_NAME, false);
	}

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException
	{
		// We are looking for presence packets.
		if (packet instanceof Presence)
		{
			final Presence pres = Presence.class.cast(packet);
			
			final Presence.Type presType = pres.getType();
			
			// Intercept incoming presence messages from other servers but that have not yet
			// been delivered to the end points.  Regardless if the packet makes it to the end point,
			// an occupant has either entered of left the room.
			if (incoming && !processed)
			{
				if (presType == null || presType == Presence.Type.unavailable)
				{
					// check if the domain is remote... we only need to cache remote users
					if (!(DomainManager.getInstance().isRegisteredDomain(pres.getFrom().getDomain()) 
							|| DomainManager.getInstance().isRegisteredComponentDomain(pres.getFrom().getDomain())))
					{
						// check if this is an MUC event
						final PacketExtension mucPres = pres.getExtension(MUCInitialPresence.ELEMENT, MUCInitialPresence.NAMESPACE + "#user");
						if (presType == null && mucPres != null)
						{
							final Element itemEl = mucPres.getElement().element("item");
							if (itemEl != null)
							{
								// this is a presence entrance
								// add/update the cache
								synchronized(groupNickJIDMap)
								{
									groupNickJIDMap.put(packet.getFrom().toString(), new JID(itemEl.attributeValue("jid")).toString());
									
									// Bare JID of the from address will be the room
									Map<String, String> roomOccupants = roomOccupantsMap.get(packet.getFrom().asBareJID().toString());
									if (roomOccupants == null)
										roomOccupants = new HashMap<>();
										
									roomOccupants.put(packet.getFrom().toString(), new JID(itemEl.attributeValue("jid")).toString());
									
									roomOccupantsMap.put(packet.getFrom().asBareJID().toString(), roomOccupants);
								}
							}
						}
						// Handle all unavailable messages as we don't know if 
						// a group chat will have "x" extension or not.
						else if (presType == Presence.Type.unavailable)
						{
							synchronized(groupNickJIDMap)
							{
								groupNickJIDMap.remove(packet.getFrom());
								
								final Map<String, String> roomOccupants = roomOccupantsMap.get(packet.getFrom().asBareJID().toString());
								if (roomOccupants != null)
								{
									// if the room is empty, then remove the room from our map
									roomOccupants.remove(packet.getFrom().toString());
									if (roomOccupants.isEmpty())
										roomOccupantsMap.remove(packet.getFrom().asBareJID().toString());
								}
								
							}
						}
					}
				}
			}
		}
	}
	
	public JID getRemoteNickNameJID(JID nickNameJID)
	{
		JID retVal = null;
		synchronized(groupNickJIDMap)
		{
			final String jidString = groupNickJIDMap.get(nickNameJID.toString());
			
			if (!StringUtils.isEmpty(jidString))
				retVal =  new JID(jidString, true);
		}
		
		return retVal;
	}
	
	public Map<JID,JID> getRemoteRoomOccupants(JID roomJID)
	{
		final Map<JID,JID> retVal = new HashMap<>();;
		synchronized(groupNickJIDMap)
		{
			final Map<String, String> lookup =  roomOccupantsMap.get(roomJID.toString());
			if (lookup != null)
			{
				lookup.forEach((k,v) -> retVal.put(new JID( k, true), new JID(v, true)));
			}
		}
		
		return (retVal.isEmpty()) ? Collections.emptyMap() : retVal;
	}
}
