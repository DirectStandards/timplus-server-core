package org.jivesoftware.util;

import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;

public class DomainResolver
{
	public static String resolveUserDomain(String username)
	{
		if (username.contains("@"))
			return username.substring(username.indexOf("@") + 1);
		else
		{
			try
			{
				final User user = UserManager.getInstance().getUser(username);
				return user.getDomain();
			}
			catch (UserNotFoundException e)
			{
				throw new IllegalArgumentException("Domain is not resolvable for username " + username, e);
			}
		}
	}
	
	public static String resolveUsernameLocalPart(String username)
	{
		if (username.contains("@"))
			return username.substring(0, username.indexOf("@"));
		else
			return username;
	}
}
