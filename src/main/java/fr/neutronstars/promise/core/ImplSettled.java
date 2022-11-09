package fr.neutronstars.promise.core;

import fr.neutronstars.promise.api.Settled;
import fr.neutronstars.promise.api.Status;

class ImplSettled<T> implements Settled<T>
{
    private Status status = Status.WAITING;
    private Throwable throwable;
    private T value;
    private long startAt;
    private long endAt;

    @Override
    public Status status()
    {
        return this.status;
    }

    protected void status(Status status)
    {
        this.status = status;
    }

    @Override
    public T value()
    {
        return this.value;
    }

    protected void value(T value)
    {
        this.value = value;
    }

    @Override
    public Throwable throwable()
    {
        return this.throwable;
    }

    protected void throwable(Throwable throwable)
    {
        this.throwable = throwable;
    }

    @Override
    public String reason()
    {
        return throwable != null ? throwable.getMessage() : null;
    }

    @Override
    public long time()
    {
        return this.endAt - this.startAt;
    }

    protected void startAt(long startAt)
    {
        this.startAt = startAt;
    }

    protected void endAt(long endAt)
    {
        this.endAt = endAt;
    }
}
