package org.openrdf.repository.object.composition.helpers;

import static java.lang.reflect.Modifier.isPublic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.annotations.InstancePrivate;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.annotations.Precedes;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;

public class BehaviourConstructor implements BehaviourFactory {
	private static Set<String> special = new HashSet<String>(Arrays.asList(
			"groovy.lang.GroovyObject", RDFObjectBehaviour.class.getName()));
	private final Class<?> behaviourClass;
	private final Constructor<?> constructor;

	public BehaviourConstructor(Class<?> behaviourClass)
			throws NoSuchMethodException {
		this.behaviourClass = behaviourClass;
		Constructor<?> constructor;
		try {
			constructor = behaviourClass.getConstructor(Object.class);
		} catch (NoSuchMethodException e) {
			try {
				constructor = behaviourClass.getConstructor();
			} catch (NoSuchMethodException exc) {
				String msg = exc.getMessage() + " no default constructor in "
						+ behaviourClass;
				throw new NoSuchMethodException(msg);
			}
		}
		this.constructor = constructor;
	}

	public String toStritng() {
		return getName();
	}

	public String getName() {
		return getBehaviourType().getSimpleName();
	}

	public Class<?> getBehaviourType() {
		return behaviourClass;
	}

	public Class<?>[] getInterfaces() {
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
		addInterfaces(behaviourClass, interfaces);
		return  interfaces.toArray(new Class<?>[interfaces.size()]);
	}

	public boolean precedes(Method in, BehaviourFactory factory, Method to) {
		if (Advice.class.isAssignableFrom(to.getDeclaringClass()))
			return in.isAnnotationPresent(ParameterTypes.class);
		return overrides(getBehaviourType(), factory.getBehaviourType(), false,
				new HashSet<Class<?>>());
	}

	public Method[] getMethods() {
		return behaviourClass.getMethods();
	}

	public Method getInvocation(Method method) {
		Class<?>[] types = getParameterTypes(method);
		try {
			Method m = behaviourClass.getMethod(method.getName(), types);
			if (!isSpecial(m))
				return m;
		} catch (NoSuchMethodException e) {
			// look at @parameterTypes
		}
		for (Method m : behaviourClass.getMethods()) {
			if (m.getName().equals(method.getName())) {
				ParameterTypes ann = m.getAnnotation(ParameterTypes.class);
				if (ann != null && Arrays.equals(ann.value(), types)
						&& !isSpecial(m))
					return m;
			}
		}
		return null;
	}

	public Object newInstance(Object composed) throws Throwable {
		try {
			if (constructor.getParameterTypes().length == 0)
				return constructor.newInstance();
			return constructor.newInstance(composed);
		} catch (IllegalArgumentException e) {
			throw new AssertionError(e);
		} catch (InstantiationException e) {
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			IllegalAccessError error = new IllegalAccessError(e.toString());
			error.initCause(e);
			throw error;
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	@Override
	public String toString() {
		return behaviourClass.toString();
	}

	@Override
	public int hashCode() {
		return behaviourClass.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BehaviourConstructor other = (BehaviourConstructor) obj;
		return behaviourClass.equals(other.behaviourClass);
	}

	private Class<?>[] getParameterTypes(Method m) {
		if (m.isAnnotationPresent(ParameterTypes.class))
			return m.getAnnotation(ParameterTypes.class).value();
		return m.getParameterTypes();
	}

	private boolean isSpecial(Method m) {
		if (isObjectMethod(m))
			return true;
		if ("methodMissing".equals(m.getName()))
			return true;
		if ("propertyMissing".equals(m.getName()))
			return true;
		return m.isAnnotationPresent(InstancePrivate.class);
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}

	private boolean overrides(Class<?> a, Class<?> b,
			boolean explicit, Collection<Class<?>> exclude) {
		if (b.equals(a))
			return false;
		if (exclude.contains(a))
			return false;
		exclude.add(a);
		Precedes ann = a.getAnnotation(Precedes.class);
		if (ann == null)
			return false;
		Class<?>[] values = ann.value();
		for (Class<?> c : values) {
			if (c.equals(b))
				return true;
			if (c.isAssignableFrom(b))
				return explicit || !overrides(b, c, true, new HashSet<Class<?>>());
			if (overrides(c, b, explicit, exclude))
				return explicit || !overrides(b, c, true, new HashSet<Class<?>>());
		}
		return false;
	}

	private void addInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
		if (interfaces.contains(clazz))
			return;
		if (clazz.isInterface()) {
			interfaces.add(clazz);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			addInterfaces(superclass, interfaces);
		}
		for (Class<?> face : clazz.getInterfaces()) {
			if (isPublic(face.getModifiers()) && !isSpecial(face)) {
				addInterfaces(face, interfaces);
			}
		}
	}

	private boolean isSpecial(Class<?> face) {
		return special.contains(face.getName());
	}

}
