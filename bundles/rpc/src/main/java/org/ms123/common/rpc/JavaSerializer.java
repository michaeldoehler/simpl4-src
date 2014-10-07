/*
 Java JSON RPC
 RPC Java POJO by Novlog
 http://www.novlog.com

 This library is dual-licensed under the GNU Lesser General Public License (LGPL) and the Eclipse Public License (EPL).
 Check http://qooxdoo.org/license

 This library is also licensed under the Apache license.
 Check http://www.apache.org/licenses/LICENSE-2.0

 Contribution:
 This contribution is provided by Novlog company.
 http://www.novlog.com
 */
package org.ms123.common.rpc;

import java.lang.reflect.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class JavaSerializer {

	// Serialization filters
	protected List<String> fieldNamesToExcludeFromSerialization = null;

	protected Pattern fieldPatternToExcludeFromSerialization = null;

	// Unserialization filters
	protected List<String> fieldNamesToExcludeFromUnserialization = null;

	protected Pattern fieldPatternToExcludeFromUnserialization = null;

	public JavaSerializer() {
		this(null, null, null, null);
	}

	public JavaSerializer(final List<String> keysExcludedFromSerialization, final List<String> keysExcludedFromUnserialization) {
		this(keysExcludedFromSerialization, null, keysExcludedFromUnserialization, null);
	}

	public JavaSerializer(final String fieldPatternToExcludeFromSerialization, final String fieldPatternToExcludeFromUnserialization) {
		this(null, fieldPatternToExcludeFromSerialization, null, fieldPatternToExcludeFromUnserialization);
	}

	public JavaSerializer(List<String> fieldNamesToExcludeFromSerialization, String fieldPatternToExcludeFromSerialization, List<String> fieldNamesToExcludeFromUnserialization, String fieldPatternToExcludeFromUnserialization) {
		setFieldNamesToExcludeFromSerialization(fieldNamesToExcludeFromSerialization);
		setFieldPatternToExcludeFromSerialization(fieldPatternToExcludeFromSerialization);
		setFieldNamesToExcludeFromUnserialization(fieldNamesToExcludeFromUnserialization);
		setFieldPatternToExcludeFromUnserialization(fieldPatternToExcludeFromUnserialization);
	}

	protected void setFieldNamesToExcludeFromSerialization(final List<String> fieldNames) {
		if (fieldNames != null) {
			this.fieldNamesToExcludeFromSerialization = fieldNames;
		} else {
			this.fieldNamesToExcludeFromSerialization = new ArrayList<String>(0);
		}
	}

	protected void setFieldPatternToExcludeFromSerialization(final String regexp) {
		if (regexp != null) {
			this.fieldPatternToExcludeFromSerialization = Pattern.compile(regexp);
		}
	}

	protected void setFieldNamesToExcludeFromUnserialization(final List<String> fieldNames) {
		if (fieldNames != null) {
			this.fieldNamesToExcludeFromUnserialization = fieldNames;
		} else {
			this.fieldNamesToExcludeFromUnserialization = new ArrayList<String>(0);
		}
	}

	protected void setFieldPatternToExcludeFromUnserialization(String regexp) {
		if (regexp != null) {
			this.fieldPatternToExcludeFromUnserialization = Pattern.compile(regexp);
		}
	}

	/* Serialization/unserialization rules */
	protected boolean isFieldValidForUnserialization(final Field aField) {
		return !isFieldTransient(aField) && !(fieldPatternToExcludeFromUnserialization != null && fieldPatternToExcludeFromUnserialization.matcher(aField.getName()).matches());
	}

	protected boolean isFieldValidForSerialization(final Field aField) {
		return !isFieldTransient(aField) && !isFieldStatic(aField) && !fieldNamesToExcludeFromSerialization.contains(aField.getName()) && !(fieldPatternToExcludeFromSerialization != null && fieldPatternToExcludeFromSerialization.matcher(aField.getName()).matches());
	}

	/* --------------- */
	/* UNSERIALIZATION */
	/* --------------- */
	public <T> T unserialize(final Object obj, final Class<T> targetClass) throws UnserializationException {
		T result = null;
		if (obj == null) {
			if (!targetClass.isPrimitive()) {
				result = null;
			} else {
				throw new UnserializationException("Cannot assign null to primitive type " + targetClass.getName());
			}
		} else {
			if (obj instanceof List) {
				// The object must represent an array, a list, or a set
				final List listObj = (List) obj;
				if (targetClass.isArray()) {
					final Class componentType = targetClass.getComponentType();
					final Object arrayRes = Array.newInstance((componentType == null ? Object.class : componentType), listObj.size());
					for (int i = 0; i < listObj.size(); ++i) {
						final Object value = unserialize(listObj.get(i), componentType);
						Array.set(arrayRes, i, value);
					}
					result = (T) arrayRes;
				} else if (targetClass == Object.class) {
					result = (T) obj;
				} else {
					if (List.class.isAssignableFrom(targetClass)) {
						result = (T) obj;
					} else {
						throw new UnserializationException("Cannot unserialize the provided java.util.List to something else than array. Wanted destination is " + targetClass.getName());
					}
				}
			} else if (obj instanceof Map) {
				// the serialized object represents a Map or a complex object
				if (Map.class.isAssignableFrom(targetClass)) {
					// the source map represents a map
					//throw new UnserializationException("Cannot unserialize " + targetClass.getName() + " to a map");
					result = (T) obj;
				} else {
					// the source map represents an object
					result = unserializeObject((Map<String, Object>) obj, targetClass);
				}
			} else {
				// The object is a primitive or a string
				final Class sourceType = obj.getClass();
				if (targetClass.isAssignableFrom(sourceType)) {
					result = (T) obj;
				} else if (sourceType == String.class) {
					final String s = (String) obj;
					if (targetClass == Integer.class || targetClass == Integer.TYPE) {
						result = (T) Integer.valueOf(s);
					} else if (targetClass == Short.class || targetClass == Short.TYPE) {
						result = (T) Short.valueOf(s);
					} else if (targetClass == Long.class || targetClass == Long.TYPE) {
						result = (T) Long.valueOf(s);
					} else if (targetClass == Float.class || targetClass == Float.TYPE) {
						result = (T) Float.valueOf(s);
					} else if (targetClass == Double.class || targetClass == Double.TYPE) {
						result = (T) Double.valueOf(s);
					} else if (targetClass == Byte.class || targetClass == Byte.TYPE) {
						result = (T) Byte.valueOf(s);
					} else if (targetClass.isEnum()) {
						result = (T) Enum.valueOf((Class<Enum>) targetClass, s);
					} else {
						throw new UnserializationException("Unable to convert " + sourceType.getName() + " to " + targetClass.getName());
					}
				} else if ((Number.class.isAssignableFrom(targetClass) || isPrimitiveNumber(targetClass)) && (Number.class.isAssignableFrom(sourceType) || isPrimitiveNumber(sourceType))) {
					if (targetClass == Integer.class || targetClass == Integer.TYPE) {
						result = (T) new Integer(((Number) obj).intValue());
					} else if (targetClass == Short.class || targetClass == Short.TYPE) {
						result = (T) new Short(((Number) obj).shortValue());
					} else if (targetClass == Long.class || targetClass == Long.TYPE) {
						result = (T) new Long(((Number) obj).longValue());
					} else if (targetClass == Float.class || targetClass == Float.TYPE) {
						result = (T) new Float(((Number) obj).floatValue());
					} else if (targetClass == Double.class || targetClass == Double.TYPE) {
						result = (T) new Double(((Number) obj).doubleValue());
					} else if (targetClass == Byte.class || targetClass == Byte.TYPE) {
						result = (T) new Byte(((Number) obj).byteValue());
					} else {
						throw new UnserializationException("Unable to convert " + sourceType.getName() + " to " + targetClass.getName());
					}
				} else if (Boolean.class.isAssignableFrom(sourceType) && (targetClass == Boolean.class || targetClass == Boolean.TYPE)) {
					result = (T) new Boolean((Boolean) obj);
				} else {
					throw new UnserializationException("Unable to convert " + sourceType.getName() + " to " + targetClass.getName());
				}
			}
		}
		return result;
	}

	public Object unserialize(final Object obj, final Class targetClass, final Type targetType) throws UnserializationException {
		Object result = null;
		if (targetType instanceof Class) {
			result = unserialize(obj, targetClass);
		} else if (targetType instanceof GenericArrayType) {
			result = unserialize(obj, targetClass);
		} else if (obj != null) {
			if (targetType instanceof ParameterizedType) {
				final ParameterizedType parameterizedType = (ParameterizedType) targetType;
				final Type[] typeArguments = parameterizedType.getActualTypeArguments();
				if (Map.class.isAssignableFrom(targetClass)) {
					if (obj instanceof Map) {
						result = unserializeMap((Map<String, Object>) obj, targetClass, typeArguments);
					} else {
						throw new UnserializationException("Cannot unserialize object to a Map. The serialized object (" + obj.getClass().getName() + ") has not a map format.");
					}
				} else if (Collection.class.isAssignableFrom(targetClass)) {
					// we expect List, Set, or a 1D Collection
					if (obj instanceof List) {
						result = unserialize1DCollection((List) obj, targetClass, typeArguments);
					} else {
						throw new UnserializationException("Cannot unserialize object to a 1D collection. The serialized object (" + obj.getClass().getName() + ") has not a one dimension collection format.");
					}
				} else {
					// We have a a parameterized class like Box<T>, or a Collection that is not a List/Set/Map
					throw new FeatureException("Missing feature: unserialization of parameterized types that are not 1D collections, like Box<T>. Unable to unserialize to " + targetType);
				}
			} else if (targetType instanceof WildcardType) {
				// WildcardType
				throw new FeatureException("Missing feature: unserialization of types parameterized by wildcards (like List<? super String>). Unable to unserialize to " + targetType);
			} else {
				// targetType instanceof TypeVariable
				throw new FeatureException("Missing feature: unserialization of type variables (like an attribute of type T in a Box<T> object). Unable to unserialize to " + targetType);
			}
		}
		return result;
	}

	protected <T> T unserializeObject(final Map<String, Object> mapObj, final Class<T> targetClass) throws UnserializationException {
		T result = null;
		try {
			result = targetClass.newInstance();
		} catch (InstantiationException e) {
			throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
		} catch (IllegalAccessException e) {
			throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
		}
		if (targetClass == Object.class) {
			result = (T) mapObj;
		} else {
			for (final String key : mapObj.keySet()) {
				// Skip excluded keys
				if (fieldNamesToExcludeFromUnserialization.contains(key)) {
					continue;
				}
				Field field = null;
				try {
					field = getDeclaredField(key, targetClass);
				} catch (NoSuchFieldException e) {
					throw new UnserializationException("Unable to unserialize \"" + key + "\" property because no such field exists in class " + targetClass.getName(), e);
				}
				if (isFieldValidForUnserialization(field)) {
					final Object fieldValue = unserialize(mapObj.get(key), field);
					field.setAccessible(true);
					try {
						field.set(result, fieldValue);
					} catch (IllegalAccessException e) {
						throw new UnserializationException("Unable to set " + field.getName() + " field value into the " + targetClass.getName() + " instance.", e);
					}
				} else {
					throw new UnserializationException("Unable to unserialize \"" + key + "\" property in class " + targetClass.getName() + " because this field is invalid for unserialization");
				}
			}
		}
		return result;
	}

	protected Object unserialize(final Object obj, final Field field) throws UnserializationException {
		final Type targetType = field.getGenericType();
		final Class<?> targetClass = field.getType();
		return unserialize(obj, targetClass, targetType);
	}

	private Map unserializeMap(final Map<String, Object> mapObj, final Class targetClass, final Type[] typeArguments) throws UnserializationException {
		Map result = null;
		Type keyType = null;
		Type valueType = null;
		if (typeArguments == null || typeArguments.length == 0) {
			// we treat the map as a map<String, Object>, which is what we already have
			result = mapObj;
		} else {
			result = getMap(targetClass);
			keyType = typeArguments[0];
			valueType = typeArguments[1];
			if (keyType != String.class) {
				// Since JSON only have string keys, we must have string keys in the destination map
				throw new UnserializationException("Unable to unserialize the given map to a " + targetClass.getName() + " object because " + targetClass.getName() + " doesn't have string keys (it has " + keyType + " ones)");
			}
			// mapObj represents something like Map<String, List<...>> or Map<String, Map<...>>
			if (valueType instanceof ParameterizedType) {
				final ParameterizedType p = (ParameterizedType) valueType;
				// valueType looks like List<T> or Map<U, V>
				Type rawType = p.getRawType();
				if (rawType instanceof Class) {
					final Class rawClass = (Class) rawType;
					// unserialize to Collection only if rawClass is a Collection (we can have Box<T> !)
					if (Collection.class.isAssignableFrom(rawClass)) {
						for (final String key : mapObj.keySet()) {
							final Object value = mapObj.get(key);
							if (value instanceof List) {
								final Object unserializedValue = unserialize1DCollection((List) value, rawClass, p.getActualTypeArguments());
								result.put(key, unserializedValue);
							} else if (value instanceof Map) {
								final Object unserializedValue = unserializeMap((Map<String, Object>) value, rawClass, p.getActualTypeArguments());
								result.put(key, unserializedValue);
							} else {
								throw new UnserializationException("Cannot unserialize the provided object to a " + rawClass.getName() + " object. The serialized object has not a collection format.");
							}
						}
					} else {
						// We do not have Collection<T> as a desired type, but something like Box<T>
						throw new FeatureException("Missing feature: unserialization of parameterized types that are not 1D collections, like Box<T>. Unable to unserialize to " + rawClass.getName());
					}
				} else {
					// raw type of valueType is not a Class
					throw new UnserializationException("Parameterized type of the unserialized " + targetClass.getName() + " object is not a valid class");
				}
			} else {
				// Map<String, Object[]>
				if (valueType instanceof GenericArrayType) {
					final Class valueClass = (Class) valueType;
					for (final String key : mapObj.keySet()) {
						final Object value = mapObj.get(key);
						final Object unserializedValue = unserialize(value, valueClass);
						result.put(key, unserializedValue);
					}
				} else {
					// Map<String, Object>
					if (valueType instanceof Class) {
						final Class valueClass = (Class) valueType;
						for (final String key : mapObj.keySet()) {
							final Object value = mapObj.get(key);
							final Object unserializedValue = unserialize(value, valueClass);
							result.put(key, unserializedValue);
						}
					} else if (valueType instanceof WildcardType) {
						// WildcardType
						throw new FeatureException("Missing feature: unserialization of types parameterized by wildcards (like List<? super String>). Unable to unserialize to " + valueType);
					} else {
						// valueType instanceof TypeVariable
						throw new FeatureException("Missing feature: unserialization of type variables (like an attribute of type T in a Box<T> object). Unable to unserialize to " + valueType);
					}
				}
			}
		}
		return result;
	}

	private Collection unserialize1DCollection(final List listObj, final Class targetClass, final Type[] typeArguments) throws UnserializationException {
		Collection result = get1DCollectionInstance(targetClass, listObj.size());
		if (typeArguments == null || typeArguments.length == 0) {
			// we treat the collection as a collection of Objects
			for (final Object elt : listObj) {
				final Object unserializedElt = unserialize(elt, Object.class);
				result.add(unserializedElt);
			}
		} else {
			final Type typeArg = typeArguments[0];
			// listObj represents a type that looks like List< List<...> > or List< Map<..., ...> >
			if (typeArg instanceof ParameterizedType) {
				final ParameterizedType p = (ParameterizedType) typeArg;
				final Type rawType = p.getRawType();
				if (rawType instanceof Class) {
					final Class rawClass = (Class) rawType;
					// unserialize to Collection only if rawClass is a Collection (we can have Box<T> !)
					if (Collection.class.isAssignableFrom(rawClass)) {
						for (final Object elt : listObj) {
							if (elt instanceof List) {
								final Object unserializedElt = unserialize1DCollection((List) elt, rawClass, p.getActualTypeArguments());
								result.add(unserializedElt);
							} else if (elt instanceof Map) {
								final Object unserializedElt = unserializeMap((Map<String, Object>) elt, rawClass, p.getActualTypeArguments());
								result.add(unserializedElt);
							} else {
								throw new UnserializationException("Cannot unserialize the provided object to a " + rawClass.getName() + " object. The serialized object has not a collection format.");
							}
						}
					} else {
						// We do not have Collection<T> as a desired type, but something like Box<T>
						throw new FeatureException("Missing feature: unserialization of parameterized types that are not 1D collections, like Box<T>. Unable to unserialize to " + rawClass.getName());
					}
				} else {
					// raw type of typeArg is not a Class
					throw new UnserializationException("Parameterized type of the unserialized " + targetClass.getName() + " object is not a valid class");
				}
			} else {
				// List<String[]>
				if (typeArg instanceof GenericArrayType) {
					final Class typeClass = (Class) typeArg;
					for (final Object elt : listObj) {
						final Object unserializedElt = unserialize(elt, typeClass);
						result.add(unserializedElt);
					}
				} else {
					// List<String>
					if (typeArg instanceof Class) {
						final Class typeClass = (Class) typeArg;
						for (final Object elt : listObj) {
							final Object unserializedElt = unserialize(elt, typeClass);
							result.add(unserializedElt);
						}
					} else if (typeArg instanceof WildcardType) {
						// WildcardType
						throw new FeatureException("Missing feature: unserialization of types parameterized by wildcards (like List<? super String>). Unable to unserialize to " + typeArg);
					} else {
						// typeArg instanceof TypeVariable
						throw new FeatureException("Missing feature: unserialization of type variables (like an attribute of type T in a Box<T> object). Unable to unserialize to " + typeArg);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates a one dimension collection that is an instance of targetClass.
	 *
	 * @param targetClass the class to instantiate. targetClasss should be a one dimension collection class
	 * @return A new object that is of targetClass type. If targetClass is abstract, the method will try to create an ArrayList or a HashSet if it s compatible with the requested class.
	 * @throws UnserializationException
	 */
	private Collection get1DCollectionInstance(final Class targetClass, final int collectionSize) throws UnserializationException {
		Collection res = null;
		if (!isAbstract(targetClass)) {
			try {
				res = (Collection) targetClass.newInstance();
			} catch (InstantiationException e) {
				throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
			} catch (IllegalAccessException e) {
				throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
			}
		} else if (List.class.isAssignableFrom(targetClass)) {
			if (targetClass.isAssignableFrom(ArrayList.class)) {
				res = new ArrayList(collectionSize);
			} else {
				throw new UnserializationException("Unable to create a " + targetClass.getName() + " object because " + targetClass.getName() + "is abstract");
			}
		} else if (Set.class.isAssignableFrom(targetClass)) {
			if (targetClass.isAssignableFrom(HashSet.class)) {
				res = new HashSet(collectionSize);
			} else {
				throw new UnserializationException("Unable to create a " + targetClass.getName() + " object because " + targetClass.getName() + "is abstract");
			}
		} else if (targetClass.isAssignableFrom(ArrayList.class)) {
			// e.g. Collection.class
			res = new ArrayList(collectionSize);
		} else {
			final StringBuilder builder = new StringBuilder();
			builder.append("Unable to unserialize the provided 1d collection to a ");
			builder.append(targetClass.getName());
			builder.append(" object because ");
			builder.append(targetClass.getName());
			builder.append(" is abstract.");
			throw new UnserializationException(builder.toString());
		}
		return res;
	}

	/**
	 * Creates a map that is an instance of targetClass.
	 *
	 * @param targetClass the class to instantiate. targetClasss should be a map class
	 * @return A new object that is of targetClass type. If targetClass is abstract, the method will try to create a HashMap if HashMap is compatible with the requested class.
	 * @throws UnserializationException
	 */
	private Map getMap(final Class targetClass) throws UnserializationException {
		Map res = null;
		if (Map.class.isAssignableFrom(targetClass)) {
			if (!isAbstract(targetClass)) {
				try {
					res = (Map) targetClass.newInstance();
				} catch (InstantiationException e) {
					throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
				} catch (IllegalAccessException e) {
					throw new UnserializationException("Unable to create a " + targetClass.getName() + " instance.", e);
				}
			} else if (targetClass.isAssignableFrom(HashMap.class)) {
				res = new HashMap();
			} else {
				throw new UnserializationException("Unable to create a " + targetClass.getName() + " object because " + targetClass.getName() + "is abstract");
			}
		} else {
			final StringBuilder builder = new StringBuilder();
			builder.append("Unable to unserialize the provided object to a ");
			builder.append(targetClass.getName());
			builder.append(" object because ");
			builder.append(targetClass.getName());
			builder.append(" is not a Map");
			throw new UnserializationException(builder.toString());
		}
		return res;
	}

	/* ------------- */
	/* SERIALIZATION */
	/* ------------- */
	public Object serialize(final Object obj) throws SerializationException {
		return serialize(obj, null,0,10);
	}

	public Object serialize(final Object obj, final Map<Class, List<String>> wantedFields,int deep,int maxDeep) throws SerializationException {
		if( deep >=maxDeep){
			System.out.println("JavaSerializer.serialize: maxDeep reached:"+obj);
			return obj;
		}
		Object res = null;
		if (obj != null) {
			if (obj instanceof Integer) {
				res = obj;
			} else if (obj instanceof Short) {
				res = obj;
			} else if (obj instanceof Long) {
				res = obj;
			} else if (obj instanceof Double) {
				res = obj;
			} else if (obj instanceof Float) {
				res = obj;
			} else if (obj instanceof Byte) {
				res = obj;
			} else if (obj instanceof Character) {
				res = obj;
			} else if (obj instanceof Boolean) {
				res = obj;
			} else if (obj instanceof BigDecimal) {
				res = ((BigDecimal)obj).doubleValue();
			} else if (obj instanceof BigInteger) {
				res = ((BigInteger)obj).intValue();
			} else if (obj instanceof String) {
				// Create a brand new String
				res = new String((String) obj);
			} else if (obj instanceof Date) {
				// Conversion of dates is done in the JSON string creation
				return obj;
			} else if (obj instanceof List) {
				final List listObj = (List) obj;
				final List<Object> listRes = new ArrayList<Object>(listObj.size());
				for (final Object elt : listObj) {
					listRes.add(serialize(elt, wantedFields,deep+1,maxDeep));
				}
				res = listRes;
			} else if (obj instanceof Set) {
				final Set setObj = (Set) obj;
				final List<Object> listRes = new ArrayList<Object>(setObj.size());
				for (final Object elt : setObj) {
					listRes.add(serialize(elt, wantedFields,deep+1,maxDeep));
				}
				res = listRes;
			} else if (obj instanceof Object[]) {
				final Object[] arrayObj = (Object[]) obj;
				final List<Object> listRes = new ArrayList<Object>(arrayObj.length);
				for (final Object elt : arrayObj) {
					listRes.add(serialize(elt, wantedFields,deep+1,maxDeep));
				}
				res = listRes;
			} else if (obj instanceof Map) {
				final HashMap<String, Object> mapRes = new HashMap<String, Object>();
				final Map mapObj = (Map) obj;
				for (final Object key : mapObj.keySet()) {
					final Object keyRes = serialize(key, wantedFields,deep+1,maxDeep);
					if (!(keyRes instanceof String)) {
						// TODO try to convert to a String?
						String clazzName = keyRes != null ? keyRes.getClass().getName() : "NULL";
						throw new SerializationException("Map key("+key+") is not a String but a " + clazzName+"/"+mapObj);
					}
					mapRes.put((String) keyRes, serialize(mapObj.get(key), wantedFields,deep+1,maxDeep));
				}
				res = mapRes;
			} else if (obj.getClass().isEnum()) {
				res = obj.toString();
			} else {
				final HashMap<String, Object> mapRes = new HashMap<String, Object>();
				final Class clazz = obj.getClass();
				final List<Field> fields = getDeclaredFields(clazz);
				for (final Field field : fields) {
					boolean serializeField;
					// wantedFields takes priority on other filters (list of excluded attributes/patterns, filter over transient fields)
					if (wantedFields != null) {
						serializeField = isFieldWanted(wantedFields, clazz, field.getName());
					} else {
						serializeField = isFieldValidForSerialization(field);
					}
					if (serializeField) {
						field.setAccessible(true);
						final Object fieldValue;
						try {
							fieldValue = field.get(obj);
						} catch (IllegalAccessException e) {
							throw new SerializationException("Unable to get value of field " + field.getName() + " in the " + obj.getClass().getName() + " instance.", e);
						}
						Object serializedValue = null;
						if (fieldValue != null) {
							if (field.getType().isEnum()) {
								serializedValue = fieldValue.toString();
							} else {
								serializedValue = serialize(fieldValue, wantedFields,deep+1,maxDeep);
							}
						}
						mapRes.put(field.getName(), serializedValue);
					}
				}
				res = mapRes;
			}
		}
		return res;
	}

	private boolean isFieldWanted(final Map<Class, List<String>> authorizedFields, final Class clazz, final String fieldName) {
		boolean result = false;
		final List<String> classFields = authorizedFields.get(clazz);
		if ((classFields != null && classFields.contains(fieldName))) {
			result = true;
		} else {
			final Class parent = clazz.getSuperclass();
			if (parent != null && parent != Object.class) {
				result = isFieldWanted(authorizedFields, parent, fieldName);
			}
		}
		return result;
	}

	/* ----- */
	/* UTILS */
	/* ----- */
	protected boolean isAbstract(Class aClass) {
		final int modifiers = aClass.getModifiers();
		return Modifier.isAbstract(modifiers);
	}

	protected boolean isPrimitiveNumber(Class aClass) {
		return aClass.isPrimitive() && aClass != Boolean.TYPE;
	}

	/**
	 * Returns a Field object that reflects the specified declared field of the aClass parameter. The name parameter is the name of the desired field.
	 * The desired field can be in the class itself, a superclass, or a superinterface. The field can be public/protected/private
	 * <p/>
	 * The field is determined by the algorithm that follows:
	 * 1. If aClass declares directly a field with the name specified, that is the field to be reflected.
	 * 2. If no field was found in step 1 above, this algorithm is applied recursively to each direct superinterface of aClass
	 * 3. If no field was found in steps 1 and 2 above, and aClass has a superclass S, then this algorithm is invoked recursively upon S. If aClass has no superclass, then a NoSuchFieldException is thrown.
	 *
	 * @param name   The name of the desired field.
	 * @param aClass The class or interface in which the field will be searched.
	 * @return The Field object for the specified name.
	 * @throws NoSuchFieldException if a field with the specified name has not been found.
	 */
	public Field getDeclaredField(String name, Class aClass) throws NoSuchFieldException {
		Field res = null;
		try {
			// step1: search if the class declares directly the field
			res = aClass.getDeclaredField(name);
		} catch (NoSuchFieldException e0) {
			// step2: if no field was found, apply recursively to each superinterface
			for (final Class c : aClass.getInterfaces()) {
				try {
					res = getDeclaredField(name, c);
				} catch (NoSuchFieldException e1) {
				}
			}
			// step3: search recursively inside the superclasses
			if (res == null) {
				final Class superclass = aClass.getSuperclass();
				// getSuperClass returns nulls when super class is Object
				if (superclass != null) {
					res = getDeclaredField(name, aClass.getSuperclass());
				} else {
					throw new NoSuchFieldException();
				}
			}
		}
		return res;
	}

	public List<Field> getDeclaredFields(Class classz) {
		List<Field> res = null;
		if (classz != null) {
			res = new ArrayList<Field>();
			Field[] fields = null;
			// get all fields of the class
			fields = classz.getDeclaredFields();
			res.addAll(Arrays.asList(fields));
			// get all fields of the superinterfaces
			for (Class c : classz.getInterfaces()) {
				fields = c.getDeclaredFields();
				res.addAll(Arrays.asList(fields));
			}
			// get all fields of the superclass
			final Class superclass = classz.getSuperclass();
			// getSuperClass returns nulls when super class is Object
			if (superclass != null) {
				List<Field> superFields = getDeclaredFields(superclass);
				res.addAll(superFields);
			}
		}
		return res;
	}

	protected boolean isFieldTransient(Field aField) {
		int modifiers = aField.getModifiers();
		return Modifier.isTransient(modifiers);
	}

	protected boolean isFieldStatic(Field aField) {
		int modifiers = aField.getModifiers();
		return Modifier.isStatic(modifiers);
	}
}
