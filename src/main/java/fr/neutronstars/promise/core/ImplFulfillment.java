package fr.neutronstars.promise.core;

import fr.neutronstars.promise.api.Fulfillment;

class ImplFulfillment<T> implements Fulfillment<T>
{
    private T value;
    private Throwable throwable;

    private volatile boolean completed;

    protected ImplFulfillment()
    {}

    protected ImplFulfillment(ImplFulfillment<T> fulfillment)
    {
        this.value = fulfillment.value;
        this.throwable = fulfillment.throwable;
    }

    protected boolean hasValue()
    {
        return this.value != null;
    }

    protected T value()
    {
        return this.value;
    }

    protected boolean completed()
    {
        return this.completed;
    }

    @Override
    public void accept(T value)
    {
        this.value = value;
        this.completed = true;
    }

    protected Throwable throwable()
    {
        return this.throwable;
    }

    @Override
    public void reject(Throwable throwable)
    {
        this.throwable = throwable;
        this.completed = true;
    }
}
