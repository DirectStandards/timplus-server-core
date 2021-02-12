package org.jivesoftware.openfire.cluster;

public class ClusterNodeAlreadyExistsException extends ClusterException
{
	private static final long serialVersionUID = -1109719290216204793L;

	public ClusterNodeAlreadyExistsException() 
    {
        super();
    }

    public ClusterNodeAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public ClusterNodeAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public ClusterNodeAlreadyExistsException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}