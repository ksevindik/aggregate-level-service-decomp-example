package com.example.clubservice;

import org.openjdk.jol.vm.VM;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

public class MapProxy<K, V> implements InvocationHandler {

    private Map<K, V> map;

    private MapProxy(Map<K, V> map) {
        this.map = map;
    }

    public static <K, V> Map<K, V> createProxy(Map<K, V> map) {
        return (Map<K, V>) Proxy.newProxyInstance(
                map.getClass().getClassLoader(),
                new Class<?>[]{Map.class},
                new MapProxy<>(map)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        /*
        System.out.println(">>>intercepted method: " + method.getName());
        System.out.println(">>>intercepted args: " + Arrays.toString(args));
        System.out.println(">>>thread: " + Thread.currentThread().getName());
        System.out.println(">>>map: " + VM.current().addressOf(map));
        System.out.println(">>>map content: " + map);
         */
        return method.invoke(map, args);
    }
}
