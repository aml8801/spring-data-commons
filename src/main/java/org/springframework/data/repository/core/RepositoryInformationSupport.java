/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.repository.core;

import static org.springframework.data.repository.util.ClassUtils.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public abstract class RepositoryInformationSupport implements RepositoryInformation {

	private final RepositoryMetadata metadata;
	private final Class<?> repositoryBaseClass;

	public RepositoryInformationSupport(RepositoryMetadata metadata, Class<?> repositoryBaseClass) {

		Assert.notNull(metadata, "Repository metadata must not be null!");
		Assert.notNull(repositoryBaseClass, "Repository base class must not be null!");

		this.metadata = metadata;
		this.repositoryBaseClass = repositoryBaseClass;
	}

	@Override
	public Streamable<Method> getQueryMethods() {

		Set<Method> result = new HashSet<>();

		for (Method method : getRepositoryInterface().getMethods()) {
			method = ClassUtils.getMostSpecificMethod(method, getRepositoryInterface());
			if (isQueryMethodCandidate(method)) {
				result.add(method);
			}
		}

		return Streamable.of(Collections.unmodifiableSet(result));
	}

	@Override
	public Class<?> getIdType() {
		return metadata.getIdType();
	}

	@Override
	public Class<?> getDomainType() {
		return metadata.getDomainType();
	}

	@Override
	public Class<?> getRepositoryInterface() {
		return metadata.getRepositoryInterface();
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return metadata.getReturnType(method);
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		return metadata.getReturnedDomainClass(method);
	}

	@Override
	public CrudMethods getCrudMethods() {
		return metadata.getCrudMethods();
	}

	@Override
	public boolean isPagingRepository() {
		return metadata.isPagingRepository();
	}

	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return metadata.getAlternativeDomainTypes();
	}

	@Override
	public boolean isReactiveRepository() {
		return metadata.isReactiveRepository();
	}

	@Override
	public Class<?> getRepositoryBaseClass() {
		return repositoryBaseClass;
	}

	@Override
	public boolean isQueryMethod(Method method) {
		return getQueryMethods().stream().anyMatch(it -> it.equals(method));
	}

	@Override
	public TypeInformation<?> getDomainTypeInformation() {
		return metadata.getDomainTypeInformation();
	}

	@Override
	public TypeInformation<?> getIdTypeInformation() {
		return metadata.getIdTypeInformation();
	}

	@Override
	public boolean hasCustomMethod() {

		Class<?> repositoryInterface = getRepositoryInterface();

		// No detection required if no typing interface was configured
		if (isGenericRepositoryInterface(repositoryInterface)) {
			return false;
		}

		for (Method method : repositoryInterface.getMethods()) {
			if (isCustomMethod(method) && !isBaseClassMethod(method)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the given method contains a custom store specific query annotation annotated with
	 * {@link QueryAnnotation}. The method-hierarchy is also considered in the search for the annotation.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryAnnotationPresentOn(Method method) {

		return AnnotationUtils.findAnnotation(method, QueryAnnotation.class) != null;
	}

	/**
	 * Checks whether the given method is a query method candidate.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryMethodCandidate(Method method) {
		return !method.isBridge() && !method.isDefault() //
				&& !Modifier.isStatic(method.getModifiers()) //
				&& (isQueryAnnotationPresentOn(method) || !isCustomMethod(method) && !isBaseClassMethod(method));
	}
}
