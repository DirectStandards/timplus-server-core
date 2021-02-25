package org.jivesoftware.openfire.cluster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultClusterNodeProvider implements ClusterNodeProvider
{
	private static final Logger Log = LoggerFactory.getLogger(DefaultClusterNodeProvider.class);
	
	private static final String LOAD_CLUSTER_MEMBERS = "SELECT * FROM ofClusterMember WHERE nodeStatus=0 or nodeStatus=1";

	private static final String LOAD_NON_CLUSTER_MEMBERS = "SELECT * FROM ofClusterMember WHERE nodeStatus=2 or nodeStatus=3";
	
	private static final String LOAD_CLUSTER_MEMBER_BY_NODE = "SELECT * FROM ofClusterMember WHERE nodeId = ?";	
	
    private static final String INSERT_CLUSTER_MEMBER =
            "INSERT INTO ofClusterMember (nodeID,rawNodeID,nodeHost,nodeIP,nodeJoinedDtTm,nodeLeftDtTm,lastNodeHBDtTm,nodeStatus) " +
            "VALUES (?,?,?,?,?,?,?,?)"; 	
	
    private static final String UPDATE_CLUSTER_MEMBER = "UPDATE ofClusterMember SET nodeHost=?, nodeIP=?, nodeJoinedDtTm=?, " +
            "nodeLeftDtTm=?, lastNodeHBDtTm=?, nodeStatus=? WHERE nodeId=?";   
    
    private static final String HEARTBEAT_CLUSTER_MEMBER = "UPDATE ofClusterMember SET lastNodeHBDtTm=? WHERE nodeId=?";   
    
    private static final String DELETE_CLUSTER_MEMBER = "DELETE FROM ofClusterMember WHERE nodeId = ?";
    
	@Override
	public Collection<ClusterNode> getClusterMembers() throws ClusterException 
	{
		return getCluster(LOAD_CLUSTER_MEMBERS);
	}

	public Collection<ClusterNode> getNonClusterMembers() throws ClusterException
	{
		return getCluster(LOAD_NON_CLUSTER_MEMBERS);
	}
	
	protected Collection<ClusterNode> getCluster(String query) throws ClusterException 
	{
		   final List<ClusterNode> nodes = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(query);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            while (rs.next()) 
	            {
	            	final ClusterNode node = clusterNodeFromResultSet(rs);
	            	nodes.add(node);
	            	
	            }
		   }
	       catch (SQLException e) 
		   {
	    	   throw new ClusterException("Failed to load cluster nodes.", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return nodes;
	}
	
	@Override
	public ClusterNode getClusterMember(NodeID node) throws ClusterException 
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CLUSTER_MEMBER_BY_NODE);
            pstmt.setString(1, node.toString());
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new ClusterNodeNotFoundException("Could not find cluster member with id " + node.toString());
            }

            final ClusterNode retNode = clusterNodeFromResultSet(rs);
            
            return retNode;
        }
        catch (Exception e) 
        {
            throw new ClusterNodeNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public ClusterNode addClusterMember(ClusterNode clusterNode) throws ClusterException 
	{
		if (this.isExistingClusterNode(clusterNode.getNodeId()))
			throw new ClusterNodeAlreadyExistsException("Cluster node " + clusterNode.getNodeId() + "  already exists.");
				
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            pstmt = con.prepareStatement(INSERT_CLUSTER_MEMBER);
            pstmt.setString(1, clusterNode.getNodeId().toString());
            pstmt.setBytes(2, clusterNode.getNodeId().toByteArray());
            pstmt.setString(3, clusterNode.getNodeHost());
            pstmt.setString(4, clusterNode.getNodeIP());
            pstmt.setLong(5, clusterNode.getNodeJoinedDtTm().toEpochMilli());
            
            if (clusterNode.getNodeLeftDtTm() != null)
                pstmt.setLong(6, clusterNode.getNodeLeftDtTm().toEpochMilli());
            else
            	pstmt.setLong(6, 0);
            
            if (clusterNode.getLastNodeHBDtTm() != null)
                pstmt.setLong(7, clusterNode.getLastNodeHBDtTm().toEpochMilli());  
            else
            	pstmt.setLong(7, 0);            
           
            pstmt.setInt(8, clusterNode.getNodeStatus().getCode());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new ClusterException("Failed to insert cluster node.", e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return clusterNode;	
		
	}

	@Override
	public ClusterNode updateClusterMember(ClusterNode clusterNode) throws ClusterException 
	{
		// throws an exception if the node doesn't exist
		getClusterMember(clusterNode.getNodeId());
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_CLUSTER_MEMBER);

            pstmt.setString(1, clusterNode.getNodeHost());
            pstmt.setString(2, clusterNode.getNodeIP());
            pstmt.setLong(3, clusterNode.getNodeJoinedDtTm().toEpochMilli());
            
            if (clusterNode.getNodeLeftDtTm() != null)
            	pstmt.setLong(4, clusterNode.getNodeLeftDtTm().toEpochMilli());
            else
            	pstmt.setLong(4, 0);
            
            if (clusterNode.getLastNodeHBDtTm() != null)
            	pstmt.setLong(5, clusterNode.getLastNodeHBDtTm().toEpochMilli());
            else
            	pstmt.setLong(5, 0);
            
            pstmt.setInt(6, clusterNode.getNodeStatus().getCode());
            
            pstmt.setString(7, clusterNode.getNodeId().toString());
            
            pstmt.executeUpdate();
            
            return clusterNode;
        }
        catch (SQLException sqle) 
        {
            throw new ClusterException(sqle);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
		
	}

	@Override
	public ClusterNode heartBeatClusterMemeber(NodeID node) throws ClusterException
	{
		final ClusterNode clusterNode = getClusterMember(node);
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(HEARTBEAT_CLUSTER_MEMBER);

            clusterNode.setLastNodeHBDtTm(Instant.now());
            
            pstmt.setLong(1, clusterNode.getLastNodeHBDtTm().toEpochMilli());
            pstmt.setString(2, clusterNode.getNodeId().toString());
            
            pstmt.executeUpdate();
            
            return clusterNode;
            
        }
        catch (SQLException sqle) 
        {
            throw new ClusterException(sqle);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }		
	}
	
	@Override
	public void purgeClusterMemeber(NodeID node) throws ClusterException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_CLUSTER_MEMBER);
            pstmt.setString(1, node.toString());
            pstmt.execute();
        }
        catch (Exception e) 
        {
            Log.error(e.getMessage(), e);
            abortTransaction = true;
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }		
	}
	
	@Override
	public void purgeClusterMemebers(long inactiveDateThreshold) throws ClusterException 
	{
		// TODO Auto-generated method stub
		
	}
	
	protected ClusterNode clusterNodeFromResultSet(final ResultSet rs) throws SQLException
	{
		final ClusterNode node = new ClusterNode();

		final NodeID nodeId = NodeID.getInstance(rs.getBytes(2));
		node.setNodeId(nodeId);
		node.setNodeHost(rs.getString(3));
		node.setNodeIP(rs.getString(4));
		
		if (rs.getLong(5) != 0)
			node.setNodeJoinedDtTm(Instant.ofEpochMilli(rs.getLong(5)));
		
		if (rs.getLong(6) != 0)
			node.setNodeLeftDtTm(Instant.ofEpochMilli(rs.getLong(6)));
		
		if (rs.getLong(7) != 0)
			node.setLastNodeHBDtTm(Instant.ofEpochMilli(rs.getLong(7)));
		
		node.setNodeStatus(ClusterNodeStatus.fromCode(rs.getInt(8)));

		return node;		
	}	
	
	protected boolean isExistingClusterNode(NodeID nodeId)
	{
		try
		{
			return this.getClusterMember(nodeId) != null;
		}
		catch(ClusterException e)
		{
			return false;
		}
	}	

}
