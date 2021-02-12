package org.jivesoftware.openfire.cluster;

import java.time.Instant;

public class ClusterNode
{
	private String nodeId;
	
	private byte[] rawNodeId;
	
	private String nodeHost;
	
	private String nodeIP;
	
	private Instant nodeJoinedDtTm;
	
	private Instant nodeLeftDtTm;
	
	private Instant lastNodeHBDtTm;
	
	private ClusterNodeStatus nodeStatus;
	
    public ClusterNode()
    {
    	
    }

    
    
    public NodeID getNodeId() 
    {
		return NodeID.getInstance(rawNodeId);
	}



	public void setNodeId(NodeID nodeId) 
	{
		this.nodeId = nodeId.toString();
		this.rawNodeId = nodeId.toByteArray();
	}



	public String getNodeHost() 
	{
		return nodeHost;
	}



	public void setNodeHost(String nodeHost)
    {
		this.nodeHost = nodeHost;
	}



	public String getNodeIP() 
	{
		return nodeIP;
	}

	public void setNodeIP(String nodeIP) 
	{
		this.nodeIP = nodeIP;
	}


	public Instant getNodeJoinedDtTm() 
	{
		return nodeJoinedDtTm;
	}



	public void setNodeJoinedDtTm(Instant nodeJoinedDtTm) 
	{
		this.nodeJoinedDtTm = nodeJoinedDtTm;
	}



	public Instant getNodeLeftDtTm() 
	{
		return nodeLeftDtTm;
	}



	public void setNodeLeftDtTm(Instant nodeLeftDtTm) 
	{
		this.nodeLeftDtTm = nodeLeftDtTm;
	}



	public Instant getLastNodeHBDtTm() 
	{
		return lastNodeHBDtTm;
	}



	public void setLastNodeHBDtTm(Instant lastNodeHBDtTm) 
	{
		this.lastNodeHBDtTm = lastNodeHBDtTm;
	}



	public ClusterNodeStatus getNodeStatus() 
	{
		return nodeStatus;
	}



	public void setNodeStatus(ClusterNodeStatus nodeStatus) 
	{
		this.nodeStatus = nodeStatus;
	}

	@Override
    public String toString() 
    {
    	
        return nodeId;
    }

}
