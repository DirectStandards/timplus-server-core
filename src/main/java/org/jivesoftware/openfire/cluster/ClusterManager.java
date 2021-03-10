/*
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.SystemProperty.Builder;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cluster manager is responsible for triggering events related to clustering.
 * A future version will also provide statistics about the cluster.
 *
 * @author Gaston Dombiak
 */
public class ClusterManager {
    
    private static final Logger Log = LoggerFactory.getLogger(ClusterManager.class);

    /*
     * Default heart beat threshold in seconds.
     */
    protected static final int DEFAULT_HEARTBEAT_THRESHOLD = 130;
    
    /*
     * Default heartbeat interval in seconds.
     */
    protected static final int DEFAULT_HEARTBEAT_INTERVAL = 60;
    
    /*
     * Default threshold in hours that a cluster node's information is held in the database.
     */
    protected static final int DEFAULT_CLUSTER_CLEANER_KICKED_NODE_THRESHOLD = 24;
    
    /*
     * Default cluster cleaner interval in seconds.  Default to 15 minutes (900 seconds)
     */
    protected static final int DEFAULT_CLUSTER_CLEANER_INTERVAL = 900;    
    
    public static String CLUSTER_PROPERTY_NAME = "clustering.enabled";
    
    public static String CLUSTER_HEARTBEAT_THRESHOLD_NAME = "clustering.heartbeat.evictionThreshold";
    
    public static String CLUSTER_HEARTBEAT_INTERVAL_NAME = "clustering.heartbeat.interval";
    
    public static String CLUSTER_CLEARNER_KICKED_NODE_THRESHOLD_NAME = "clustering.cleaner.kickedNodeThreshold";
    
    public static String CLUSTER_CLEARNER_INTERVAL_NAME = "clustering.cleaner.interval";    
    
    private static Queue<ClusterEventListener> listeners = new ConcurrentLinkedQueue<>();
    private static BlockingQueue<Event> events = new LinkedBlockingQueue<>(10000);
    private static Thread dispatcher;
    private static Thread clusterHeartBeatThread;
    private static Thread clusterHeartCleanThread;    
    
    
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> CLUSTER_NODE_PROVIDER;
	
