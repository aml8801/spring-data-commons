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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.util.ClassTypeInformation.from;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link ParameterizedTypeInformation}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jürgen Diez
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParameterizedTypeInformationUnitTests {

	@Mock ParameterizedType one;

	@BeforeEach
	void setUp() {
		when(one.getActualTypeArguments()).thenReturn(new Type[0]);
	}

	@Test
	void considersTypeInformationsWithDifferingParentsNotEqual() {

		var stringParent = new TypeDiscoverer<String>(String.class, emptyMap());
		var objectParent = new TypeDiscoverer<Object>(Object.class, emptyMap());

		var first = new ParameterizedTypeInformation<Object>(one, stringParent);
		var second = new ParameterizedTypeInformation<Object>(one, objectParent);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void considersTypeInformationsWithSameParentsNotEqual() {

		var stringParent = new TypeDiscoverer<String>(String.class, emptyMap());

		var first = new ParameterizedTypeInformation<Object>(one, stringParent);
		var second = new ParameterizedTypeInformation<Object>(one, stringParent);

		assertThat(first.equals(second)).isTrue();
	}

	@Test // DATACMNS-88
	void resolvesMapTypesCorrectly() {

		TypeInformation<Foo> type = ClassTypeInformation.from(Foo.class);
		var propertyType = type.getProperty("param");
		var value = propertyType.getProperty("value");

		assertThat(propertyType.getComponentType().getType()).isEqualTo(Locale.class);
		assertThat(value.getType()).isEqualTo(String.class);
		assertThat(propertyType.getMapValueType().getType()).isEqualTo(String.class);

		propertyType = type.getProperty("param2");
		value = propertyType.getProperty("value");

		assertThat(propertyType.getComponentType().getType()).isEqualTo(String.class);
		assertThat(value.getType()).isEqualTo(String.class);
		assertThat(propertyType.getMapValueType().getType()).isEqualTo(Locale.class);
	}

	@Test // #2517
	void resolvesVavrMapTypesCorrectly() {

		var type = ClassTypeInformation.from(VavrFoo.class);
		var propertyType = type.getProperty("param");

		assertThat(propertyType.getComponentType().getType()).isEqualTo(Locale.class);
		assertThat(propertyType.getMapValueType().getType()).isEqualTo(String.class);

		propertyType = type.getProperty("param2");

		assertThat(propertyType.getComponentType().getType()).isEqualTo(String.class);
		assertThat(propertyType.getMapValueType().getType()).isEqualTo(Locale.class);
	}

	@Test // DATACMNS-446
	void createsToStringRepresentation() {

		assertThat(from(Foo.class).getProperty("param").toString())
				.isEqualTo("org.springframework.data.util.ParameterizedTypeInformationUnitTests$Localized<java.lang.String>");
	}

	@Test // DATACMNS-485
	void hashCodeShouldBeConsistentWithEqualsForResolvedTypes() {

		var first = from(First.class).getProperty("property");
		var second = from(Second.class).getProperty("property");

		assertThat(first).isEqualTo(second);

		assertThat(first).satisfies(
				left -> assertThat(second).satisfies(right -> assertThat(left.hashCode()).isEqualTo(right.hashCode())));
	}

	@Test // DATACMNS-485
	void getActualTypeShouldNotUnwrapParameterizedTypes() {

		var type = from(First.class).getProperty("property");

		assertThat(type.getActualType()).isEqualTo(type);
	}

	@Test // DATACMNS-697
	void usesLocalGenericInformationOfFields() {

		TypeInformation<NormalizedProfile> information = ClassTypeInformation.from(NormalizedProfile.class);

		assertThat(information.getProperty("education2.data").getComponentType().getProperty("value"))//
				.satisfies(it -> assertThat(it.getType()).isEqualTo(Education.class));
	}

	@Test // DATACMNS-899
	void returnsEmptyOptionalMapValueTypeForNonMapProperties() {

		var typeInformation = ClassTypeInformation.from(Bar.class).getProperty("param");
		assertThat(typeInformation).isInstanceOf(ParameterizedTypeInformation.class);
		assertThat(typeInformation.getMapValueType()).isNull();
	}

	@Test // DATACMNS-1135
	void prefersLocalGenericsDeclarationOverParentBound() {

		var candidate = ClassTypeInformation.from(Candidate.class);

		var componentType = candidate.getRequiredProperty("experiences.values").getRequiredComponentType();
		componentType = componentType.getRequiredProperty("responsibilities.values").getRequiredComponentType();

		assertThat(componentType.getType()).isEqualTo(Responsibility.class);
	}

	@Test // DATACMNS-1196
	void detectsNestedGenerics() {

		var myList = ClassTypeInformation.from(EnumGeneric.class).getRequiredProperty("inner.myList");

		assertThat(myList.getRequiredComponentType().getType()).isEqualTo(MyEnum.class);
	}

	@SuppressWarnings("serial")
	class Localized<S> extends HashMap<Locale, S> {
		S value;
	}

	@SuppressWarnings("serial")
	class Localized2<S> extends HashMap<S, Locale> {
		S value;
	}

	class Foo {
		Localized<String> param;
		Localized2<String> param2;
	}

	class VavrFoo {
		io.vavr.collection.HashMap<Locale, String> param;
		io.vavr.collection.HashMap<String, Locale> param2;
	}

	class Bar {
		List<String> param;
	}

	class Parameterized<T> {
		T property;
	}

	class First {
		Parameterized<String> property;
	}

	class Second {
		Parameterized<String> property;
	}

	// see DATACMNS-697

	class NormalizedProfile {

		ListField<Education> education2;
	}

	class ListField<L> {
		List<Value<L>> data;
	}

	class Value<T> {
		T value;
	}

	private class Education {}

	// DATACMNS-1135

	abstract class CandidateInfo {}

	private class Responsibility extends CandidateInfo {}

	class Experience extends CandidateInfo {
		CandidateInfoContainer<Responsibility> responsibilities;
	}

	class CandidateInfoContainer<E extends CandidateInfo> {
		List<E> values = new ArrayList<>();
	}

	class Candidate {
		CandidateInfoContainer<Experience> experiences;
	}

	// FOO

	static abstract class Generic<T> {

		Inner<T> inner;

		static class Inner<T> {
			List<T> myList;
		}
	}

	private static class EnumGeneric extends Generic<MyEnum> {}

	public enum MyEnum {
		E1, E2
	}
}
