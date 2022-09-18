package com.github.youssefwadie.akoamdownloader.injector;

import com.github.youssefwadie.akoamdownloader.injector.annotations.DependsOn;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Container implements AutoCloseable {
    private final Map<Class<?>, Bean> classToBean = new HashMap<>();

    public <T> T getBean(Class<T> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (classToBean.containsKey(clazz)) {
            Bean bean = classToBean.get(clazz);
            return (T) bean.getInstance();
        }

        Constructor<?> constructor = getFirstConstructor(clazz);
        List<Object> constructorArguments = new ArrayList<>(constructor.getParameterCount());

        for (Parameter parameter : constructor.getParameters()) {
            Class<?> parameterType = parameter.getType();
            if (!classToBean.containsKey(parameterType)) {
                getBean(parameterType);
            }
            Bean bean = classToBean.get(parameterType);
            if (!parameter.isAnnotationPresent(DependsOn.class)) {
                constructorArguments.add(bean.getInstance());
            } else {
                DependsOn dependsOn = parameter.getAnnotation(DependsOn.class);
                if (!dependsOn.value().equals(bean.getName())) {
                    throw new RuntimeException(String.format("Failed to satisfy the dependencies for %s", clazz.getName()));
                }
                constructorArguments.add(bean.getInstance());
            }

        }
        Bean newBean = new Bean(clazz);
        constructor.setAccessible(true);
        newBean.setInstance(constructor.newInstance(constructorArguments.toArray()));
        classToBean.put(clazz, newBean);
        return (T) classToBean.get(clazz).getInstance();
    }


    private Constructor<?> getFirstConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new IllegalStateException(String.format("No constructor has been found for class %s", clazz.getName()));
        }

        return constructors[0];
    }


    @Override
    public void close() throws Exception {
        for (Bean bean : classToBean.values()) {
            invokeMethodsOf(bean, Closeable.class);
        }
    }

    private void invokeMethodsOf(Bean bean, Class<?> interfaceClass) throws InvocationTargetException, IllegalAccessException {
        if (!implementsInterface(bean.getBeanClass(), interfaceClass)) return;
        for (Method method : interfaceClass.getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
                method.invoke(bean.getInstance());
            }
        }
    }

    private boolean implementsInterface(Class<?> clazz, Class<?> interfaceClass) {
        return interfaceClass.isAssignableFrom(clazz);
    }
}
