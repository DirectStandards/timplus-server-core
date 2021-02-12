package org.jivesoftware.openfire;

public enum PacketRouteStatus 
{
	ROUTED(true),
	
	ROUTED_TO_CLUSTER(true),
	
	ROUTE_FAILED(false);
	
	protected final boolean routingSuccessful;
	
	private PacketRouteStatus(boolean routingSuccessful)
	{
		this.routingSuccessful = routingSuccessful;
	}
	
	public boolean isRoutingSuccessful()
	{
		return routingSuccessful;
	}
}
