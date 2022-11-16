package fr.neutronstars.promise.core;

import fr.neutronstars.promise.api.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

class ImplPromise<T> implements Promise<T>
{
    private final Function<? super Throwable, Object> rejectFunction;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ImplSettled<T> settled = new ImplSettled<>();
    private final Function<Object, Object> fulfillFunction;
    private final Consumer<? super Throwable> reject;
    private final Consumer<? super T> fulfill;
    private final PromiseService service;
    private final ImplPromise<?> parent;
    private final Resolver<T> resolver;

    private Runnable last;
    protected ImplPromise(
        PromiseService service,
        ScheduledExecutorService scheduledExecutorService,
        Resolver<T> resolver
    ) {
        this.service = service;
        this.scheduledExecutorService = scheduledExecutorService;
        this.resolver = resolver;
        this.fulfillFunction = null;
        this.rejectFunction = null;
        this.fulfill = null;
        this.reject = null;
        this.parent = null;
    }

    private ImplPromise(
        PromiseService service,
        ScheduledExecutorService scheduledExecutorService,
        Function<Object, Object> fulfillFunction,
        Function<? super Throwable, Object> rejectFunction,
        ImplPromise<?> parent
    ) {
        this.service = service;
        this.scheduledExecutorService = scheduledExecutorService;
        this.resolver = null;
        this.fulfillFunction = fulfillFunction;
        this.rejectFunction = rejectFunction;
        this.fulfill = null;
        this.reject = null;
        this.parent = parent;
    }

    private ImplPromise(
        PromiseService service,
        ScheduledExecutorService scheduledExecutorService,
        Consumer<? super T> fulfill,
        Consumer<? super Throwable> reject,
        ImplPromise<?> parent
    ) {
        this.service = service;
        this.scheduledExecutorService = scheduledExecutorService;
        this.resolver = null;
        this.fulfillFunction = null;
        this.rejectFunction = null;
        this.fulfill = fulfill;
        this.reject = reject;
        this.parent = parent;
    }

    @Override
    public Promise<T> then(Consumer<? super T> fulfill)
    {
        return this.then(fulfill, null);
    }

    @Override
    public Promise<T> then(Consumer<? super T> fulfill, Consumer<? super Throwable> reject)
    {
        return new ImplPromise<>(this.service, this.scheduledExecutorService, fulfill, reject, this);
    }

    @Override
    public <V> Promise<V> map(Function<? super T, ? extends V> fulfill)
    {
        return this.map(fulfill, null);
    }

    @Override
    public <V> Promise<V> map(Function<? super T, ? extends V> fulfill, Function<? super Throwable, ? extends V> reject)
    {
        if (fulfill == null) {
            throw new PromiseException("fulfill can't be null.");
        }
        return new ImplPromise<>(
            this.service,
            this.scheduledExecutorService,
            (Function<Object, Object>) fulfill,
            (Function<? super Throwable, Object>) reject,
            this
        );
    }

    @Override
    public <V> Promise<V> error(Consumer<? super Throwable> reject)
    {
        return new ImplPromise<>(this.service, this.scheduledExecutorService, null, reject, this);
    }

    @Override
    public Promise<T> last(Runnable last)
    {
        this.last = last;
        return this;
    }

    @Override
    public Settled<T> settled()
    {
        return this.settled;
    }

    @Override
    public T await() {
        return this.exec(true).value();
    }

    @Override
    public void async()
    {
        this.scheduledExecutorService.schedule(this::await, 0, TimeUnit.MILLISECONDS);
    }

    private ImplFulfillment<T> exec(boolean last)
    {
        if (!this.settled.status().equals(Status.WAITING)) {
            throw new PromiseException("This promise has already been executed!");
        }
        this.settled.status(Status.RUNNING);
        this.settled.startAt(System.currentTimeMillis());
        final ImplFulfillment<T> fulfillment;

        if (this.resolver != null) {
            fulfillment = new ImplFulfillment<>();
            try {
                this.resolver.resolve(fulfillment);
                while (true) {
                    if (fulfillment.completed()) {
                        break;
                    }
                }
            } catch (Throwable throwable) {
                fulfillment.reject(throwable);
            }
        } else {
            assert this.parent != null;
            final ImplFulfillment<?> prev = this.parent.exec(false);
            if (this.fulfillFunction != null) {
                fulfillment = new ImplFulfillment<>();
                if (prev.hasValue()) {
                    fulfillment.accept((T) this.fulfillFunction.apply(prev.value()));
                } else if(this.rejectFunction != null) {
                    try {
                        fulfillment.accept((T) this.rejectFunction.apply(prev.throwable()));
                    } catch (Throwable throwable) {
                        fulfillment.reject(throwable);
                    }
                } else {
                    fulfillment.reject(prev.throwable());
                }
            } else {
                fulfillment = new ImplFulfillment<>((ImplFulfillment<T>) prev);
            }
        }

        if (fulfillment.hasValue()) {
            this.settled.value(fulfillment.value());
            if (this.fulfill != null) {
                this.fulfill.accept(fulfillment.value());
            }
            this.settled.status(Status.FULFILLED);

        } else {
            this.settled.throwable(fulfillment.throwable());
            if (this.reject != null) {
                this.reject.accept(fulfillment.throwable());
            }
            this.settled.status(Status.REJECTED);
        }

        if (last) {
            this.execLast();
        }

        this.settled.endAt(System.currentTimeMillis());

        return fulfillment;
    }

    private void execLast()
    {
        if (this.parent != null) {
            this.parent.execLast();
        }
        if (this.last != null) {
            this.last.run();
        }
    }
}
