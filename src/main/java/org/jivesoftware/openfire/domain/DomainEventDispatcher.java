package org.jivesoftware.openfire.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainEventDispatcher
{
    private static final Logger Log = LoggerFactory.getLogger(DomainEventDispatcher.class);

    private static List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    private DomainEventDispatcher() 
    {
    }

    public static void addListener(DomainEventListener listener) 
    {
        if (listener == null) 
        {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    public static void removeListener(DomainEventListener listener) 
    {
        listeners.remove(listener);
    }


    public static void dispatchEvent(Domain user, EventType eventType, Map<String,Object> params) 
    {
        for (DomainEventListener listener : listeners) 
        {
            try 
            {
                switch (eventType) 
                {
                    case domain_created: 
                    {
                        listener.domainCreated(user, params);
                        break;
                    }
                    case domain_deleted: 
                    {
                        listener.domainDeleted(user, params);
                        break;
                    }
                    case domain_modified: 
                    {
                        listener.domainModified(user, params);
                        break;
                    }
                    default:
                        break;
                }
            }
            catch (Exception e) 
            {
                Log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Represents valid event types.
     */
    public enum EventType 
    {

        domain_created,


        domain_deleted,

 
        domain_modified,
    }
}
