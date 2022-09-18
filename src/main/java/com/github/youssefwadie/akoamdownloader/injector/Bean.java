package com.github.youssefwadie.akoamdownloader.injector;

import java.util.Objects;

class Bean {
    private final Class<?> beanClass;
    private final String name;
    private Object instance;
    public Bean(Class<?> beanClass) {
        this.beanClass = beanClass;
        this.name = beanName();
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bean bean = (Bean) o;
        return Objects.equals(beanClass, bean.beanClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanClass);
    }

    private String beanName() {
        String beanSimpleName = this.beanClass.getSimpleName();
        return Character.toLowerCase(beanSimpleName.charAt(0)) + beanSimpleName.substring(1);
    }
}
