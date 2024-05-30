package com.jasonfitch.test.springboot3.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
public class SessionCallbackGenericTypeTest {

    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-value";

    @Autowired
    private SimpleRedisClient simpleRedisClient;

    @BeforeEach
    public void beforeTestMethod() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();
        template.delete(TEST_KEY);
    }

    @Test
    public void testRawWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        //TODO how to implement type safely instead of using raw type for ValueOperations?
        // https://docs.spring.io/spring-data/redis/reference/redis/transactions.html
        String rawValue = template.execute(new SessionCallback<>() {
            @Override
            public String execute(RedisOperations operations) throws DataAccessException {
                ValueOperations opsForValue = operations.opsForValue();

                opsForValue.set(TEST_KEY, TEST_VALUE);
                String value = (String) opsForValue.get(TEST_KEY);
                return value;
            }
        });

        System.out.println("rawValue=" + rawValue);
        Assertions.assertEquals(TEST_VALUE, rawValue);
    }

    @Test
    public void testCastWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        //TODO how to implement type safely instead of casting type for ValueOperations?
        // https://stackoverflow.com/questions/21664487/how-to-implement-transaction-in-spring-data-redis-in-a-clean-way
        String castValue = template.execute(new SessionCallback<>() {
            @Override
            public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
                ValueOperations<String, String> opsForValue = (ValueOperations<String, String>) operations.opsForValue();

                opsForValue.set(TEST_KEY, TEST_VALUE);
                String value = opsForValue.get(TEST_KEY);
                return value;
            }
        });

        System.out.println("castValue=" + castValue);
        Assertions.assertEquals(TEST_VALUE, castValue);
    }

    @Test
    public void testSafetyWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        // 在这个实现中，这里 SessionCallback 回调内部的 【RedisOperations<K, V> operations】 和外部调用者 【template】 必定是同一个对象吗？
        // 答案，是的，具体见下面方法的【return session.execute(this);】 语句，所以我们可以采用这种方法来实现类型安全，
        // @see org.springframework.data.redis.core.RedisTemplate.execute(org.springframework.data.redis.core.SessionCallback<T>)
        // 不过这种方法
        //    从本质上忽略了回调入参 【operations】的使用，所以也就不关心回调方法级别上面的泛型信息了，即其压根就没有使用在该回调方法层级上的泛型信息
        //    但是他并不是完全忽略了 SessionCallback 所提供的泛型信息，其还是使用了 SessionCallback 在类层级上的泛型信息，用以指定最终的返回值类型
        // 另外该方法存在限制，其需要 SessionCallback 知道执行他的 RedisTemplate 是哪个对象，并能访问到其对象的引用，这样子我们就知道其泛型的真实类型了
        String safetyValue = template.execute(new SessionCallback<>() {
            @Override
            public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
                // 这里不用使用 cast 的原因是【public class StringRedisTemplate extends RedisTemplate<String, String>】
                RedisTemplate<String, String> stringStringRedisTemplate = template;
                ValueOperations<String, String> opsForValue = stringStringRedisTemplate.opsForValue();
                opsForValue.set(TEST_KEY, TEST_VALUE);
                String value = opsForValue.get(TEST_KEY);
                return value;
            }
        });

        System.out.println("safetyValue=" + safetyValue);
        Assertions.assertEquals(TEST_VALUE, safetyValue);
    }

    @Test
    public void testGenericWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        //TODO how to implement type safely SessionCallback with RedisTemplate?
        // 这里的核心问题就是接口中的方法级别指定了类型参数变量，而我们这里没法传递这个类型参数变量到接口的方法中
        // 这里编译器
        //    提示1 【'execute(RedisOperations<String, String>)' in 'Anonymous class derived from org.springframework.data.redis.core.SessionCallback'
        //    clashes with 'execute(RedisOperations<K, V>)' in 'org.springframework.data.redis.core.SessionCallback';
        //    both methods have same erasure, yet neither overrides the other】
        //    提示2 【Method does not override method from its superclass】
        // 解析编译器的提示，
        //    这里我们现在就清楚了，【RedisOperations<String, String>)】 类型擦除后成为【RedisOperations<Object, Object>)】，
        //    而【execute(RedisOperations<K, V>)】 也是一样的，作为方法的重载，他们之间是相同这个是合理的，没有问题
        //    但是由于 【execute(RedisOperations<K, V>)】 是接口中的方法，而我们这里的 【execute(RedisOperations<String, String>)】 并不是重载接口中的方法，故【提示2】
        //    所以此时的 【execute(RedisOperations<String, String>)】 是内部子类的一个新方法，此时他就和接口中的 【execute(RedisOperations<K, V>)】 方法在类型擦除后是一样的了，故【提示1】
