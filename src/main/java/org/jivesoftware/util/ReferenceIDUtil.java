package org.jivesoftware.util;

import java.util.HashMap;
import java.util.Map;

public class ReferenceIDUtil
{
	private static Map<Object, String> connectionContext = new HashMap<>();
	
	public static synchronized String getSessionReferenceId(Object correlationObject)
	{
		return connectionContext.get(correlationObject);
	}
	
	public static synchronized void setSessionReferenceId(Object correlationObject, String referenceId)
	{
		connectionContext.put(correlationObject, referenceId);
	}
	
	public static synchronized void removeSessionReferenceId(Object correlationObject)
	{
		connectionContext.remove(correlationObject);
	}
}
