# Promise Core using Promise API.

This core readapts the Promise javascript on java. They aim to be as similar as possible to the original ones and are also asynchronous.

## Implementation of core in your project with gradle

**API**: https://github.com/Neutron-Pro/promise-api

```gradle
repositories {
  maven { url "https://jitpack.io" }
}

dependencies {
  implementation 'com.github.Neutron-Pro:promise-core:{{ version }}'
}
```

## Documentation

### Create Promise service:

For create the service, your must use `fr.neutronstars.promise.core.ImplPromiseService`
You must indicate the number of pools (thread) you want.

```java
final int pools = 8;
final PromiseService service = ImplPromiseService.create(pools);
```

Otherwise, if your project already uses a ScheduledExecutorService. You can provide it directly.
```java
final PromiseService service = ImplPromiseService.create(yourScheduledExecutorService);
```

Once your program is finished, don't forget to stop the service.
```java
service.shutdown();
```

### Use Promise:

```java
service.of(resolver -> resolver.accept("This is a test!"))
    .then(System.out::println) // Output: "This is a test!"
    .map(str -> 5)
    .then(System.out::println) // Output: 5
    .async();
```

```java
int x = service.of(resolver -> resolver.accept("This is a test!"))
    .then(System.out::println) // Output: "This is a test!"
    .map(str -> 5)
    .then(System.out::println) // Output: 5
    .await();

System.out.println(x); // Output: 5
```

```java
service.of(resolver -> resolver.reject(new RuntimeException("An error has occured")))
    .error(System.err::println) // Output: "RuntimeException: An error has occured"
    .async();
```

```java
service.of(resolver -> resolver.accept("This is a test!"))
    .then(System.out::println) // Output: "This is a test!"
    .last(() -> System.out.println("This is a Promise.finnaly"))
    .async();
```

```java
final Promise<String> promise = service.of(resolver -> resolver.accept("This is a test!"));
promise.await();

final Settled<String> settled = promise.settled();
System.out.println("Status: " + settled.status()); // Output: "Status: FULFILLED"
System.out.println("Value: " + settled.value()); // Output: "Value: This is a test!"
System.out.println("Time: " + settled.time() + "ms"); // Output: "Time: 0ms"
```

### Promise.all, Promise.allSettled, Promise.any and Promise.race

These Promise are quite special in javascript but they are of course added to this library.
It is recommended to run them asynchronously.

```java
service.all(
    service.of(resolver.accept("Promise 1")),
    service.of(resolver.accept("Promise 2")),
    service.of(resolver.reject(new RuntimeException("Promise 3")))
)
.then(objects -> {
    for(Object object : objects) {
        System.out.println(object);
    }
})
.async();
```

```java
service.allSettled(
    service.of(resolver.accept("Promise 1")),
    service.of(resolver.accept("Promise 2")),
    service.of(resolver.reject(new RuntimeException("Promise 3")))
)
.then(settleds -> {
    for(Settled<?> settled : settleds) {
        System.out.println(settled);
    }
})
.async();
```

```java
final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
final PromiseService service = ImplPromiseService.create(executorService);

service.any(
    service.of(resolver -> {
        executorService.schedule(() -> {
            resolver.accept("Promise 1");
        }, 5000, TimeUnit.MILLISECONDS);
    }),
    service.of(resolver -> {
        executorService.schedule(() -> {
            resolver.accept("Promise 2");
        }, 1000, TimeUnit.MILLISECONDS);
    }),
    service.of(resolver.reject(new RuntimeException("Promise 3")))
)
.then(System.out::println)
.error(System.err::println)
.async();

// Output: then -> Promise 2
```

```java
final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
final PromiseService service = ImplPromiseService.create(executorService);

service.race(
    service.of(resolver -> {
        executorService.schedule(() -> {
            resolver.accept("Promise 1");
        }, 5000, TimeUnit.MILLISECONDS);
    }),
    service.of(resolver -> {
        executorService.schedule(() -> {
            resolver.accept("Promise 2");
        }, 1000, TimeUnit.MILLISECONDS);
    }),
    service.of(resolver.reject(new RuntimeException("Promise 3")))
)
.then(System.out::println)
.error(System.err::println)
.async();

// Output: catch -> Promise 3
```
