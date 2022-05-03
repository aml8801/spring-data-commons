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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.ManagedTypes;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class AotContributingRepositoryBeanPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;
	private Map<String, RepositoryMetadata<?>> configMap;

	@Nullable
	@Override
	public RepositoryBeanContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		if (ObjectUtils.isEmpty(configMap) || !configMap.containsKey(beanName)) {
			return null;
		}

		RepositoryMetadata<?> metadata = configMap.get(beanName);

		Set<Class<? extends Annotation>> identifyingAnnotations = Collections.emptySet();
		String moduleName = null;
		if (metadata.getConfigurationSource() instanceof RepositoryConfigurationExtensionSupport ces) {
			identifyingAnnotations = new LinkedHashSet<>(ces.getIdentifyingAnnotations());
			moduleName = ces.getModuleName();
		}

		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.readRepositoryInformation(metadata,
				beanFactory);

		DefaultRepositoryContext ctx = new DefaultRepositoryContext();
		ctx.setAotContext(() -> beanFactory);
		ctx.setBeanName(beanName);
		ctx.setBasePackages(metadata.getBasePackages().toSet());
		ctx.setRepositoryInformation(repositoryInformation);
		ctx.setIdentifyingAnnotations(identifyingAnnotations);

		/* TODO: contribute ManagedTypesBean maybe based on the config */
//		if (!ObjectUtils.isEmpty(beanFactory.getBeanNamesForType(ManagedTypes.class))
//				&& beanFactory instanceof DefaultListableBeanFactory bf) {
//
//			Set<Class<?>> classes = new TypeScanner(bf.getBeanClassLoader()).scanForTypesAnnotatedWith(identifyingAnnotations)
//					.inPackages(ctx.getBasePackages());
//			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ManagedTypes.class)
//					.setFactoryMethod("of").addConstructorArgValue(classes);
//			bf.registerBeanDefinition(
//					String.format("%s.managed-types", StringUtils.hasText(moduleName) ? moduleName : beanName),
//					builder.getBeanDefinition());
//		}

		/*
		 * Help the AOT processing render the FactoryBean<T> type correctly that is used to tell the outcome of the FB.
		 * We just need to set the target repo type of the RepositoryFactoryBeanSupport while keeping the actual ID and DomainType set to object.
		 * If the generics do not match we do not try to resolve and remap them, but rather set the ObjectType attribute.
		 */
		ResolvableType resolvedFactoryBean = ResolvableType.forClass(
				ctx.resolveType(metadata.getRepositoryFactoryBeanClassName()).orElse(RepositoryFactoryBeanSupport.class));
		if (resolvedFactoryBean.getGenerics().length == 3) {
			beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(
					ctx.resolveType(metadata.getRepositoryFactoryBeanClassName()).orElse(RepositoryFactoryBeanSupport.class),
					repositoryInformation.getRepositoryInterface(), Object.class, Object.class));
		} else {
			beanDefinition.setTargetType(resolvedFactoryBean);
			beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, repositoryInformation.getRepositoryInterface());
		}

		return new RepositoryBeanContribution(ctx).setModuleContribution(this::contribute);
	}

	protected void contribute(AotRepositoryContext ctx, CodeContribution contribution) {

		ctx.getResolvedTypes() //
				.stream() //
				.filter(it -> !isJavaOrPrimitiveType(it)) //
				.forEach(it -> contributeType(it, contribution));

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

	private static boolean isJavaOrPrimitiveType(Class<?> type) {
		if (TypeUtils.type(type).isPartOf("java") || type.isPrimitive() || ClassUtils.isPrimitiveArray(type)) {
			return true;
		}
		return false;
	}

	protected void contributeType(Class<?> type, CodeContribution contribution) {
		TypeContributor.contribute(type, it -> true, contribution);
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
