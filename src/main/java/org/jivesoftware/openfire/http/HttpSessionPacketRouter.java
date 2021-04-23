package org.jivesoftware.openfire.http;

import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.SessionPacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

public class HttpSessionPacketRouter extends SessionPacketRouter
{
	protected String streamName;
	
	protected PacketRouter router;
	
	public HttpSessionPacketRouter(HttpSession session)
	{
		super(session);
		
		router = XMPPServer.getInstance().getPacketRouter();
		
		streamName = "";
	}
	
	public void setStreamName(String streamName)
	{
		this.streamName = streamName;
	}
	
    @Override
    public void route(Packet packet) {
        // Security: Don't allow users to send packets on behalf of other users
        packet.setFrom(((HttpSession)session).getAddress(streamName));
        if(packet instanceof IQ) {
            route((IQ)packet);
        }
        else if(packet instanceof Message) {
            route((Message)packet);
        }
        else if(packet instanceof Presence) {
            route((Presence)packet);
        }
    }

    @Override
    public void route(IQ packet) {
        packet.setFrom(((HttpSession)session).getAddress(streamName));
        router.route(packet);
        session.incrementClientPacketCount();
    }

    @Override
    public void route(Message packet) {
        packet.setFrom(((HttpSession)session).getAddress(streamName));
        router.route(packet);
        session.incrementClientPacketCount();
    }

    @Override
    public void route(Presence packet) {
        packet.setFrom(((HttpSession)session).getAddress(streamName));
        router.route(packet);
        session.incrementClientPacketCount();
    }
}
