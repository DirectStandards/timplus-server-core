package org.jivesoftware.openfire.cluster;


public enum ClusterNodeStatus 
{
	/**
	 * Indicates the node is an active part of the cluster
	 */
	NODE_JOINED(0),
	
	/**
	 * Indicates the node is the master node of the cluster
	 */
	NODE_MASTER(1),
	
	/**
	 * Indicates the node left the cluster on its own accord
	 */
	NODE_LEFT(2),
	
	/**
	 * Indicates the node was evicted from the cluster due to lack of reponse
	 */
	NODE_EVICTED(3);
	
	
	private final int code;

    private ClusterNodeStatus(int code) 
    {
        this.code = code;
    }

    public int getCode() 
    {
        return code;
    }
    
    public static ClusterNodeStatus fromCode(int code)
    {
    	switch (code) 
    	{
		    case 0:
			    return NODE_JOINED; 	
    		case 1:
    			return NODE_MASTER;
    		case 2:
    			return NODE_LEFT;    			
    		case 3:
    			return NODE_EVICTED; 
    		default:
    			return NODE_JOINED;
    			
    	}
    		
    }
}
