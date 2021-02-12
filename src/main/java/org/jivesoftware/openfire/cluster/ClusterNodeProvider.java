package org.jivesoftware.openfire.cluster;

import java.util.Collection;

public interface ClusterNodeProvider 
{
	public Collection<ClusterNode> getClusterMembers() throws ClusterException;
	
	public ClusterNode getClusterMember(NodeID node) throws ClusterException;
	
	public ClusterNode addClusterMember(ClusterNode clusterNode) throws ClusterException;

	public ClusterNode updateClusterMember(ClusterNode clusterNode) throws ClusterException;	
	
	public ClusterNode heartBeatClusterMemeber(NodeID node) throws ClusterException;	
	
	public void purgeClusterMemebers(long inactiveDateThreshold) throws ClusterException;	
}
	

