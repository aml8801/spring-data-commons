/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.util.ClassTypeInformation.from;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TypeDiscoverer}.
 *
 * @author Oliver Gierke
 * @author Jürgen Diez
 */
@ExtendWith(MockitoExtension.class)
public class TypeDiscovererUnitTests {

	private static final Map<TypeVariable<?>, Type> EMPTY_MAP = Collections.emptyMap();

	@Mock Map<TypeVariable<?>, Type> firstMap;
	@Mock Map<TypeVariable<?>, Type> secondMap;

	@Test
	void rejectsNullType() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TypeDiscoverer<>(null, null));
	}

	@Test
	void isNotEqualIfTypesDiffer() {

		var objectTypeInfo = new TypeDiscoverer<Object>(Object.class, EMPTY_MAP);
		var stringTypeInfo = new TypeDiscoverer<String>(String.class, EMPTY_MAP);

		assertThat(objectTypeInfo.equals(stringTypeInfo)).isFalse();
	}

	@Test
	void isNotEqualIfTypeVariableMapsDiffer() {

		assertThat(firstMap.equals(secondMap)).isFalse();

		var first = new TypeDiscoverer<Object>(Object.class, firstMap);
		var second = new TypeDiscoverer<Object>(Object.class, secondMap);

		assertThat(first.equals(second)).isFalse();
	}

	@Test
	void dealsWithTypesReferencingThemselves() {

		TypeInformation<SelfReferencing> information = from(SelfReferencing.class);
		var first = information.getProperty("parent").getMapValueType();
		var second = first.getProperty("map").getMapValueType();

		assertThat(second).isEqualTo(first);
	}

	@Test
	void dealsWithTypesReferencingThemselvesInAMap() {

		TypeInformation<SelfReferencingMap> information = from(SelfReferencingMap.class);
		var property = information.getProperty("map");

		assertThat(property.getMapValueType()).isEqualTo(information);
	}

	@Test
	void returnsComponentAndValueTypesForMapExtensions() {

		TypeInformation<?> discoverer = new TypeDiscoverer<>(CustomMap.class, EMPTY_MAP);

		assertThat(discoverer.getMapValueType().getType()).isEqualTo(Locale.class);
		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	void returnsComponentTypeForCollectionExtension() {

		var discoverer = new TypeDiscoverer<CustomCollection>(CustomCollection.class, firstMap);

		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	void returnsComponentTypeForArrays() {

		var discoverer = new TypeDiscoverer<String[]>(String[].class, EMPTY_MAP);

		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-57
	void discoveresConstructorParameterTypesCorrectly() throws NoSuchMethodException, SecurityException {

		var discoverer = new TypeDiscoverer<GenericConstructors>(GenericConstructors.class, firstMap);
		var constructor = GenericConstructors.class.getConstructor(List.class, Locale.class);
		var types = discoverer.getParameterTypes(constructor);

		assertThat(types).hasSize(2);
		assertThat(types.get(0).getType()).isEqualTo(List.class);
		assertThat(types.get(0).getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void returnsNullForComponentAndValueTypesForRawMaps() {

		var discoverer = new TypeDiscoverer<Map>(Map.class, EMPTY_MAP);

		assertThat(discoverer.getComponentType()).isNull();
		assertThat(discoverer.getMapValueType()).isNull();
	}

	@Test // DATACMNS-167
	@SuppressWarnings("rawtypes")
	void doesNotConsiderTypeImplementingIterableACollection() {

		var discoverer = new TypeDiscoverer<Person>(Person.class, EMPTY_MAP);
		TypeInformation reference = from(Address.class);

		var addresses = discoverer.getProperty("addresses");

		assertThat(addresses).satisfies(it -> {
			assertThat(it.isCollectionLike()).isFalse();
			assertThat(it.getComponentType()).isEqualTo(reference);
		});

		var adressIterable = discoverer.getProperty("addressIterable");

		assertThat(adressIterable).satisfies(it -> {
			assertThat(it.isCollectionLike()).isTrue();
			assertThat(it.getComponentType()).isEqualTo(reference);
		});
	}

	@Test // DATACMNS-1342, DATACMNS-1430
	void considersStreamableToBeCollectionLike() {

		TypeInformation<SomeStreamable> type = from(SomeStreamable.class);

		assertThat(type.isCollectionLike()).isTrue();
		assertThat(type.getRequiredProperty("streamable").isCollectionLike()).isTrue();
	}

	@Test // DATACMNS-1419
	void detectsSubTypes() {

		var type = from(Set.class);

		assertThat(type.isSubTypeOf(Collection.class)).isTrue();
		assertThat(type.isSubTypeOf(Set.class)).isFalse();
		assertThat(type.isSubTypeOf(String.class)).isFalse();
	}

	@Test // #2511
	void considerVavrMapToBeAMap() {

		var type = from(io.vavr.collection.Map.class);

		assertThat(type.isMap()).isTrue();
	}

	@Test // #2517
	void returnsComponentAndValueTypesForVavrMapExtensions() {

		var discoverer = new TypeDiscoverer<>(CustomVavrMap.class, EMPTY_MAP);

		assertThat(discoverer.getMapValueType().getType()).isEqualTo(Locale.class);
		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test // #2511
	void considerVavrSetToBeCollectionLike() {

		var type = from(io.vavr.collection.Set.class);

		assertThat(type.isCollectionLike()).isTrue();
	}

	@Test // #2511
	void considerVavrSeqToBeCollectionLike() {

		var type = from(io.vavr.collection.Seq.class);

		assertThat(type.isCollectionLike()).isTrue();
	}

	@Test // #2511
	void considerVavrListToBeCollectionLike() {

		var type = from(io.vavr.collection.List.class);

		assertThat(type.isCollectionLike()).isTrue();
	}

	class Person {

		Addresses addresses;
		Iterable<Address> addressIterable;
	}

	abstract class Addresses implements Iterable<Address> {

	}

	class Address {

	}

	class SelfReferencing {

		Map<String, SelfReferencingMap> parent;
	}

	class SelfReferencingMap {
		Map<String, SelfReferencingMap> map;
	}

	interface CustomMap extends Map<String, Locale> {

	}

	interface CustomCollection extends Collection<String> {

	}

	public static class GenericConstructors {

		public GenericConstructors(List<String> first, Locale second) {

		}
	}

	// DATACMNS-1342

	static class SomeStreamable implements Streamable<String> {

		Streamable<String> streamable;

		@Override
		public Iterator<String> iterator() {
			return Collections.emptyIterator();
		}
	}

	interface CustomVavrMap extends io.vavr.collection.Map<String, Locale> {}
}
