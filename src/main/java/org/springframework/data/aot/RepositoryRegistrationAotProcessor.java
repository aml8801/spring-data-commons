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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link BeanRegistrationAotProcessor} responsible for data repositories.
 * <p>
 * Processes {@link RepositoryFactoryBeanSupport repository factory beans} to provide generic type information to
 * the AOT tooling to allow deriving target type from the {@link RootBeanDefinition bean definition}. If generic types
 * do not match due to customization of the factory bean by the user, at least the target repository type is provided
 * via the {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}.
 * </p>
 * <p>
 * Via {@link #contribute(AotRepositoryContext, GenerationContext)} stores can provide custom logic for contributing
 * additional (eg. reflection) configuration. By default reflection configuration will be added for types reachable from
 * the repository declaration and query methods as well as all used {@link Annotation annotations} from the
 * {@literal org.springframework.data} namespace.
 * </p>
 * The processor is typically configured via {@link RepositoryConfigurationExtension#getRepositoryAotProcessor()}
 * and gets added by the {@link org.springframework.data.repository.config.RepositoryConfigurationDelegate}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
 * @since 3.0
 */
public class RepositoryRegistrationAotProcessor implements BeanRegistrationAotProcessor, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(RepositoryRegistrationAotProcessor.class);

	private ConfigurableListableBeanFactory beanFactory;

	private Map<String, RepositoryMetadata<?>> configMap;

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(@NonNull RegisteredBean registeredBean) {

		BeanRegistrationAotContribution repositoryBeanAotContribution = (generationContext, beanRegistrationCode) -> {

			String repositoryBeanName = registeredBean.getBeanName();

			RepositoryMetadata<?> repositoryMetadata = configMap.get(repositoryBeanName);

			RepositoryInformation repositoryInformation = resolveRepositoryInformation(repositoryMetadata);

			Set<Class<? extends Annotation>> identifyingAnnotations = resolveIdentifyingAnnotations(repositoryMetadata);

			DefaultAotRepositoryContext repositoryContext = new DefaultAotRepositoryContext();

			repositoryContext.setAotContext(() -> beanFactory);
			repositoryContext.setBeanName(repositoryBeanName);
			repositoryContext.setBasePackages(repositoryMetadata.getBasePackages().toSet());
			repositoryContext.setIdentifyingAnnotations(identifyingAnnotations);
			repositoryContext.setRepositoryInformation(repositoryInformation);

			/*
			 * Help the AOT processing render the FactoryBean<T> type correctly that is used to tell the outcome of the FB.
			 * We just need to set the target repo type of the RepositoryFactoryBeanSupport while keeping the actual ID and DomainType set to object.
			 * If the generics do not match we do not try to resolve and remap them, but rather set the ObjectType attribute.
			 */
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Enhancing repository factory bean definition %s.", repositoryBeanName));
			}

			Class<?> repositoryFactoryBeanType = repositoryContext
					.resolveType(repositoryMetadata.getRepositoryFactoryBeanClassName())
					.orElse(RepositoryFactoryBeanSupport.class);

			ResolvableType resolvedRepositoryFactoryBeanType = ResolvableType.forClass(repositoryFactoryBeanType);

			RootBeanDefinition repositoryBeanDefinition = registeredBean.getMergedBeanDefinition();

			if (resolvedRepositoryFactoryBeanType.getGenerics().length == 3) {
				repositoryBeanDefinition.setTargetType(ResolvableType.forClassWithGenerics(repositoryFactoryBeanType,
						repositoryInformation.getRepositoryInterface(), Object.class, Object.class));
			} else {
				repositoryBeanDefinition.setTargetType(resolvedRepositoryFactoryBeanType);
				repositoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, repositoryInformation.getRepositoryInterface());
			}

			new RepositoryBeanContribution(repositoryContext).setModuleContribution(this::contribute);
		};

		return isRepositoryBeanPresent(registeredBean) ? repositoryBeanAotContribution : null;
	}

	@NonNull
	private RepositoryInformation resolveRepositoryInformation(RepositoryMetadata<?> repositoryMetadata) {
		return RepositoryBeanDefinitionReader.readRepositoryInformation(repositoryMetadata, beanFactory);
	}

	@NonNull
	private Set<Class<? extends Annotation>> resolveIdentifyingAnnotations(RepositoryMetadata<?> repositoryMetadata) {

		return repositoryMetadata.getConfigurationSource() instanceof RepositoryConfigurationExtensionSupport configurationExtension
				? new LinkedHashSet<>(configurationExtension.getIdentifyingAnnotations())
				: Collections.emptySet();
	}

	private boolean isRepositoryBeanPresent(@Nullable RegisteredBean bean) {
		return bean != null && !ObjectUtils.isEmpty(configMap) && configMap.containsKey(bean.getBeanName());
	}

	protected void contribute(AotRepositoryContext context, GenerationContext generationContext) {

		context.getResolvedTypes().stream()
				.filter(it -> !isJavaOrPrimitiveType(it))
				.forEach(it -> contributeType(it, generationContext));

		context.getResolvedAnnotations().stream()
				.filter(RepositoryRegistrationAotProcessor::isSpringDataManagedAnnotation)
				.map(MergedAnnotation::getType)
				.forEach(it -> contributeType(it, generationContext));
	}

	private static boolean isJavaOrPrimitiveType(Class<?> type) {

		return TypeUtils.type(type).isPartOf("java")
			|| type.isPrimitive()
			|| ClassUtils.isPrimitiveArray(type);
	}

	protected static boolean isSpringDataManagedAnnotation(MergedAnnotation<?> annotation) {

		return isInDataNamespace(annotation.getType())
				|| annotation.getMetaTypes().stream().anyMatch(RepositoryRegistrationAotProcessor::isInDataNamespace);
	}

	private static boolean isInDataNamespace(Class<?> type) {
		return type != null && type.getPackage().getName().startsWith(TypeContributor.DATA_NAMESPACE);
	}

	protected void contributeType(Class<?> type, GenerationContext generationContext) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Contributing type information for %s.", type));
		}

		TypeContributor.contribute(type, it -> true, generationContext);
	}

	public Predicate<Class<?>> typeFilter() { // like only document ones.
		return it -> true;
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {

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
