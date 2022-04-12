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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.SynthesizedAnnotation;
import org.springframework.data.annotation.SpringDataAnnotation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2022/03e
 */
public class AotContributingRepositoryBeanPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;
	// instance supplier
	// set the Map<String, RepositoryConfiguration<?>> configurationsByRepositoryName //

	public Map<String, RepositoryMetadata<?>> getConfigMap() {
		return configMap;
	}

	public void setConfigMap(Map<String, RepositoryMetadata<?>> configMap) {
		this.configMap = configMap;
	}

	Map<String, RepositoryMetadata<?>> configMap;
	Collection<Class<?>> managedTypes;

	AtomicBoolean x = new AtomicBoolean(false);

	@Nullable
	@Override
	public RepositoryBeanContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		// check if repo config is available or do the parsing

		if (!ObjectUtils.isEmpty(configMap)) {
			if (x.compareAndSet(false, true)) {
				AotBeanContext beanContext = new AotBeanContext(beanName, beanDefinition, beanFactory);
				for (RepositoryMetadata<?> metadata : configMap.values()) {
					RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader
							.readRepositoryInformation(metadata, beanFactory);
					return new RepositoryBeanContribution(beanContext, repositoryInformation,
							discoverTypes(repositoryInformation, typeFilter())).setModuleContribution(this::contribute);
				}
			}
			return null;
		}

		if (!ClassUtils.isAssignable(Repository.class, beanDefinition.getTargetType())) {
			return null;
		}

		AotBeanContext beanContext = new AotBeanContext(beanName, beanDefinition, beanFactory);
		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.readRepositoryInformation(beanContext);

		return new RepositoryBeanContribution(beanContext, repositoryInformation,
				discoverTypes(repositoryInformation, typeFilter())).setModuleContribution(this::contribute);
	}

	protected void contribute(AotRepositoryContext ctx, CodeContribution contribution) {

		ctx.getResolvedTypes().forEach(it -> contributeType(it, contribution));
		ctx.getResolvedAnnotations().stream().map(MergedAnnotation::getType).filter(it -> {

			if (it.getPackage().getName().startsWith("org.springframework.data")) {
				return true;
			}

			if (AnnotatedElementUtils.hasMetaAnnotationTypes(it, SpringDataAnnotation.class)) {
				return true;
			}

			return false;
		}).forEach(it -> contributeType(it, contribution));
	}

	protected Set<Class<?>> discoverTypes(RepositoryInformation repositoryInformation, Predicate<Class<?>> filter) {

		Set<Class<?>> types = new LinkedHashSet<>(TypeCollector.inspect(repositoryInformation.getDomainType()).list());

		repositoryInformation.getQueryMethods()
				.flatMap(it -> TypeUtils.resolveTypesInSignature(repositoryInformation.getRepositoryInterface(), it).stream())
				.flatMap(it -> TypeCollector.inspect(it).list().stream()).forEach(types::add);

		return types;
	}

	protected void contributeType(Class<?> type, CodeContribution contribution) {

		if (type.isAnnotation()) {
			contribution.runtimeHints().reflection().registerType(type, hint -> {
				hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
			});
			if (type.getPackage().getName().startsWith("org.springframework.data")) {
				contribution.runtimeHints().proxies().registerJdkProxy(type, SynthesizedAnnotation.class);
			}
			return;
		}
		if (type.isInterface()) {
			contribution.runtimeHints().reflection().registerType(type, hint -> {
				hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
			});
			return;
		}
		if (type.isPrimitive()) {
			return;
		}
		contribution.runtimeHints().reflection().registerType(type, hint -> {
			hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
		});
	}

	public Predicate<Class<?>> typeFilter() { // like only document ones.
		return it -> true;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}
}
