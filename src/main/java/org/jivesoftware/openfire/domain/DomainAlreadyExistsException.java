package org.jivesoftware.openfire.domain;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DomainAlreadyExistsException extends Exception 
{
	private static final long serialVersionUID = 5626689386034624501L;

	private Throwable nestedThrowable = null;

    public DomainAlreadyExistsException() 
    {
        super();
    }

    public DomainAlreadyExistsException(String msg) 
    {
        super(msg);
    }

    public DomainAlreadyExistsException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public DomainAlreadyExistsException(String msg, Throwable nestedThrowable) 
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
