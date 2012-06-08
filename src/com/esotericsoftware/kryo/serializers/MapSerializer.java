
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects that implement the {@link Map} interface.
 * <p>
 * With the default constructor, a map requires a 1-3 byte header and an extra 4 bytes is written for each key/value pair.
 * @author Nathan Sweet <misc@n4te.com> */
public class MapSerializer extends Serializer<Map> {
	private Class keyClass, valueClass;
	private Serializer keySerializer, valueSerializer;
	private boolean keysCanBeNull = true, valuesCanBeNull = true;
	private Class keyGenericType, valueGenericType;

	/** @param keysCanBeNull False if all keys are not null. This saves 1 byte per key if keyClass is set. True if it is not known
	 *           (default). */
	public void setKeysCanBeNull (boolean keysCanBeNull) {
		this.keysCanBeNull = keysCanBeNull;
	}

	/** @param keyClass The concrete class of each key. This saves 1 byte per key. Set to null if the class is not known or varies
	 *           per key (default).
	 * @param keySerializer The serializer to use for each key. */
	public void setKeyClass (Class keyClass, Serializer keySerializer) {
		this.keyClass = keyClass;
		this.keySerializer = keySerializer;
	}

	/** @param valueClass The concrete class of each value. This saves 1 byte per value. Set to null if the class is not known or
	 *           varies per value (default).
	 * @param valueSerializer The serializer to use for each value. */
	public void setValueClass (Class valueClass, Serializer valueSerializer) {
		this.valueClass = valueClass;
		this.valueSerializer = valueSerializer;
	}

	/** @param valuesCanBeNull True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not known
	 *           (default). */
	public void setValuesCanBeNull (boolean valuesCanBeNull) {
		this.valuesCanBeNull = valuesCanBeNull;
	}

	public void setGenerics (Kryo kryo, Type[] generics) {
		if (generics == null) {
			keyGenericType = null;
			valueGenericType = null;
		} else {
			Class type = (Class)generics[0];
			if (kryo.isFinal(type)) keyGenericType = type;
			type = (Class)generics[1];
			if (kryo.isFinal(type)) valueGenericType = type;
		}
	}

	public void write (Kryo kryo, Output output, Map map) {
		int length = map.size();
		output.writeInt(length, true);
		if (length == 0) return;

		Serializer keySerializer = this.keySerializer;
		if (keyGenericType != null) {
			if (keySerializer == null) keySerializer = kryo.getSerializer(keyGenericType);
			keyGenericType = null;
		}
		Serializer valueSerializer = this.valueSerializer;
		if (valueGenericType != null) {
			if (valueSerializer == null) valueSerializer = kryo.getSerializer(valueGenericType);
			valueGenericType = null;
		}

		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (keySerializer != null) {
				if (keysCanBeNull)
					kryo.writeObjectOrNull(output, entry.getKey(), keySerializer);
				else
					kryo.writeObject(output, entry.getKey(), keySerializer);
			} else
				kryo.writeClassAndObject(output, entry.getKey());
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					kryo.writeObjectOrNull(output, entry.getValue(), valueSerializer);
				else
					kryo.writeObject(output, entry.getValue(), valueSerializer);
			} else
				kryo.writeClassAndObject(output, entry.getValue());
		}
	}

	public Map read (Kryo kryo, Input input, Class<Map> type) {
		Map map = kryo.newInstance(type);
		int length = input.readInt(true);
		if (length == 0) return map;

		Serializer keySerializer = this.keySerializer;
		if (keyGenericType != null) {
			if (keySerializer == null) keySerializer = kryo.getSerializer(keyGenericType);
			keyGenericType = null;
		}
		Serializer valueSerializer = this.valueSerializer;
		if (valueGenericType != null) {
			if (valueSerializer == null) valueSerializer = kryo.getSerializer(valueGenericType);
			valueGenericType = null;
		}
		kryo.reference(map);

		for (int i = 0; i < length; i++) {
			Object key;
			if (keySerializer != null) {
				if (keysCanBeNull)
					key = kryo.readObjectOrNull(input, keyClass, keySerializer);
				else
					key = kryo.readObject(input, keyClass, keySerializer);
			} else
				key = kryo.readClassAndObject(input);
			Object value;
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					value = kryo.readObjectOrNull(input, valueClass, valueSerializer);
				else
					value = kryo.readObject(input, valueClass, valueSerializer);
			} else
				value = kryo.readClassAndObject(input);
			map.put(key, value);
		}
		return map;
	}

	public Map copy (Kryo kryo, Map original) {
		Map copy = kryo.newInstance(original.getClass());
		for (Iterator iter = original.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			copy.put(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
		}
		return copy;
	}
}
