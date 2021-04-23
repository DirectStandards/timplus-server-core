package org.jivesoftware.openfire.domain;

import java.util.Map;

public interface DomainEventListener
{
    void domainCreated( Domain user, Map<String, Object> params );

    void domainDeleted( Domain user, Map<String, Object> params );

    void domainModified( Domain user, Map<String, Object> params );
}