//        String unsafeValue = template.execute(new SessionCallback<>() {
//            @Override
//            public <K, V> String execute(RedisOperations<String, String> operations) throws DataAccessException {
//                ValueOperations<String, String> opsForValue = operations.opsForValue();
//                opsForValue.set(TEST_KEY, TEST_VALUE);
//                String value = opsForValue.get(TEST_KEY);
//                return value;
//            }
//        });
    }

    @Test
    public void testWrapperWithSessionCallback() {
        StringRedisTemplate template = simpleRedisClient.getTemplate();

        SessionCallbackWrapper<String, String, StringRedisTemplate, Boolean> sessionCallbackWrapper = new SessionCallbackWrapper<>(template, TEST_KEY, TEST_VALUE);
        Boolean wrapperValue = template.execute(sessionCallbackWrapper);

        System.out.println("wrapperValue=" + wrapperValue);
        Assertions.assertFalse(wrapperValue);
    }

    private record SessionCallbackWrapper<DK, DV, T extends RedisOperations<DK, DV>, R>(T template, DK key, DV value)
            implements SessionCallback<R> {

        public R doExecute() throws DataAccessException {
            ValueOperations<DK, DV> opsForValue = template.opsForValue();
            opsForValue.set(key, value);
            DV getValue = opsForValue.get(key);

            Boolean isNull = getValue == null;
            return (R) isNull;
        }

        @Override
        public <K, V> R execute(RedisOperations<K, V> unused) throws DataAccessException {
            // 上面我们分析过，其实 SessionCallback 的回调方法中的参数对象，总是和在外部执行该回调的 template 对象是同一个对象实例
            Assertions.assertEquals(template, unused);
            return doExecute();
        }

    }

    /**
     * ******************************************************************************************************************
     * ******************************************************************************************************************
     * 以下的类用于说明类型参数变量在方法级别和在类级别上面的区别，并不是本测试用例执行测试时需要使用的类
     * ******************************************************************************************************************
     * ******************************************************************************************************************
     */

    private static class MethodLevelTypeArgumentSessionCallback implements SessionCallback<String> {

        // 这里我们发现如果一个接口中的方法中定义了方法级别的类型参数，那么该接口实现类中对应的重载方法，也必须使用相同的类型参数才行
        // 只要我们将其替换为具体的类型，那么编译器就会提示【Method does not override method from its superclass】报错，
        // 这说明了，该方法已经不是重载了接口中的方法，而是一个新的方法，这并不是我们期望的结果
//        @Override
//        public String execute(RedisOperations<String, String> operations) throws DataAccessException {
//            return null;
//        }

        //TODO how to implement concrete type instead of using type parameter variable?
        @Override
        public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
            return null;
        }

    }

    private static class ClasLevelsTypeArgumentSessionCallback<K, V> implements SessionCallback<String> {

        // 注意，方法上定义的类型参数变量<K, V>, 和在类上面定义的类型参数变量<K, V> 是两个不同的概念
        // 如果他们同时定义了相同名字的类型参数变量，则
        // 对于方法，编译器提示【Type parameter 'K' hides type parameter 'K' 】
        // 对于类，编译器提示【Type parameter 'K' is never used 】
        // 所以我们要注意，不要在类和方法上同时定义相同名字的类型参数变量
        @Override
        public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
            return null;
        }

        // 而这个方法，是一个新的方法，而不是重载接口中的方法，其没有定义方法级别的类型参数变量，
        // 其仅仅是在能够接受类型参数变量的 RedisOperations 类上面使用使用类上定义的类型参数变量
        // 此时 idea 不再提示类级别的类型参数变量没有被使用了，也就是说方法级别的类型参数变量比类级别的类型参数变量优先级更高
//        public String executeNotOverride(RedisOperations<K, V> operations) throws DataAccessException {
//            return null;
//        }

        // 这里我们意识到 【<String,String> execute(RedisOperations<String,String> operations)】 的含义是
        // 首先定义两个类型参数变量，他们的名字都是 String ，然后 RedisOperations 使用这两个类型参数变量
        // 而不是我们误解的将 RedisOperations 的类型参数变量都替换为真实的 java.lang.String 类型
        // 所以这里有下面的编译器提示
        // 对于【Duplicate type parameter: 'String'】说明在方法级别的类型参数变量中，是不能重复定义相同名字的类型参数变量
        // 对于【Method does not override method from its superclass】说明本方法的类型参数变量和接口中的类型参数变量不一致，所以不是方法重载
//        @Override
//        public <String, String> String execute(RedisOperations<String, String> operations) throws DataAccessException {
//            return null;
//        }

    }

}
