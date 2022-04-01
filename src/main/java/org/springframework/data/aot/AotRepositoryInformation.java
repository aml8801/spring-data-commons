/*
 * Copyright 2022. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.aot;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformationSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2022/03
 */
public class AotRepositoryInformation extends RepositoryInformationSupport implements RepositoryInformation {

	private static final String REPOSITORY_BASE_CLASS_PROPERTY = "repositoryBaseClass";
	private static final String FRAGMENTS_PROPERTY = "repositoryFragments";
	private static final String CUSTOM_IMPLEMENTATION_PROPERTY = "customImplementation";

	private Collection<RepositoryFragment<?>> fragments;

	AotRepositoryInformation(RepositoryMetadata repositoryMetadata, Class<?> repositoryBaseClass,
			Collection<RepositoryFragment<?>> fragments) {

		super(repositoryMetadata, repositoryBaseClass);
		this.fragments = fragments;
	}

	static AotRepositoryInformation from(AotBeanContext beanContext) {

		DefaultRepositoryMetadata metadata = new DefaultRepositoryMetadata(
				readRepositoryInterfaceFromBeanDefinition(beanContext));
		Class<?> repositoryBaseClass = readRepositoryBaseClassFromBeanDefinition(beanContext);
		List<RepositoryFragment<?>> repositoryFragments = readFragmentsFromBeanDefinition(beanContext);

		return new AotRepositoryInformation(metadata, repositoryBaseClass, repositoryFragments);
	}

	@Nullable
	private static Class<?> readRepositoryBaseClassFromBeanDefinition(AotBeanContext beanContext) {

		if (!beanContext.containsProperty(REPOSITORY_BASE_CLASS_PROPERTY)) {
			return null;
		}

		PropertyValue repositoryBaseClass = beanContext.getPropertyValue(REPOSITORY_BASE_CLASS_PROPERTY);

		if(repositoryBaseClass.getValue() instanceof Class<?> type) {
			return type;
		}

		if(repositoryBaseClass.getValue() instanceof String typeName) {

			return beanContext //
					.resolveType(typeName) //
					.orElseThrow(() -> new InvalidPropertyException(RepositoryFactoryBeanSupport.class,
							REPOSITORY_BASE_CLASS_PROPERTY, "Unable to load custom repository base class!"));
		}

		throw new BeanDefinitionValidationException("must hjave repo base class0");
	}

	private static Class<?> readRepositoryInterfaceFromBeanDefinition(AotBeanContext beanContext) {

		if (beanContext.getBeanDefinition().getConstructorArgumentValues().getArgumentCount() != 1) {
			throw new BeanDefinitionValidationException(
					"No repository interface defined on for " + beanContext.getBeanDefinition());
		}

		Object repositoryInterface = beanContext.getConstructorArgument(0);
		if (repositoryInterface instanceof Class) {
			return (Class<?>) repositoryInterface;
		}

		return beanContext //
				.resolveType(repositoryInterface.toString()) //
				.orElseThrow(() -> new CannotLoadBeanClassException(null, beanContext.getBeanName(),
						repositoryInterface.toString(), new ClassNotFoundException(repositoryInterface.toString())));

	}

	private static List<RepositoryFragment<?>> readFragmentsFromBeanDefinition(AotBeanContext beanContext) {
		if (!beanContext.getBeanDefinition().getPropertyValues().contains(FRAGMENTS_PROPERTY)) {
			return Collections.emptyList();
		}
		List<RepositoryFragment<?>> detectedFragments = new ArrayList<>();
		PropertyValue repositoryFragments = beanContext.getBeanDefinition().getPropertyValues()
				.getPropertyValue(FRAGMENTS_PROPERTY);
		Object fragments = repositoryFragments.getValue();
		if (fragments instanceof RootBeanDefinition fragmentsBeanDefinition) {

			ValueHolder argumentValue = fragmentsBeanDefinition.getConstructorArgumentValues().getArgumentValue(0,
					List.class);
			List<String> fragmentBeanNames = (List<String>) argumentValue.getValue();
			for (String beanName : fragmentBeanNames) {

				RootBeanDefinition bd = beanContext.getRootBeanDefinition(beanName);
				ValueHolder fragmentInterface = bd.getConstructorArgumentValues().getArgumentValue(0, String.class);
				try {
					detectedFragments.add(
							RepositoryFragment.structural(beanContext.resolveRequiredType(fragmentInterface.getValue().toString())));
				} catch (ClassNotFoundException ex) {
					throw new CannotLoadBeanClassException(null, beanName, fragmentInterface.getValue().toString(), ex);
				}
				if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(1)) { // fragment implementation
					ValueHolder fragmentImplementation = bd.getConstructorArgumentValues().getArgumentValue(1,
							BeanReference.class);
					if (fragmentImplementation.getValue() instanceof BeanReference beanReference) {
						detectedFragments.add(RepositoryFragment.implemented(beanContext.resolveType(beanReference)));
					}
				}
			}
		}
		return detectedFragments;
	}

	@Override
	public boolean isCustomMethod(Method method) {

		// TODO:
		return false;
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		// TODO
		return false;
	}

	@Override
	public Method getTargetClassMethod(Method method) {

		// TODO
		return method;
	}

	/**
	 * @return
	 * @since 3.0
	 */
	@Nullable
	public Set<RepositoryFragment<?>> getFragments() {
		return new LinkedHashSet<>(fragments);
	}

}
