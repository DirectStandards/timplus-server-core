package org.jivesoftware.openfire.cluster;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ClusterException extends Exception 
{
	private static final long serialVersionUID = -3417844865410851447L;

	protected Throwable nestedThrowable = null;

    public ClusterException() 
    {
        super();
    }

    public ClusterException(String msg) 
    {
        super(msg);
    }

    public ClusterException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public ClusterException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    @Override
    public void printStackTrace() 
    {
        super.printStackTrace();
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream ps) 
    {
        super.printStackTrace(ps);
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace(ps);
    }

    @Override
    public void printStackTrace(PrintWriter pw) 
    {
        super.printStackTrace(pw);
        if (nestedThrowable != null)
            nestedThrowable.printStackTrace(pw);
    }
}
