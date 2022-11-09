import fr.neutronstars.promise.api.Promise;
import fr.neutronstars.promise.api.PromiseService;
import fr.neutronstars.promise.core.ImplPromiseService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Test {
    private static PromiseService service;
    public static void main(String[] args) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
        Test.service = ImplPromiseService.create(executorService);

        Test.service.race(
            Test.service.of(fulfillment -> {
                executorService.schedule(() -> {
                    System.out.println("test 1");
                    fulfillment.accept("toto");
                }, 1000, TimeUnit.MILLISECONDS);
            }),
            Test.service.of(fulfillment -> {
                executorService.schedule(() -> {
                    System.out.println("test 2");
                    fulfillment.accept("tata");
                }, 5000, TimeUnit.MILLISECONDS);
            }),
            Test.service.of(fulfillment -> fulfillment.reject(new NullPointerException("This is a test!")))
        ).then(System.out::println)
        .error(throwable -> System.out.println(throwable.getMessage()))
        .last(() -> Test.service.shutdown())
        .async();
    }

    private static Promise<String> name()
    {
        return Test.service.of(resolve -> {
            resolve.accept("Toto");
        });
    }

    private static Promise<String> printAndReturnName() {
        return Test.name().then(System.out::println);
    }
}
