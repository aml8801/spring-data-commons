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
package org.springframework.data.aot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.repository.config.RepositoryFragmentConfiguration;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class RepositoryBeanDefinitionReader {

	static RepositoryInformation readRepositoryInformation(RepositoryMetadata metadata,
			ConfigurableListableBeanFactory beanFactory) {

		// TODO: how to use user defined factory beans?

		DefaultRepositoryMetadata metadata1 = new DefaultRepositoryMetadata(
				metadata.getRepositoryInterfaceType(beanFactory.getBeanClassLoader()));
		Class<?> repositoryBaseClass = (Class<?>) metadata.getRepositoryBaseClassName()
				.map(it -> forName(it.toString(), beanFactory)).orElseGet(() -> {

					// TODO: retrieve the default without loading the actual RepositoryBeanFactory
					return Object.class;
				});

		Object theFragments = metadata.getFragmentConfiguration().stream().flatMap(it -> {
			RepositoryFragmentConfiguration fragmentConfiguration = (RepositoryFragmentConfiguration) it;

			List<RepositoryFragment> fragments = new ArrayList<>(2);
			if (fragmentConfiguration.getClassName() != null) {
				fragments.add(RepositoryFragment.implemented(forName(fragmentConfiguration.getClassName(), beanFactory)));
			}
			if (fragmentConfiguration.getInterfaceName() != null) {
				fragments.add(RepositoryFragment.structural(forName(fragmentConfiguration.getInterfaceName(), beanFactory)));
			}

			return fragments.stream();
		}).collect(Collectors.toList());

		return new AotRepositoryInformation(metadata1, repositoryBaseClass,
				(Collection<RepositoryFragment<?>>) theFragments);
	}

	static Class<?> forName(String name, ConfigurableListableBeanFactory beanFactory) {
		try {
			return ClassUtils.forName(name, beanFactory.getBeanClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	// private static final String REPOSITORY_BASE_CLASS_PROPERTY = "repositoryBaseClass";
	// private static final String FRAGMENTS_PROPERTY = "repositoryFragments";
	// private static final String CUSTOM_IMPLEMENTATION_PROPERTY = "customImplementation";
	//
	// @Nullable
	// private static Class<?> readRepositoryBaseClassFromBeanDefinition(AotBeanContext beanContext) {
	//
	// if (!beanContext.containsProperty(REPOSITORY_BASE_CLASS_PROPERTY)) {
	// return null;
	// }
	//
	// PropertyValue repositoryBaseClass = beanContext.getPropertyValue(REPOSITORY_BASE_CLASS_PROPERTY);
	//
	// if (repositoryBaseClass.getValue() instanceof Class<?> type) {
	// return type;
	// }
	//
	// if (repositoryBaseClass.getValue() instanceof String typeName) {
	//
	// return beanContext //
	// .resolveType(typeName) //
	// .orElseThrow(() -> new InvalidPropertyException(RepositoryFactoryBeanSupport.class,
	// REPOSITORY_BASE_CLASS_PROPERTY, "Unable to load custom repository base class!"));
	// }
	//
	// throw new BeanDefinitionValidationException("must hjave repo base class0");
	// }
	//
	// private static Class<?> readRepositoryInterfaceFromBeanDefinition(AotBeanContext beanContext) {
	//
	// if (beanContext.getBeanDefinition().getConstructorArgumentValues().getArgumentCount() != 1) {
	// throw new BeanDefinitionValidationException(
	// "No repository interface defined on for " + beanContext.getBeanDefinition());
	// }
	//
	// Object repositoryInterface = beanContext.getConstructorArgument(0);
	// if (repositoryInterface instanceof Class) {
	// return (Class<?>) repositoryInterface;
	// }
	//
	// return beanContext //
	// .resolveType(repositoryInterface.toString()) //
	// .orElseThrow(() -> new CannotLoadBeanClassException(null, beanContext.getBeanName(),
	// repositoryInterface.toString(), new ClassNotFoundException(repositoryInterface.toString())));
	// }
	//
	// private static List<RepositoryFragment<?>> readCustomImplementationFromBeanDefinition(AotBeanContext beanContext) {
	//
	// if (!beanContext.containsProperty(CUSTOM_IMPLEMENTATION_PROPERTY)) {
	// return Collections.emptyList();
	// }
	//
	// PropertyValue propertyValue = beanContext.getPropertyValue(CUSTOM_IMPLEMENTATION_PROPERTY);
	// if (propertyValue.getValue() instanceof BeanReference beanReference) {
	// Class<?> customImplementationType = beanContext.resolveType(beanReference);
	// if (!ObjectUtils.isEmpty(customImplementationType.getInterfaces())) {
	// ArrayList<RepositoryFragment<?>> repositoryFragments = new ArrayList<>(
	// Arrays.stream(customImplementationType.getInterfaces()).map(RepositoryFragment::structural)
	// .collect(Collectors.toList()));
	// repositoryFragments.add(RepositoryFragment.implemented(customImplementationType));
	// return repositoryFragments;
	// }
	// return Collections.singletonList(RepositoryFragment.implemented(beanContext.resolveType(beanReference)));
	// }
	//
	// throw new InvalidPropertyException(RepositoryFactoryBeanSupport.class, CUSTOM_IMPLEMENTATION_PROPERTY,
	// "Not a BeanReference to custom repository implementation!");
	// }
	//
	// private static List<RepositoryFragment<?>> readFragmentsFromBeanDefinition(AotBeanContext beanContext) {
	// if (!beanContext.getBeanDefinition().getPropertyValues().contains(FRAGMENTS_PROPERTY)) {
	// return Collections.emptyList();
	// }
	// List<RepositoryFragment<?>> detectedFragments = new ArrayList<>();
	// PropertyValue repositoryFragments = beanContext.getBeanDefinition().getPropertyValues()
	// .getPropertyValue(FRAGMENTS_PROPERTY);
	// Object fragments = repositoryFragments.getValue();
	// if (fragments instanceof RootBeanDefinition fragmentsBeanDefinition) {
	//
	// ValueHolder argumentValue = fragmentsBeanDefinition.getConstructorArgumentValues().getArgumentValue(0,
	// List.class);
	// List<String> fragmentBeanNames = (List<String>) argumentValue.getValue();
	// for (String beanName : fragmentBeanNames) {
	//
	// RootBeanDefinition bd = beanContext.getRootBeanDefinition(beanName);
	// ValueHolder fragmentInterface = bd.getConstructorArgumentValues().getArgumentValue(0, String.class);
	// try {
	// detectedFragments.add(
	// RepositoryFragment.structural(beanContext.resolveRequiredType(fragmentInterface.getValue().toString())));
	// } catch (ClassNotFoundException ex) {
	// throw new CannotLoadBeanClassException(null, beanName, fragmentInterface.getValue().toString(), ex);
	// }
	// if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(1)) { // fragment implementation
	// ValueHolder fragmentImplementation = bd.getConstructorArgumentValues().getArgumentValue(1,
	// BeanReference.class);
	// if (fragmentImplementation.getValue() instanceof BeanReference beanReference) {
	// detectedFragments.add(RepositoryFragment.implemented(beanContext.resolveType(beanReference)));
	// }
	// }
	// }
	// }
	// return detectedFragments;
	// }
}
