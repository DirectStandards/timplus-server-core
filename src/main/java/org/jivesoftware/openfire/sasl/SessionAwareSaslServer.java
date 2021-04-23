package org.jivesoftware.openfire.sasl;

import org.jivesoftware.openfire.session.LocalSession;

public interface SessionAwareSaslServer 
{
	public void setLocalSession(LocalSession session);
}
