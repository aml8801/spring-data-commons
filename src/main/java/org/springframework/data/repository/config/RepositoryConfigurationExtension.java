/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Collection;

import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.aot.RepositoryRegistrationAotProcessor;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * SPI to implement store specific extension to the repository bean definition registration process.
 *
 * @see RepositoryConfigurationExtensionSupport
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author John Blum
 */
public interface RepositoryConfigurationExtension {

	/**
	 * Returns a descriptive {@link String name} for the module.
	 *
	 * @return a descriptive {@link String name} for the module.
	 * @see #getModulePrefix()
	 */
	default String getModuleName() {
		return StringUtils.capitalize(getModulePrefix());
	}

	/**
	 * Returns a {@link String prefix} identifying the module.
	 *
	 * @return a {@link String prefix} identifying the module.
	 * @see #getModuleName()
	 */
	String getModulePrefix();

	/**
	 * Returns the {@link BeanRegistrationAotProcessor} type responsible for contributing AOT/native configuration
	 * required by the Spring Data Repository infrastructure components at native runtime.
	 *
	 * @return the {@link BeanRegistrationAotProcessor} type responsible for contributing AOT/native configuration.
	 * Defaults to {@link RepositoryRegistrationAotProcessor}. Must not be {@literal null}.
	 * @see org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
	 * @since 3.0
	 */
	@NonNull
	default Class<? extends BeanRegistrationAotProcessor> getRepositoryAotProcessor() {
		return RepositoryRegistrationAotProcessor.class;
	}

	/**
	 * Returns all {@link RepositoryConfiguration}s obtained through the given {@link RepositoryConfigurationSource}.
	 *
	 * @param configSource {@link RepositoryConfigurationSource} encapsulating the source (XML, Annotation) of
	 * the repository configuration.
	 * @param loader {@link ResourceLoader} used to load resources.
	 * @param strictMatchesOnly whether to return strict repository matches only. Handing in {@literal true} will cause
	 *          the repository interfaces and domain types handled to be checked whether they are managed by the current
	 *          store.
	 * @return a {@link Collection} of {@link RepositoryConfiguration RepositoryConfigurations}.
	 * @see org.springframework.data.repository.config.RepositoryConfiguration
	 * @since 1.9
	 */
	<T extends RepositoryConfigurationSource> Collection<RepositoryConfiguration<T>> getRepositoryConfigurations(
			T configSource, ResourceLoader loader, boolean strictMatchesOnly);

	/**
	 * Returns the {@link String name} of the repository factory class to be used.
	 *
	 * @return the {@link String name} of the repository factory class to be used.
	 */
	String getRepositoryFactoryBeanClassName();

	/**
	 * Returns the default location of the Spring Data named queries.
	 *
	 * @return must not be {@literal null} or empty.
	 */
	String getDefaultNamedQueryLocation();

	/**
	 * Callback to register additional bean definitions for a {@literal repositories} root node. This usually includes
	 * beans you have to set up once independently of the number of repositories to be created. Will be called before any
	 * repositories bean definitions have been registered.
	 *
	 * @param registry {@link BeanDefinitionRegistry} containing bean definitions.
	 * @param configurationSource {@link RepositoryConfigurationSource} encapsulating the source (e.g. XML, Annotation)
	 * of the repository configuration.
	 */
	void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource);

	/**
	 * Callback to post process the {@link BeanDefinition} and tweak the configuration if necessary.
	 *
	 * @param builder will never be {@literal null}.
	 * @param config will never be {@literal null}.
	 */
	void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource config);

	/**
	 * Callback to post process the {@link BeanDefinition} built from annotations and tweak the configuration if
	 * necessary.
	 *
	 * @param builder will never be {@literal null}.
	 * @param config will never be {@literal null}.
	 */
	void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config);

	/**
	 * Callback to post process the {@link BeanDefinition} built from XML and tweak the configuration if necessary.
	 *
	 * @param builder will never be {@literal null}.
	 * @param config will never be {@literal null}.
	 */
	void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config);

}
