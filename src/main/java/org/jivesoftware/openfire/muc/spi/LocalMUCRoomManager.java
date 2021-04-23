package org.jivesoftware.openfire.muc.spi;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.openfire.event.GroupEventDispatcher;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class supports the simple LocalMUCRoom management including remove,add and query.
 * @author <a href="mailto:583424568@qq.com">wuchang</a>
 * 2016-1-14
 */
public class LocalMUCRoomManager {
    
    private static final Logger Log = LoggerFactory.getLogger(LocalMUCRoomManager.class);
	
	public static final String LOCAL_ROOM_MANAGER_CACHE_BASE_NAME = "LocalMUC Room Mangaer Cache";
	
	protected final Cache<String, LocalMUCRoom> rooms;
    
	//private final Map<String, LocalMUCRoom> rooms = new ConcurrentHashMap<>();
	
    /*
     * The name of the multi user chat service.  This would be groupchat.<domain name>;
     */
    protected final String serviceName;
    
	public LocalMUCRoomManager(String serviceName)
    {
    	this.serviceName = serviceName;
    	
    	rooms = CacheFactory.createCache(LOCAL_ROOM_MANAGER_CACHE_BASE_NAME + serviceName, false);
    }
    
    public String getServiceName() 
    {
		return serviceName;
	}

	public int getNumberChatRooms(){
        return rooms.size();
    }
    public void addRoom(final String roomname, final LocalMUCRoom room)
    {
    	rooms.put(roomname, room);
        /*
         * No group support
         */
        GroupEventDispatcher.addListener(room);
    }
    
    public Collection<LocalMUCRoom> getRooms(){
        return rooms.values();
    }
    
    public LocalMUCRoom getRoom(final String roomname)
    {	    	
    	return rooms.get(roomname);
    }
    
    public LocalMUCRoom removeRoom(final String roomname){
        //memory leak will happen if we forget remove it from GroupEventDispatcher
        final LocalMUCRoom room = rooms.remove(roomname);
        if (room != null) {
        	/*
        	 * No group support
        	 */
            GroupEventDispatcher.removeListener(room);
        }
        return room;
    }
    
    public void cleanupRooms(final Date cleanUpDate) {
        for (final MUCRoom room : getRooms()) {
            if (room.getEmptyDate() != null && room.getEmptyDate().before(cleanUpDate)) {
                removeRoom(room.getName());
            }
        }
    }
    
    public static String createMUCServiceName(String service, String domain)
    {
    	return new StringBuilder(service).append("@").append(domain).toString();
    }
    

    void persistRoomCacheState(LocalMUCRoom room)
    {
    	rooms.put(room.getName(), room);
    }
}
