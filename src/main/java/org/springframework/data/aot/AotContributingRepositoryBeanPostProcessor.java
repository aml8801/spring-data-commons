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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.SynthesizedAnnotation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
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
	private Map<String, RepositoryMetadata<?>> configMap;

	@Nullable
	@Override
	public RepositoryBeanContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		if (!ObjectUtils.isEmpty(configMap) && configMap.containsKey(beanName)) {

			AotContext aotContext = new AotContext() {
				@Override
				public ConfigurableListableBeanFactory getBeanFactory() {
					return beanFactory;
				}
			};

			RepositoryMetadata<?> metadata = configMap.get(beanName);

			Set<Class<? extends Annotation>> identifyingAnnotations = Collections.emptySet();
			if (metadata.getConfigurationSource() instanceof RepositoryConfigurationExtensionSupport ces) {
				identifyingAnnotations = new LinkedHashSet<>(ces.getIdentifyingAnnotations());
			}

			RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.readRepositoryInformation(metadata,
					beanFactory);

			DefaultRepositoryContext ctx = new DefaultRepositoryContext();
			ctx.setAotContext(aotContext);
			ctx.setBeanName(beanName);
			ctx.setBasePackages(metadata.getBasePackages().toSet());
			ctx.setRepositoryInformation(repositoryInformation);
			ctx.setIdentifyingAnnotations(identifyingAnnotations);

			return new RepositoryBeanContribution(ctx).setModuleContribution(this::contribute);
		}

		return null;
	}

	protected void contribute(AotRepositoryContext ctx, CodeContribution contribution) {

		ctx.getResolvedTypes().forEach(it -> contributeType(it, contribution));
		ctx.getResolvedAnnotations().stream() //
				.filter(AotContributingRepositoryBeanPostProcessor::isSpringDataManagedAnnotation) //
				.map(MergedAnnotation::getType) //
				.forEach(it -> contributeType(it, contribution));
	}

	protected static boolean isSpringDataManagedAnnotation(MergedAnnotation<?> annotation) {

		if (isInDataNamespace(annotation.getType())) {
			return true;
		}

		return annotation.getMetaTypes().stream().anyMatch(AotContributingRepositoryBeanPostProcessor::isInDataNamespace);
	}

	private static boolean isInDataNamespace(Class<?> type) {
		return type.getPackage().getName().startsWith("org.springframework.data");
	}

	protected void contributeType(Class<?> type, CodeContribution contribution) {

		if (type.isAnnotation()) {
			contribution.runtimeHints().reflection().registerType(type, hint -> {
				hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
			});
			// TODO: not only package check
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

	public Map<String, RepositoryMetadata<?>> getConfigMap() {
		return configMap;
	}

	public void setConfigMap(Map<String, RepositoryMetadata<?>> configMap) {
		this.configMap = configMap;
	}
}
