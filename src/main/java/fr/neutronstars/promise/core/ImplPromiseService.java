package fr.neutronstars.promise.core;

import fr.neutronstars.promise.api.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ImplPromiseService implements PromiseService
{
    public static PromiseService create(int pools)
    {
        return ImplPromiseService.create(Executors.newScheduledThreadPool(pools));
    }

    public static PromiseService create(ScheduledExecutorService service)
    {
        return new ImplPromiseService(service);
    }

    private final ScheduledExecutorService service;

    private ImplPromiseService(ScheduledExecutorService service)
    {
        this.service = service;
    }

    @Override
    public <T> Promise<T> of(Resolver<T> resolver)
    {
        return new ImplPromise<>(this, this.service, resolver);
    }

    @Override
    public Promise<Object[]> all(final Promise<?>... promises)
    {
        return new ImplPromise<>(
            this,
            this.service,
            resolver -> {
                this.launch(promises);
                final Object[] objects = new Object[promises.length];
                while (true) {
                    int end = 0;
                    for (int i = 0; i < promises.length; i++) {
                        final Promise<?> promise = promises[i];
                        if (!promise.settled().status().equals(Status.RUNNING)) {
                            end++;
                            final Object value = promise.settled().value();
                            objects[i] = value != null ? value : promise.settled().throwable();
                        }
                    }

                    if (end >= promises.length) {
                        break;
                    }
                }
                resolver.accept(objects);
            }
        );
    }

    @Override
    public Promise<Settled<?>[]> allSettled(Promise<?>... promises)
    {
        return new ImplPromise<>(
                this,
                this.service,
                resolver -> {
                    this.launch(promises);
                    final Settled<?>[] settleds = new Settled[promises.length];
                    while (true) {
                        int end = 0;
                        for (int i = 0; i < promises.length; i++) {
                            final Promise<?> promise = promises[i];
                            if (!promise.settled().status().equals(Status.RUNNING)) {
                                end++;
                                settleds[i] = promise.settled();
                            }
                        }

                        if (end >= promises.length) {
                            break;
                        }
                    }
                    resolver.accept(settleds);
                }
        );
    }

    @Override
    public Promise<?> any(Promise<?>... promises)
    {
        return new ImplPromise<>(
            this,
            this.service,
            resolver -> {
                this.launch(promises);
                this.forWait(promises);
                Promise<?> firstExecuted = null;
                for (Promise<?> promise : promises) {
                    if (!promise.settled().status().equals(Status.FULFILLED)) {
                        continue;
                    }
                    if (firstExecuted == null) {
                        firstExecuted = promise;
                        continue;
                    }
                    if (firstExecuted.settled().time() > promise.settled().time()) {
                        firstExecuted = promise;
                    }
                }
                if (firstExecuted != null) {
                    resolver.accept(firstExecuted.settled().value());
                    return;
                }
                resolver.reject(new PromiseException("Can't resolve any promise!"));
            }
        );
    }

    @Override
    public Promise<?> race(Promise<?>... promises)
    {
        return new ImplPromise<>(
            this,
            this.service,
            resolver -> {
                this.launch(promises);
                this.forWait(promises);
                Promise<?> firstExecuted = null;
                for (Promise<?> promise : promises) {
                    if (promise.settled().status().equals(Status.REJECTED)) {
                        resolver.reject(promise.settled().throwable());
                        return;
                    }
                    if (firstExecuted == null) {
                        firstExecuted = promise;
                        continue;
                    }
                    if (firstExecuted.settled().time() > promise.settled().time()) {
                        firstExecuted = promise;
                    }
                }
                if (firstExecuted != null) {
                    resolver.accept(firstExecuted.settled().value());
                    return;
                }
                resolver.reject(new PromiseException("Can't resolve race promise!"));
            }
        );
    }

    private void launch(Promise<?>... promises)
    {
        for (Promise<?> promise : promises) {
            promise.async();
        }
    }

    private void forWait(Promise<?>... promises)
    {
        while (true) {
            int end = 0;
            for (final Promise<?> promise : promises) {
                if (promise.settled().status().completed()) {
                    end++;
                }
            }
            if (end >= promises.length) {
                break;
            }
        }
    }

    @Override
    public void shutdown()
    {
        this.service.shutdown();
    }
}
