package com.tyron.builder.internal.service;

import com.tyron.builder.internal.Cast;
import com.tyron.builder.internal.reflect.service.UnknownServiceException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachingServiceLocator implements ServiceLocator {
    private final ServiceLocator delegate;
    private final Map<Class<?>, DefaultServiceLocator.ServiceFactory<?>> serviceFactories = new HashMap<Class<?>, DefaultServiceLocator.ServiceFactory<?>>();
    private final Map<Class<?>, Object> services = new HashMap<Class<?>, Object>();
    private final Map<Class<?>, List<?>> allServices = new HashMap<Class<?>, List<?>>();

    public static CachingServiceLocator of(ServiceLocator other) {
        return new CachingServiceLocator(other);
    }

    private CachingServiceLocator(ServiceLocator delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized <T> DefaultServiceLocator.ServiceFactory<T> findFactory(Class<T> serviceType) {
        if (serviceFactories.containsKey(serviceType)) {
            return Cast.uncheckedCast(serviceFactories.get(serviceType));
        }
        DefaultServiceLocator.ServiceFactory<T> factory = delegate.findFactory(serviceType);
        serviceFactories.put(serviceType, factory);
        return factory;
    }

    @Override
    public synchronized <T> T get(Class<T> serviceType) throws UnknownServiceException {
        if (services.containsKey(serviceType)) {
            return Cast.uncheckedCast(services.get(serviceType));
        }
        T t = delegate.get(serviceType);
        services.put(serviceType, t);
        return t;
    }

    @Override
    public synchronized <T> List<T> getAll(Class<T> serviceType) throws UnknownServiceException {
        if (allServices.containsKey(serviceType)) {
            return Cast.uncheckedCast(allServices.get(serviceType));
        }
        List<T> all = delegate.getAll(serviceType);
        allServices.put(serviceType, all);
        return all;
    }

    @Override
    public synchronized <T> DefaultServiceLocator.ServiceFactory<T> getFactory(Class<T> serviceType) throws UnknownServiceException {
        DefaultServiceLocator.ServiceFactory<T> factory = findFactory(serviceType);
        if (factory == null) {
            throw new UnknownServiceException(serviceType, String.format("Could not find meta-data resource 'META-INF/services/%s' for service '%s'.", serviceType.getName(), serviceType.getName()));
        }
        return factory;
    }
}
