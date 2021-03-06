package com.sunchaser.sparrow.microservice.springcloud.resilience4j.web.controller;

import com.alibaba.fastjson.JSON;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

/**
 * @author sunchaser admin@lilu.org.cn
 * @since JDK8 2021/1/16
 */
@RestController
public class TestController {
    @GetMapping("/testResilience4j")
    public void testResilience4j() {
        doTestResilience4j();
    }

    public static void main(String[] args) {
        doTestResilience4j();
    }

    public static void doTestResilience4j() {
        doTestGetCircuitBreaker();
        doBaseTestCircuitBreaker();
        doRxJavaTestCircuitBreaker();
        doTestCircuitBreaker();
    }

    private static void doTestCircuitBreaker() {
        // 熔断器配置：执行2次进入open状态
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(2)
                .slidingWindowType(COUNT_BASED)
                .build();
        // 根据指定配置创建熔断器
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-circuit-breaker", circuitBreakerConfig);
        // 初始状态：CLOSED
        System.out.println("init state:" + circuitBreaker.getState());
        // 模拟调用目标方法发生2次异常
        circuitBreaker.onError(0, TimeUnit.SECONDS, new RuntimeException());
        circuitBreaker.onError(0, TimeUnit.SECONDS, new RuntimeException());
        // 再次查看熔断器状态：OPEN
        System.out.println("after two error#state:" + circuitBreaker.getState());
        // 为目标方法配置熔断器，已为OPEN状态的熔断器是否能使目标方法调用成功
        Try<String> result = Try.of(
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker, TestController::doNoInputTargetMethod)
        ).map(value -> value + "do map");
        // 调用结果输出
        System.out.println(result.isSuccess());
        System.out.println(result.get());
    }

    private static void doRxJavaTestCircuitBreaker() {
        CircuitBreaker rxJavaCircuitBreaker = CircuitBreaker.ofDefaults("rx-java-circuit-breaker");
        Callable<String> callable = TestController::doRxJavaTargetMethod;
        Observable<String> observable = Observable.fromCallable(callable)
                .compose(CircuitBreakerOperator.of(rxJavaCircuitBreaker));
        Flowable<String> flowable = Flowable.fromCallable(callable)
                .compose(CircuitBreakerOperator.of(rxJavaCircuitBreaker));
    }

    private static void doReactorTestCircuitBreaker() {
        CircuitBreaker reactorCircuitBreaker = CircuitBreaker.ofDefaults("reactor-circuit-breaker");
        Callable<String> callable = TestController::doReactorTargetMethod;
        Observable<String> compose = Observable.fromCallable(callable)
                .compose(CircuitBreakerOperator.of(reactorCircuitBreaker));
    }

    private static String doRxJavaTargetMethod() {
        return "do rx java";
    }

    private static String doReactorTargetMethod() {
        return "do reactor";
    }

    private static void doBaseTestCircuitBreaker() {
        // 获取一个熔断器实例对象
        CircuitBreaker noInputCircuitBreaker = CircuitBreaker.ofDefaults("no-input-circuit-breaker");
        // 为目标方法（没有入参）配置熔断器
        CheckedFunction0<String> noInputSupplier = CircuitBreaker.decorateCheckedSupplier(
                noInputCircuitBreaker,
                TestController::doNoInputTargetMethod
        );
        // 使用Try从装饰函数中获取无参的目标方法的执行结果
        Try<String> result = Try.of(noInputSupplier)
                .map(value -> value);
        boolean success = result.isSuccess();
        if (success) {
            System.out.println(result.get());
        }
        // 如果目标方法有入参，可按以下方式配置熔断器
        CircuitBreaker hasInputCircuitBreaker = CircuitBreaker.ofDefaults("has-input-circuit-breaker");
        CheckedFunction1<String, String> hasInputFunction = CircuitBreaker.decorateCheckedFunction(
                hasInputCircuitBreaker,
                TestController::doHasInputTargetMethod
        );
        // 使用Try从装饰函数中获取带参的目标方法的执行结果
        Try<String> stringTry = Try.of(noInputSupplier)
                .mapTry(hasInputFunction);
        if (stringTry.isSuccess()) {
            System.out.println(stringTry.get());
        }
    }

    private static String doHasInputTargetMethod(String input) {
        return input + "执行带参的目标方法";
    }

    private static String doNoInputTargetMethod() {
        return "执行无参的目标方法";
    }

    private static void doTestGetCircuitBreaker() {
        // 生成自定义熔断器配置
        CircuitBreakerConfig customConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .slidingWindowType(COUNT_BASED)
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(RuntimeException.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();

        // 获取默认的熔断器的注册器实例
        CircuitBreakerRegistry defaultRegistry = CircuitBreakerRegistry.ofDefaults();
        // 根据自定义熔断器配置生成自定义熔断器的注册器
        CircuitBreakerRegistry customConfigRegistry = CircuitBreakerRegistry.of(customConfig);

        // 生成熔断器实例
        // 使用默认注册器生成
        CircuitBreaker defaultConfigCircuitBreaker = defaultRegistry.circuitBreaker("defaultConfigCircuitBreaker");
        // 使用自定义注册器生成
        CircuitBreaker customConfigCircuitBreaker = customConfigRegistry.circuitBreaker("customConfigCircuitBreaker");
        // 使用默认注册器+自定义熔断器配置生成
        CircuitBreaker defaultRegistryCustomConfigCircuitBreaker = defaultRegistry.circuitBreaker("defaultRegistryCustomConfigCircuitBreaker", customConfig);
        // 对Java8的lambda表达式和方法引用的支持：在生成熔断器时进行自定义配置
        CircuitBreaker lambdaCustomConfigCircuitBreaker = defaultRegistry.circuitBreaker("lambdaCustomConfigCircuitBreaker", TestController::buildCustomCircuitBreakerConfig);

        // 打印输出由注册器生成的熔断器的配置
        CircuitBreakerConfig defaultCircuitBreakerConfig = defaultConfigCircuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerConfig customCircuitBreakerConfig = customConfigCircuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerConfig defaultRegistryCustomCircuitBreakerConfig = defaultRegistryCustomConfigCircuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerConfig lambdaCustomCircuitBreakerConfig = lambdaCustomConfigCircuitBreaker.getCircuitBreakerConfig();
        System.out.println(JSON.toJSONString(defaultCircuitBreakerConfig));
        System.out.println(JSON.toJSONString(customCircuitBreakerConfig));
        System.out.println(JSON.toJSONString(defaultRegistryCustomCircuitBreakerConfig));
        System.out.println(JSON.toJSONString(lambdaCustomCircuitBreakerConfig));

        // 使用熔断器的静态方法生成熔断器
        CircuitBreaker staticDefaultCircuitBreaker = CircuitBreaker.ofDefaults("defaultCircuitBreaker");
        CircuitBreaker staticCustomConfigCircuitBreaker = CircuitBreaker.of("customConfigCircuitBreaker", customCircuitBreakerConfig);
        CircuitBreaker staticLambdaCustomCircuitBreaker = CircuitBreaker.of("lambdaCustomCircuitBreaker", TestController::buildCustomCircuitBreakerConfig);

        // 打印输出由静态方法生成的熔断器的配置
        CircuitBreakerConfig staticDefaultCircuitBreakerConfig = staticDefaultCircuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerConfig staticCustomConfigCircuitBreakerConfig = staticCustomConfigCircuitBreaker.getCircuitBreakerConfig();
        CircuitBreakerConfig staticLambdaCustomCircuitBreakerConfig = staticLambdaCustomCircuitBreaker.getCircuitBreakerConfig();
        System.out.println(JSON.toJSONString(staticDefaultCircuitBreakerConfig));
        System.out.println(JSON.toJSONString(staticCustomConfigCircuitBreakerConfig));
        System.out.println(JSON.toJSONString(staticLambdaCustomCircuitBreakerConfig));

    }

    private static CircuitBreakerConfig buildCustomCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .slidingWindowType(COUNT_BASED)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(RuntimeException.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();
    }
}
