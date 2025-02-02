/*
 * Copyright 2016-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ClassGeneratingPropertyAccessorFactory}
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
public class ClassGeneratingPropertyAccessorFactoryDatatypeTests {

	private final ClassGeneratingPropertyAccessorFactory factory = new ClassGeneratingPropertyAccessorFactory();
	private final SampleMappingContext mappingContext = new SampleMappingContext();

	static List<Object[]> parameters() throws Exception {

		List<Object[]> parameters = new ArrayList<>();
		var types = Arrays.asList(FieldAccess.class, PropertyAccess.class, PrivateFinalFieldAccess.class,
				PrivateFinalPropertyAccess.class);

		parameters.addAll(parameters(types, "primitiveInteger", Integer.valueOf(1)));
		parameters.addAll(parameters(types, "primitiveIntegerArray", new int[] { 1, 2, 3 }));
		parameters.addAll(parameters(types, "boxedInteger", Integer.valueOf(1)));
		parameters.addAll(parameters(types, "boxedIntegerArray", new Integer[] { Integer.valueOf(1) }));
		parameters.addAll(parameters(types, "primitiveShort", Short.valueOf("1")));
		parameters.addAll(parameters(types, "primitiveShortArray", new short[] { 1, 2, 3 }));
		parameters.addAll(parameters(types, "boxedShort", Short.valueOf("1")));
		parameters.addAll(parameters(types, "boxedShortArray", new Short[] { Short.valueOf("1") }));
		parameters.addAll(parameters(types, "primitiveByte", Byte.valueOf("1")));
		parameters.addAll(parameters(types, "primitiveByteArray", new byte[] { 1, 2, 3 }));
		parameters.addAll(parameters(types, "boxedByte", Byte.valueOf("1")));
		parameters.addAll(parameters(types, "boxedByteArray", new Byte[] { Byte.valueOf("1") }));
		parameters.addAll(parameters(types, "primitiveChar", Character.valueOf('c')));
		parameters.addAll(parameters(types, "primitiveCharArray", new char[] { 'a', 'b', 'c' }));
		parameters.addAll(parameters(types, "boxedChar", Character.valueOf('c')));
		parameters.addAll(parameters(types, "boxedCharArray", new Character[] { Character.valueOf('c') }));
		parameters.addAll(parameters(types, "primitiveBoolean", Boolean.valueOf(true)));
		parameters.addAll(parameters(types, "primitiveBooleanArray", new boolean[] { true, false }));
		parameters.addAll(parameters(types, "boxedBoolean", Boolean.valueOf(true)));
		parameters.addAll(parameters(types, "boxedBooleanArray", new Boolean[] { Boolean.valueOf(true) }));
		parameters.addAll(parameters(types, "primitiveFloat", Float.valueOf(1f)));
		parameters.addAll(parameters(types, "primitiveFloatArray", new float[] { 1f, 2f }));
		parameters.addAll(parameters(types, "boxedFloat", Float.valueOf(1f)));
		parameters.addAll(parameters(types, "boxedFloatArray", new Float[] { Float.valueOf(1f) }));
		parameters.addAll(parameters(types, "primitiveDouble", Double.valueOf(1d)));
		parameters.addAll(parameters(types, "primitiveDoubleArray", new double[] { 1d, 2d }));
		parameters.addAll(parameters(types, "boxedDouble", Double.valueOf(1d)));
		parameters.addAll(parameters(types, "boxedDoubleArray", new Double[] { Double.valueOf(1d) }));
		parameters.addAll(parameters(types, "primitiveLong", Long.valueOf(1L)));
		parameters.addAll(parameters(types, "primitiveLongArray", new long[] { 1L, 2L }));
		parameters.addAll(parameters(types, "boxedLong", Long.valueOf(1L)));
		parameters.addAll(parameters(types, "boxedLongArray", new Long[] { Long.valueOf(1L) }));
		parameters.addAll(parameters(types, "string", "hello"));
		parameters.addAll(parameters(types, "stringArray", new String[] { "hello", "world" }));

		return parameters;
	}

	private static List<Object[]> parameters(List<Class<?>> types, String propertyName, Object value) throws Exception {

		List<Object[]> parameters = new ArrayList<>();

		for (var type : types) {

			var constructors = type.getDeclaredConstructors();
			constructors[0].setAccessible(true);
			parameters.add(new Object[] { constructors[0].newInstance(), propertyName, value,
					type.getSimpleName() + "/" + propertyName });
		}

		return parameters;
	}

	@ParameterizedTest(name = "{3}") // DATACMNS-809
	@MethodSource("parameters")
	void shouldSetAndGetProperty(Object bean, String propertyName, Object value, String displayName) {

		assertThat(getProperty(bean, propertyName)).satisfies(property -> {

			var persistentPropertyAccessor = getPersistentPropertyAccessor(bean);

			persistentPropertyAccessor.setProperty(property, value);
			assertThat(persistentPropertyAccessor.getProperty(property)).isEqualTo(value);
		});
	}

	@ParameterizedTest(name = "{3}") // DATACMNS-809
	@MethodSource("parameters")
	void shouldUseClassPropertyAccessorFactory(Object bean, String propertyName, Object value, String displayName)
			throws Exception {

		var persistentEntity = mappingContext
				.getRequiredPersistentEntity(bean.getClass());

		assertThat(ReflectionTestUtils.getField(persistentEntity, "propertyAccessorFactory"))
				.isInstanceOfSatisfying(InstantiationAwarePropertyAccessorFactory.class, it -> {
					assertThat(ReflectionTestUtils.getField(it, "delegate"))
							.isInstanceOf(ClassGeneratingPropertyAccessorFactory.class);
				});
	}

	private PersistentPropertyAccessor getPersistentPropertyAccessor(Object bean) {
		return factory.getPropertyAccessor(mappingContext.getRequiredPersistentEntity(bean.getClass()), bean);
	}

	private PersistentProperty<?> getProperty(Object bean, String name) {

		var persistentEntity = mappingContext
				.getRequiredPersistentEntity(bean.getClass());
		return persistentEntity.getPersistentProperty(name);
	}

	// DATACMNS-809
	@AccessType(Type.FIELD)
	public static class FieldAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;
	}

	// DATACMNS-809
	@AccessType(Type.PROPERTY)
	@Data
	public static class PropertyAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;
	}

	// DATACMNS-916
	@AccessType(Type.FIELD)
	private final static class PrivateFinalFieldAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;

	}

	// DATACMNS-916
	@AccessType(Type.PROPERTY)
	@Data
	private final static class PrivateFinalPropertyAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;

	}
}