	private static ClusterNodeProvider provider;
      
    
    static 
    {
    	CLUSTER_NODE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.clusternode.className").setBaseClass(ClusterNodeProvider.class)
				.setDefaultValue(DefaultClusterNodeProvider.class).addListener(ClusterManager::initProvider).setDynamic(true)
				.build();
    	
		initProvider(CLUSTER_NODE_PROVIDER.getValue());
		
    	// Listen for clustering property changes (e.g. enabled/disabled)
        PropertyEventDispatcher.addListener(new PropertyEventListener() {
            @Override
            public void propertySet(String property, Map<String, Object> params)
            {
                if (ClusterManager.CLUSTER_PROPERTY_NAME.equals(property)) {
                    TaskEngine.getInstance().submit(new Runnable() {
                        @Override
                        public void run() {
                            if (Boolean.parseBoolean((String) params.get("value"))) {
                                // Reload/sync all Jive properties
                                JiveProperties.getInstance().init();
                                ClusterManager.startup();
                            } else {
                                ClusterManager.shutdown();
                            }
                        }
                    });
                }
            }          

            @Override
            public void propertyDeleted(String property, Map<String, Object> params) { /* ignore */ }
            @Override
            public void xmlPropertyDeleted(String property, Map<String, Object> params) { /* ignore */ }
            @Override
            public void xmlPropertySet(final String property, final Map<String, Object> params) { /* ignore */ } 
        });
    }
    
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (ClusterNodeProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading domain provider: " + clazz.getName(), e);
                provider = new DefaultClusterNodeProvider();
            }
        }
    }
    
    public static ClusterNodeProvider getClusterNodeProvider()
    {
    	return provider;
    }
    
    /**
     * Instantiate and start the cluster event dispatcher thread
     */
    private static void initEventDispatcher() 
    {
        if (dispatcher == null || !dispatcher.isAlive()) 
        {
            dispatcher = new Thread("ClusterManager events dispatcher") 
            {
                @Override
                public void run() 
                {
                    // exit thread if/when clustering is disabled
                    while (ClusterManager.isClusteringEnabled()) 
                    {
                        try 
                        {
                            Event event = events.take();
                            EventType eventType = event.getType();
                            // Make sure that CacheFactory is getting this events first (to update cache structure)
                            if (event.getNodeID() == null) 
                            {
                                // Replace standalone caches with clustered caches and migrate data
                                if (eventType == EventType.joined_cluster) 
                                {
                                    CacheFactory.joinedCluster();
                                } 
                                else if (eventType == EventType.left_cluster) 
                                {
                                	if (!isClusteringEnabled())
                                	{
                                		// this is most likely due a shutdown in the server
                                		// it is a waste of shutdown time to copy objects from the 
                                		// cluster cache to a local cache when the server is about 
                                		// to terminate
                                		CacheFactory.leftCluster();
                                	}
                                    
                                }
                            }
                            // Now notify rest of the listeners
                            for (ClusterEventListener listener : listeners) 
                            {
                                try {
                                    switch (eventType) {
                                        case joined_cluster: {
                                            if (event.getNodeID() == null) {
                                                listener.joinedCluster();
                                            }
                                            else {
                                                listener.joinedCluster(event.getNodeID());
                                            }
                                            break;
                                        }
                                        case left_cluster: {
                                            if (event.getNodeID() == null) {
                                                listener.leftCluster();
                                            }
                                            else {
                                                listener.leftCluster(event.getNodeID());
                                            }
                                            break;
                                        }
                                        case marked_senior_cluster_member: {
                                            listener.markedAsSeniorClusterMember();
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                }
                                catch (Exception e) {
                                    Log.error(e.getMessage(), e);
                                }
                            }
                            // Mark event as processed
                            event.setProcessed(true);
                        } catch (Exception e) {
                            Log.warn(e.getMessage(), e);
                        }
                    }
                }
            };
            dispatcher.setDaemon(true);
            dispatcher.start();
        }
    }

    private static void initCusterHeartBeatMonitor()
    {
        if (clusterHeartBeatThread == null || !clusterHeartBeatThread.isAlive()) 
        {
        	clusterHeartBeatThread = new Thread("ClusterManager cluster heart beat thread") 
            {
                @Override
                public void run() 
                {
                	int heartBeatThreshold = JiveGlobals.getIntProperty(CLUSTER_HEARTBEAT_THRESHOLD_NAME, DEFAULT_HEARTBEAT_THRESHOLD);
                	int heartBeatInterval = JiveGlobals.getIntProperty(CLUSTER_HEARTBEAT_INTERVAL_NAME, DEFAULT_HEARTBEAT_INTERVAL);
                	
                    // exit thread if/when clustering is disabled
                    while (ClusterManager.isClusteringEnabled()) 
                    {
                    	
	                	try
	                	{	                		
		                	// first check if we exist in the cluster
	                		ClusterNode me = null;
	                		try
	                		{
	                			me = provider.getClusterMember(XMPPServer.getInstance().getNodeID());
	                			if (me != null && me.getNodeLeftDtTm() != null)
	                			{
	                				// we were kicked from the cluster... need to shutdown this app instance
	                				// and get restarted... mostly likely our clustered cache entries were
	                				// purged as well
	                				
	                				try
	                				{
	                					Log.info("The node {} has been kicked from the cluster but is still running.  Initiating shut down of node.",
	                							me.getNodeId().toString());
	                					XMPPServer.getInstance().stop();
	                				}
	                				catch (Exception e)
	                				{
	                					Log.error("Error detected durning cluster kick shutdown of node {}", me.getNodeId().toString(), e);
	                				}
	                				
	                				Log.info("Calling System.exit() on node {} due to detected cluster kick.", me.getNodeId().toString());
	                				System.exit(0);
	                			}
	                		}
	                		catch (ClusterNodeNotFoundException e)
	                		{
	                			me = joinCluster();
	                		}
	                		
	                		// make sure we're listed as active
	                		if (me.getNodeStatus() != ClusterNodeStatus.NODE_JOINED && me.getNodeStatus() != ClusterNodeStatus.NODE_MASTER)
	                		{
	                			// active me
	                			me.setNodeStatus(ClusterNodeStatus.NODE_JOINED);
	                			provider.updateClusterMember(me);
	                			
	                			// we rejoined the cluster
	                			fireJoinedCluster(true);
	                		}
	                			
	                		// heart beat
	                		me = provider.heartBeatClusterMemeber(me.getNodeId());
	                		
		                	/*
		                	 * get the full list of cluster entries, this should be a small list (probably less than 10)
		                	 * and see if anyone needs to be evicted... also check for the master and update if needed
		                	 */
		                	Collection<ClusterNode> clusterNodes = provider.getClusterMembers();
		                	
		                	final Instant hbThreadshold = Instant.now().minus(heartBeatThreshold, ChronoUnit.SECONDS);
		                	
		                	ClusterNode masterNode = null;
		                	boolean duplicateMastersDetected = false;
		                	for (ClusterNode node : clusterNodes)
		                	{
		                		if (node.getLastNodeHBDtTm().compareTo(hbThreadshold) < 0)
		                		{
		                			// evict
		                			Log.info("Evicting cluster node {} due to lack of heart beat.", node.getNodeId().toString());
		                			node.setNodeLeftDtTm(Instant.now());
		                			node.setNodeStatus(ClusterNodeStatus.NODE_EVICTED);
		                			provider.updateClusterMember(node);
		                			
		                			CacheFactory.purgeClusteredNodeCaches(node.getNodeId());
		                			
		                			fireLeftCluster(node.getNodeId().toByteArray());
		                			
		                			
		                		}
		                		else if (node.getNodeStatus() == ClusterNodeStatus.NODE_MASTER)
		                		{
		                			// is there already a master in the list?
		                			if (masterNode != null)
		                				duplicateMastersDetected = true;
		                			else  // no existing master detected... mark this node as the master
		                				masterNode = node;
		                		}
		                		
		                	}
		                	
		                	/*
		                	 *  check if a master node is detected or if there is a duplicate master... It's possible there is no master if we either
		                	 *  evicted the master or this is the first node in the cluster that is just now coming on line
		                	 */
		                	if (masterNode == null || duplicateMastersDetected)
		                	{
		                		
		                		/*
		                		 * The master will be selected by being the node that has been in the cluster the longest
		                		 * Reload the cluster list (in case we evicted somebody)
		                		 */
		                		clusterNodes = provider.getClusterMembers();
		                		for (ClusterNode node : clusterNodes)
		                		{
		                			if (masterNode == null)
		                				masterNode = node;
		                			else if (node.getNodeJoinedDtTm().compareTo(masterNode.getNodeJoinedDtTm()) < 0)
		                			{
		                				masterNode = node;
		                			}
		                		}
		                		
		                		/*
		                		 *  check if there are duplicate masters... if there are, then we need to demote 
		                		 *  one or more duplicates
		                		 */
		                		if (duplicateMastersDetected)
		                		{
		                			for (ClusterNode node : clusterNodes)
		                			{
		                				if (node.getNodeStatus().equals(ClusterNodeStatus.NODE_MASTER) && !node.getNodeId().equals(masterNode.getNodeId()))
		                				{
		                					// demote this master to just a cluster member
		                					node.setNodeStatus(ClusterNodeStatus.NODE_JOINED);
		                					provider.updateClusterMember(node);
		                				}
		                			}
		                		}
		                		
		                		/*
		                		 * Promote the master node
		                		 */
		                		Log.info("Promoting cluster node {} to the master node.", masterNode.getNodeId().toString());
		                		masterNode.setNodeStatus(ClusterNodeStatus.NODE_MASTER);
		                		provider.updateClusterMember(masterNode);
		                		
		                		fireMarkedAsSeniorClusterMember();
		                	}
		                	
	                    	// sleep for the amount specified by the heart beat interval
	                    	try
	                    	{
	                    		Thread.sleep(heartBeatInterval * 1000);
	                    	}
	                    	catch (Exception e)  {/* no-op */}
		               
	                	}
	                	catch (ClusterException e)
	                	{
	                		Log.error("Failed to perform cluster heartbeat operation.", e);
	                	}
                    }
                }
            };
        }
        clusterHeartBeatThread.setDaemon(true);
        clusterHeartBeatThread.start();
    }
    
    private static void initCusterCleaner()
    {
        if (clusterHeartCleanThread == null || !clusterHeartCleanThread.isAlive()) 
        {
        	clusterHeartCleanThread = new Thread("ClusterManager cluster cleaner thread") 
            {
                @Override
                public void run() 
                {

                	int kickedNodeThreshold = JiveGlobals.getIntProperty(CLUSTER_CLEARNER_KICKED_NODE_THRESHOLD_NAME, DEFAULT_CLUSTER_CLEANER_KICKED_NODE_THRESHOLD);
                	int cleanerInterval = JiveGlobals.getIntProperty(CLUSTER_CLEARNER_INTERVAL_NAME, DEFAULT_CLUSTER_CLEANER_INTERVAL);
                	
                    // exit thread if/when clustering is disabled
                    while (ClusterManager.isClusteringEnabled()) 
                    {
	                	try
	                	{
	                    	// Only the master none will do the clean up
	                    	if (isSeniorClusterMember())
	                    	{
	
	                    		Log.info("Master node {} is running cluster cleanup tasks.", XMPPServer.getInstance().getNodeID().toString());
	                    		
			                	final Collection<ClusterNode> nonMemberNodes = provider.getNonClusterMembers();
			                		
			                	for (ClusterNode node : nonMemberNodes)
			                	{
			                		Log.info("Purging caches for non cluster member node {}", node.getNodeId().toString());
			                		
			                		try
			                		{
			                			CacheFactory.purgeClusteredNodeCaches(node.getNodeId());	
			                			
			                			// check to see if this non member node has surpassed the kick threshold
			                			final Instant kickThreshold = Instant.now().minus(kickedNodeThreshold, ChronoUnit.HOURS);
			                			
				                		if (node.getNodeLeftDtTm() != null &&  node.getNodeLeftDtTm().compareTo(kickThreshold) < 0)
				                		{
				                			// purge
				                			Log.info("Purging cluster history information for cluster node {}", node.getNodeId().toString());
				                			provider.purgeClusterMemeber(node.getNodeId());
				                			
				                		}
			                		}
			                		catch (Exception e)
			                		{
			                			Log.error("Error occured cleaning non cluster member node {}", node.getNodeId(), e);
			                		}
			                	}
	                		}

                    	}
	                	catch (Exception e)
	                	{
	                		Log.error("Failed to perform cluster clean operation.", e);
	                	}
	                	
                    	// sleep for the amount specified by the heart beat interval
                    	try
                    	{
                    		Thread.sleep(cleanerInterval * 1000);
                    	}
                    	catch (Exception e)  {/* no-op */}
                    }
                }
            };
        }
        
        clusterHeartCleanThread.setDaemon(true);
        clusterHeartCleanThread.start();        
    }
    
    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(ClusterEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void removeListener(ClusterEventListener listener) {
        listeners.remove(listener);
    }


    /**
     * Triggers event indicating that this JVM is now part of a cluster. At this point the
     * {@link org.jivesoftware.openfire.XMPPServer#getNodeID()} holds the new nodeID value and
     * the old nodeID value is passed in case the listener needs it.
     * <p>
     * When joining the cluster as the senior cluster member the {@link #fireMarkedAsSeniorClusterMember()}
     * event will be sent right after this event.
     * </p>
     * This event will be triggered in another thread. This will avoid potential deadlocks
     * in Coherence.
     *
     * @param asynchronous true if event will be triggered in background
     */
    public static void fireJoinedCluster(boolean asynchronous) {
        try {
            Log.info("Firing joined cluster event for this node");
            Event event = new Event(EventType.joined_cluster, null);
            events.put(event);
            if (!asynchronous) {
                while (!event.isProcessed()) {
                    Thread.sleep(50);
                }
            }
        } catch (InterruptedException e) {
            // Should never happen
            Log.error(e.getMessage(), e);
        }
    }

    /**
     * Triggers event indicating that another JVM is now part of a cluster.<p>
     *
     * This event will be triggered in another thread. This will avoid potential deadlocks
     * in Coherence.
     *
     * @param nodeID    nodeID assigned to the JVM when joining the cluster.
     * @param asynchronous true if event will be triggered in background
     */
    public static void fireJoinedCluster(byte[] nodeID, boolean asynchronous) {
        try {
            Log.info("Firing joined cluster event for another node:" + new String(nodeID, StandardCharsets.UTF_8));
            Event event = new Event(EventType.joined_cluster, nodeID);
            events.put(event);
            if (!asynchronous) {
                while (!event.isProcessed()) {
                    Thread.sleep(50);
                }
            }
        } catch (InterruptedException e) {
            // Should never happen
            Log.error(e.getMessage(), e);
        }
    }

    /**
     * Triggers event indicating that this JVM is no longer part of the cluster. This could
     * happen when disabling clustering support or removing the enterprise plugin that provides
     * clustering support.<p>
     *
     * Moreover, if we were in a "split brain" scenario (ie. separated cluster islands) and the
     * island were this JVM belonged was marked as "old" then all nodes of that island will
     * get the {@code left cluster event} and {@code joined cluster events}. That means that
     * caches will be reset and thus will need to be repopulated again with fresh data from this JVM.
     * This also includes the case where this JVM was the senior cluster member and when the islands
     * met again then this JVM stopped being the senior member.
     */
    public static void fireLeftCluster() {
        try {
            Log.info("Firing left cluster event for this node");
            Event event = new Event(EventType.left_cluster, null);
            events.put(event);
        } catch (InterruptedException e) {
            // Should never happen
            Log.error(e.getMessage(), e);
        }
    }

    /**
     * Triggers event indicating that another JVM is no longer part of the cluster. This could
     * happen when disabling clustering support or removing the enterprise plugin that provides
     * clustering support.
     *
     * @param nodeID    nodeID assigned to the JVM when joining the cluster.
     */
    public static void fireLeftCluster(byte[] nodeID) {
        try {
            Log.info("Firing left cluster event for another node:" + new String(nodeID, StandardCharsets.UTF_8));
            Event event = new Event(EventType.left_cluster, nodeID);
            events.put(event);
        } catch (InterruptedException e) {
            // Should never happen
            Log.error(e.getMessage(), e);
        }
    }

    /**
     * Triggers event indicating that this JVM is now the senior cluster member. This
     * could either happen when initially joining the cluster or when the senior cluster
     * member node left the cluster and this JVM was marked as the new senior cluster member.
     * <p>
     * Moreover, in the case of a "split brain" scenario (ie. separated cluster islands) each
     * island will have its own senior cluster member. However, when the islands meet again there
     * could only be one senior cluster member so one of the senior cluster members will stop playing
     * that role. When that happens the JVM no longer playing that role will receive the
     * {@link #fireLeftCluster()} and {@link #fireJoinedCluster(boolean)} events.</p>
     * <p>
     * This event will be triggered in another thread. This will avoid potential deadlocks
     * in Coherence.</p>
     */
    public static void fireMarkedAsSeniorClusterMember() {
        try {
            Log.info("Firing marked as senior event");
            events.put(new Event(EventType.marked_senior_cluster_member, null));
        } catch (InterruptedException e) {
            // Should never happen
        }
    }

    /**
     * Starts the cluster service if clustering is enabled. The process of starting clustering
     * will recreate caches as distributed caches.
     */
    public static synchronized void startup() {
        if (isClusteringEnabled() && !isClusteringStarted()) {
            
        	initEventDispatcher();
            CacheFactory.startClustering();
            initCusterHeartBeatMonitor();
            initCusterCleaner();
        }
    }

    /**
     * Shuts down the clustering service. This method should be called when the
     * system is shutting down or clustering has been disabled. Failing to call
     * this method may temporarily impact cluster performance, as the system will
     * have to do extra work to recover from a non-clean shutdown.
     * If clustering is not enabled, this method will do nothing.
     */
    public static synchronized void shutdown() {
        if (isClusteringStarted()) 
        {
        	try
        	{
        		leaveCluster();
        	}
        	catch (Exception e)
        	{
        		Log.error("Error occured while node {} tried to leave the cluster.", XMPPServer.getInstance().getNodeID().toString(), e);
        	}
        	
        	
            Log.debug("ClusterManager: Shutting down clustered cache service.");
            CacheFactory.stopClustering();
        }
    }

    /**
     * Sets true if clustering support is enabled. An attempt to start or join
     * an existing cluster will be attempted in the service was enabled. On the
     * other hand, if disabled then this JVM will leave or stop the cluster.
     *
     * @param enabled if clustering support is enabled.
     */
    public static void setClusteringEnabled(boolean enabled) {
        if (enabled) {
            // Check that clustering is not already enabled and we are already in a cluster
            if (isClusteringEnabled() && isClusteringStarted()) {
                return;
            }
        }
        else {
            // Check that clustering is disabled
            if (!isClusteringEnabled()) {
                return;
            }
        }
        // set the clustering property (listener will start/stop as needed)
        JiveGlobals.setProperty(CLUSTER_PROPERTY_NAME, Boolean.toString(enabled));
    }

    /**
     * Returns true if clustering support is enabled. This does not mean
     * that clustering has started or that clustering will be able to start.
     *
     * @return true if clustering support is enabled.
     */
    public static boolean isClusteringEnabled() {
        return JiveGlobals.getBooleanProperty(CLUSTER_PROPERTY_NAME, false);
    }

    /**
     * Returns true if clustering is installed and can be used by this JVM
     * to join a cluster. A false value could mean that either clustering
     * support is not available or the license does not allow to have more
     * than 1 cluster node.
     *
     * @return true if clustering is installed and can be used by 
     * this JVM to join a cluster.
     */
    public static boolean isClusteringAvailable() {
        return CacheFactory.isClusteringAvailable();
    }

    /**
     * Returns true is clustering is currently being started. Once the cluster
     * is started or failed to be started this value will be false.
     *
     * @return true is clustering is currently being started.
     */
    public static boolean isClusteringStarting() {
        return CacheFactory.isClusteringStarting();
    }

    /**
     * Returns true if this JVM is part of a cluster. The cluster may have many nodes
     * or this JVM could be the only node.
     *
     * @return true if this JVM is part of a cluster.
     */
    public static boolean isClusteringStarted() {
        return CacheFactory.isClusteringStarted();
    }

    /**
     * Returns true if this member is the senior member in the cluster. If clustering
     * is not enabled, this method will also return true. This test is useful for
     * tasks that should only be run on a single member in a cluster.
     *
     * @return true if this cluster member is the senior or if clustering is not enabled.
     * <p><strong>Important:</strong> If clustering is enabled but has not yet started, this may return an incorrect result</p>
     * @see #isSeniorClusterMemberOrNotClustered()
     */
    public static boolean isSeniorClusterMember() {
        return CacheFactory.isSeniorClusterMember();
    }

    /**
     * Returns basic information about the current members of the cluster or an empty
     * collection if not running in a cluster.
     *
     * @return information about the current members of the cluster or an empty
     *         collection if not running in a cluster.
     */
    public static Collection<ClusterNodeInfo> getNodesInfo() {
        return CacheFactory.getClusterNodesInfo();
    }

    /**
     * Returns basic information about a specific member of the cluster.
     *
     * @since Openfire 4.4
     * @param nodeID the node whose information is required
     * @return the node specific information, or {@link Optional#empty()} if the node could not be identified
     */
    public static Optional<ClusterNodeInfo> getNodeInfo(final byte[] nodeID) {
        return getNodeInfo(NodeID.getInstance(nodeID));
    }

    /**
     * Returns basic information about a specific member of the cluster.
     *
     * @since Openfire 4.4
     * @param nodeID the node whose information is required
     * @return the node specific information, or {@link Optional#empty()} if the node could not be identified
     */
    public static Optional<ClusterNodeInfo> getNodeInfo(final NodeID nodeID) {
        return CacheFactory.getClusterNodesInfo().stream()
            .filter(nodeInfo -> nodeInfo.getNodeID().equals(nodeID))
            .findAny();
    }

    /**
     * Returns the maximum number of cluster members allowed. Both values 0 and 1 mean that clustering
     * is not available. However, a value of 1 means that it's a license problem rather than not having
     * the ability to do clustering as defined with value 0.
     *
     * @return the maximum number of cluster members allowed or 0 or 1 if clustering is not allowed.
     */
    public static int getMaxClusterNodes() {
        return CacheFactory.getMaxClusterNodes();
    }

    /**
     * Returns the id of the node that is the senior cluster member. When not in a cluster the returned
     * node id will be the {@link XMPPServer#getNodeID()}.
     *
     * @return the id of the node that is the senior cluster member.
     */
    public static NodeID getSeniorClusterMember() {
        byte[] clusterMemberID = CacheFactory.getSeniorClusterMemberID();
        if (clusterMemberID == null) {
            return XMPPServer.getInstance().getNodeID();
        }
        return NodeID.getInstance(clusterMemberID);
    }

    /**
     * Returns true if the specified node ID belongs to a known cluster node
     * of this cluster.
     *
     * @param nodeID the ID of the node to verify.
     * @return true if the specified node ID belongs to a known cluster node
     *         of this cluster.
     */
    public static boolean isClusterMember(byte[] nodeID) {
        for (ClusterNodeInfo nodeInfo : getNodesInfo()) {
            if (nodeInfo.getNodeID().equals(nodeID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If clustering is enabled, this method will wait until clustering is fully started.<br>
     * If clustering is not enabled, this method will return immediately.
     * <p><strong>Important:</strong> because this method blocks the current thread, it must not be called from a plugin
     * constructor or {@link Plugin#initializePlugin(org.jivesoftware.openfire.container.PluginManager, java.io.File)}.
     * This is because the Hazelcast clustering plugin waits until all plugins have initialised before starting, so a
     * plugin cannot wait until clustering has started during initialisation.</p>
     *
     * @throws InterruptedException if the thread is interrupted whilst waiting for clustering to start
     * @since Openfire 4.3.0
     */
    public static void waitForClusteringToStart() throws InterruptedException {
        if (!ClusterManager.isClusteringEnabled()) {
            // Clustering is not enabled, so return immediately
            return;
        }

        final Semaphore semaphore = new Semaphore(0);
        final ClusterEventListener listener = new ClusterEventListener() {
            @Override
            public void joinedCluster() {
                semaphore.release();
            }

            @Override
            public void joinedCluster(final byte[] nodeID) {
                // Not required
            }

            @Override
            public void leftCluster() {
                // Not required
            }

            @Override
            public void leftCluster(final byte[] nodeID) {
                // Not required
            }

            @Override
            public void markedAsSeniorClusterMember() {
                // Not required
            }
        };

        ClusterManager.addListener(listener);
        try {
            if (!ClusterManager.isClusteringStarted()) {
                // We need to wait for the joinedCluster() event
                semaphore.acquire();
            }
        } finally {
            ClusterManager.removeListener(listener);
        }
    }

    /**
     * Returns true if this member is the senior member in the cluster. If clustering
     * is not enabled, this method will also return true. This test is useful for
     * tasks that should only be run on a single member in a cluster. Unlike {@link #isSeniorClusterMember()} this method
     * will block, if necessary, until clustering has completed initialisation. For that reason, do not call in a
     * plugin creation or initialisation stage.
     *
     * @return true if this cluster member is the senior or if clustering is not enabled.
     * @see #isSeniorClusterMember()
     * @since Openfire 4.3.0
     */
    public static boolean isSeniorClusterMemberOrNotClustered() {
        try {
            ClusterManager.waitForClusteringToStart();
            return ClusterManager.isSeniorClusterMember();
        } catch (final InterruptedException ignored) {
            // We were interrupted before clustering started. Re-interrupt the thread, and return false.
            Thread.currentThread().interrupt();
            return false;
        }
    }

    protected static ClusterNode joinCluster() throws ClusterException
    {
    	try
    	{
    		final Instant now = Instant.now();
    		
    		final ClusterNode node = new ClusterNode();
    		node.setNodeId(XMPPServer.getInstance().getNodeID());
    		node.setNodeHost(InetAddress.getLocalHost().getCanonicalHostName());
    		node.setNodeIP(InetAddress.getLocalHost().getHostAddress());
    		node.setNodeStatus(ClusterNodeStatus.NODE_JOINED);
    		node.setNodeJoinedDtTm(now);
    		node.setLastNodeHBDtTm(now);
    		
    		provider.addClusterMember(node);
    		fireJoinedCluster(true);
    		
    		return node;
    		
    	}
    	catch (UnknownHostException e)
    	{
    		throw new ClusterException("Failed to get host ip address while joining cluster.", e);
    	}
    }
    
    protected static void leaveCluster() throws ClusterException
    {
    	final ClusterNode node = provider.getClusterMember(XMPPServer.getInstance().getNodeID());
    	
    	node.setNodeLeftDtTm(Instant.now());
    	node.setNodeStatus(ClusterNodeStatus.NODE_LEFT);
    	provider.updateClusterMember(node);
    	
    	fireLeftCluster();
    }
    

    private static class Event {
        private EventType type;
        private byte[] nodeID;
        private boolean processed;

        public Event(EventType type, byte[] oldNodeID) {
            this.type = type;
            this.nodeID = oldNodeID;
        }

        public EventType getType() {
            return type;
        }

        public byte[] getNodeID() {
            return nodeID;
        }

        public boolean isProcessed() {
            return processed;
        }

        public void setProcessed(boolean processed) {
            this.processed = processed;
        }

        @Override
        public String toString() {
            return super.toString() + " type: " + type;
        }
    }

    /**
     * Represents valid event types.
     */
    private enum EventType {

        /**
         * This JVM joined a cluster.
         */
        joined_cluster,

        /**
         * This JVM is no longer part of the cluster.
         */
        left_cluster,

        /**
         * This JVM is now the senior cluster member.
         */
        marked_senior_cluster_member
    }
}
