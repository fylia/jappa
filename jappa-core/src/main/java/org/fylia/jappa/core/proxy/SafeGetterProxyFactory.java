package org.fylia.jappa.core.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

/**
 * Creates Proxies to objects for which getters should not be called unless the setter has been called. 
 */
public final class SafeGetterProxyFactory {
    private static final String SETTER_PREFIX="set";
    private static final String GETTER_PREFIX="get";
    private static final String BOOLEAN_GETTER_PREFIX="is";
    private static final GetterAndSetterMethodFilter GETTER_AND_SETTER_METHOD_FILTER = new GetterAndSetterMethodFilter(); 
    private static final Map<Class<?>,ProxyFactory> PROXY_FACTORIES_CACHE = new HashMap<Class<?>, ProxyFactory>();

    /**
     * Exception thrown when a getter for not loaded property is called
     */
    public static final class NotSetPropertyException extends RuntimeException {
        private static final long serialVersionUID = -3436661533600742443L;

        /**
         * Constructor
         * @param m the (getter) method that was not allowed
         */
        public NotSetPropertyException(Method m) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            super("Calling method "+m.getName()+" on Object of "+m.getDeclaringClass()+" not allowed. Property not set.");
        }
    }

    /**
     * Handler for calls to not allowed getters and setters
     */
    private static final class NotAllowedGetterAndSetterHandler implements
            MethodHandler {
        private final Set<String> allowedGetters = new HashSet<>();
        
        public Object invoke(Object self, Method m, Method proceed,
                             Object[] args) {
            final String methodName = m.getName();
            if (methodName.startsWith(SETTER_PREFIX)) {
                allowedGetters.add(methodName.replace(SETTER_PREFIX, GETTER_PREFIX));
                allowedGetters.add(methodName.replace(SETTER_PREFIX, BOOLEAN_GETTER_PREFIX));
            }
            try {
            	final Object returnValue = proceed.invoke(self, args);
            	if (returnValue == null && !allowedGetters.contains(methodName) && !m.isAnnotationPresent(SafeGetter.class)) {
            		throw new NotSetPropertyException(m);
            	}
                return returnValue;
            } catch (IllegalArgumentException|IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException("Error calling method "+m.getName()+" on Object of "+m.getDeclaringClass(),e);
            }
        }
    }
    
    /**
     * Filter for finding not allowed getters and setters
     */
    private static final class GetterAndSetterMethodFilter implements MethodFilter{
        public boolean isHandled(Method m) {
        	return (isSetter(m.getName()) && Void.class.equals(m.getReturnType()) && m.getParameterCount()==1) ||
        		(isGetter(m.getName()) && !Void.class.equals(m.getReturnType()) && m.getParameterCount()==0);
        }
        
        /**
         * Checks if a method is a setter
         * @param methodName the name of the method
         * @return if the method starts with the setter prefix
         */
        private boolean isSetter(String methodName) {
            return methodName.startsWith(SETTER_PREFIX);
        }
        
        /**
         * Checks if the method is a getter
         * @param methodName the name of the method
         * @return if the method is a getter
         */
        private boolean isGetter(String methodName) {
            if (!methodName.startsWith(GETTER_PREFIX) && !methodName.startsWith(BOOLEAN_GETTER_PREFIX)) {
                return false;
            }
            return true;
        }
    }
    
    /**
     * Private constructor to avoid instantiation
     */
    private SafeGetterProxyFactory() {}
    
    /**
     * Retrieve a {@link ProxyFactory} for the given class. A filter is added to the class
     * on all getters and setters
     * @param clazz the class to get a {@link ProxyFactory} for
     * @return the {@link ProxyFactory}
     */
    public static ProxyFactory getProxyFactory(Class<?> clazz) {
        ProxyFactory f = PROXY_FACTORIES_CACHE.get(clazz);
        if (f==null) {
            f = new ProxyFactory();
            f.setSuperclass(clazz);
            f.setFilter(GETTER_AND_SETTER_METHOD_FILTER);
            PROXY_FACTORIES_CACHE.put(clazz, f);
        }
        return f;
        
    }
    
    /**
     * Get the base class of a proxied object
     * @param o the object
     * @return the base class of the proxied object (or the class of the object if it was no proxy)
     */
    public static Class<?> getClass(Object o) {
        if (!ProxyFactory.isProxyClass(o.getClass())) {
            return o.getClass();
        } else {
            return o.getClass().getSuperclass();
        }
    }
    /**
     * Creates a proxy to an object of the given class
     * @param <T> the type of the object to create
     * @param clazz the class of the object to create
     * @return the created object. Throws {@link RuntimeException} in case the creation fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> clazz) {
        final ProxyFactory pf = getProxyFactory(clazz);
        try {
        	ProxyObject proxy = (ProxyObject)pf.create(new Class[0], new Object[0]);
            proxy.setHandler(new NotAllowedGetterAndSetterHandler());
			return (T)proxy;
        } catch (IllegalArgumentException e) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            throw new RuntimeException("Error constructing element of "+clazz, e);
        } catch (NoSuchMethodException e) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            throw new RuntimeException("Error constructing element of "+clazz, e);
        } catch (InstantiationException e) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            throw new RuntimeException("Error constructing element of "+clazz, e);
        } catch (IllegalAccessException e) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            throw new RuntimeException("Error constructing element of "+clazz, e);
        } catch (InvocationTargetException e) {
            // CHECKSTYLE:OFF:StringLiteralEquality FOR 1 LINE            
            throw new RuntimeException("Error constructing element of "+clazz, e);
        }
    }

    /**
     * Annotation to be placed on getters that always may be invoked,
     * so even when this property was not loaded these annotated getters 
     * will not throw an {@link NotSetPropertyException}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface SafeGetter {
        
    }
}
