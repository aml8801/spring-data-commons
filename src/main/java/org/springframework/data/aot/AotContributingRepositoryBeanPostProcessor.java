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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.projection.EntityProjectionIntrospector;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.RepositoryConfiguration;
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
	// set the Map<String, RepositoryConfiguration<?>> configurationsByRepositoryName  //

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

		if(!ObjectUtils.isEmpty(configMap)) {
			if(x.compareAndSet(false, true)) {
				AotBeanContext beanContext = new AotBeanContext(beanName, beanDefinition, beanFactory);
				for(RepositoryMetadata<?> metadata : configMap.values()) {
					RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.readRepositoryInformation(metadata, beanFactory);
					return new RepositoryBeanContribution(beanContext, repositoryInformation, discoverTypes(repositoryInformation, typeFilter()));
				}
			}
			return null;
		}

		if (!ClassUtils.isAssignable(Repository.class, beanDefinition.getTargetType())) {
			return null;
		}

		AotBeanContext beanContext = new AotBeanContext(beanName, beanDefinition, beanFactory);
		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.readRepositoryInformation(beanContext);

		return new RepositoryBeanContribution(beanContext, repositoryInformation, discoverTypes(repositoryInformation, typeFilter()));
	}

	protected Set<Class<?>> discoverTypes(RepositoryInformation repositoryInformation, Predicate<Class<?>> filter) {


		// find all the types if you will
		Set<Class<?>> types = new LinkedHashSet<>();

		List<Class<?>> collectedTypes = TypeCollector.inspect(repositoryInformation.getDomainType()).list();
		types.addAll(collectedTypes);


		// TODO:

		// read domain type info from the repo

		// look for additioinal types with the filter predicate

		
//repositoryInformation.isQueryMethod()

		// is it a projection interface
		// EntityProjectionIntrospector.ProjectionPredicate.typeHierarchy().test()

		return types;
	}



//	private void writeDomainTypeConfiguration(Class<?> type) {
//
//		typeModelProcessor.inspect(type).forEach((domainType) -> {
//			if (seen.contains(domainType.getType())) {
//				return;
//			}
//			seen.add(domainType.getType());
//			if (domainType.isPartOf(SPRING_DATA_DOMAIN_TYPES_PACKAGES)) {  // eg. Page, Slice
//				return;
//			}
//			if (SimpleTypeHolder.DEFAULT.isSimpleType(domainType.getType())) { // eg. String, ...
//				return;
//			}
//			Builder reflectBuilder = registry.reflection().forType(domainType.getType());
//			if(domainType.hasDeclaredClasses()) {
//				reflectBuilder.withAccess(TypeAccess.DECLARED_CLASSES);
//			}
//			if (domainType.hasMethods()) {
//				reflectBuilder.withExecutables(domainType.getMethods().toArray(new Method[0]));
//			}
//			else {
//				if (domainType.isPartOf("java")) {
//					reflectBuilder.withAccess(TypeAccess.PUBLIC_METHODS);
//				}
//			}
//			if (domainType.hasFields()) {
//				reflectBuilder.withFields(domainType.getFields().toArray(new Field[0]));
//			}
//			if (domainType.hasPersistenceConstructor()) {
//				reflectBuilder.withExecutables(domainType.getPersistenceConstructor());
//			}
//			else {
//				reflectBuilder.withAccess(TypeAccess.DECLARED_CONSTRUCTORS);
//			}
//			domainType.doWithAnnotatedElements(this::writeAnnotationConfigurationFor);
//		});
//	}

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
