package org.openrdf.repository.object.composition;

import java.lang.reflect.Method;

/**
 * Behaviour mixin constructor.
 * 
 * @author James Leigh
 * 
 * @param <B>
 *            behaviour class
 */
public interface BehaviourFactory {

	/**
	 * An short name to reasonably distinguish it from similar behaviours.
	 * 
	 * @return short name
	 */
	String getName();

	/**
	 * Type of behaviour that the {@link #newInstance(Object)} will implement.
	 * 
	 * @return the type of behaviour
	 */
	Class<?> getBehaviourType();

	/**
	 * Traits that these behaviours provides.
	 * 
	 * @return array of java interfaces
	 */
	Class<?>[] getInterfaces();

	/**
	 * Public methods these behaviours provide. This includes methods from
	 * {@link #getInterfaces()} that these behaviours provides.
	 * 
	 * @return array of public methods
	 */
	Method[] getMethods();

	/**
	 * The method implemented by {@link #getBehaviourType()} that is to be
	 * invoked when the given method is called. If these behaviours do not
	 * provide an implementation for the given method, return null.
	 * 
	 * @param method
	 * @return method implemented by the return value of
	 *         {@link #getBehaviourType()} or null
	 */
	Method getInvocation(Method method);

	/**
	 * If these behaviours should always be invoked before behaviours of the
	 * given factory.
	 * 
	 * @param invocation
	 *            the method returned from {@link #getInvocation(Method)}
	 * @param factory
	 *            an alternative set of behaviours
	 * @param to
	 *            the method returned from the given factory
	 * @return false if no preference
	 */
	boolean precedes(Method invocation, BehaviourFactory factory, Method to);

	/**
	 * If this factory always returns a single instance.
	 * @return <code>true</code> if {@link #getSingleton()} should be called
	 */
	boolean isSingleton();

	/**
	 * The single behaviour that this factory produces.
	 * 
	 * @return singleton instance of {@link #isSingleton()} returns <code>true</code>
	 */
	Object getSingleton();

	/**
	 * New behaviour implementation for the given proxy object.
	 * 
	 * @param composed
	 * @return
	 * @throws Throwable
	 */
	Object newInstance(Object proxy) throws Throwable;

}
