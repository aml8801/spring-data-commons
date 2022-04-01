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

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.EnableRepositories;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/03
 */
public class AotTests {

	private static final String FRAGMENTS_PROPERTY = "repositoryFragments";

	private static final String CUSTOM_IMPLEMENTATION_PROPERTY = "customImplementation";
	private static final String REPOSITORY_BASE_CLASS_PROPERTY = "repositoryBaseClass";

	@Test
	void x2() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MyConfig.class);
		ctx.refreshForAotProcessing();

		AotContributingRepositoryBeanPostProcessor postProcessor = new AotContributingRepositoryBeanPostProcessor();
		postProcessor.setBeanFactory(ctx.getDefaultListableBeanFactory());

		Class<?> beanType = MyRepo.class;
		String beanName = ctx.getBeanNamesForType(beanType)[0];
		BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);

		RepositoryBeanContribution contribute = postProcessor.contribute((RootBeanDefinition) beanDefinition, beanType, beanName);
		System.out.println("contribute: " + contribute);

	}

	@Test
	void xxx() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MyConfig.class);
//		ctx.refreshForAotProcessing();
		ctx.refresh();

//		DefaultListableBeanFactory defaultListableBeanFactory = ctx.getDefaultListableBeanFactory();
//
//
////		defaultListableBeanFactory.getBeanNamesForType()
//
//		for(String bdName : defaultListableBeanFactory.getBeanDefinitionNames()) {
//
//			BeanDefinition beanDefinition = defaultListableBeanFactory.getBeanDefinition(bdName);
//
//			if(beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
//				if(rootBeanDefinition.getTargetType() != null) {
//					if (ClassUtils.isAssignable(Repository.class, rootBeanDefinition.getTargetType())) {
//
//						// here we could now start to extract stuff on framents
//						PropertyValue repositoryBaseClass = rootBeanDefinition.getPropertyValues().getPropertyValue("repositoryBaseClass");
//						System.out.println("repositoryBaseClass: " + repositoryBaseClass.getValue());
//					}
//				}
//			}
//		}


		RepositorySetupContributor contributor = new RepositorySetupContributor();
		BeanFactoryContribution contribution = contributor.contribute(ctx.getDefaultListableBeanFactory());
		System.out.println(contribution.toString());

		Class<?> beanType = MyRepo.class;
		String beanName = ctx.getBeanNamesForType(MyRepo.class)[0];
		BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);
		contributor.contribute((RootBeanDefinition) beanDefinition, beanType, beanName);
	}

//
//	static class DummyRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
//
//		DummyRegistrar() {
//			setResourceLoader(new DefaultResourceLoader());
//		}
//
//		@Override
//		protected Class<? extends Annotation> getAnnotation() {
//			return EnableRepositories.class;
//		}
//
//		@Override
//		protected RepositoryConfigurationExtension getExtension() {
//			return new DummyConfigurationExtension();
//		}
//	}
//
//	static class DummyConfigurationExtension extends RepositoryConfigurationExtensionSupport {
//
//		public String getRepositoryFactoryBeanClassName() {
//			return DummyRepositoryFactoryBean.class.getName();
//		}
//
//		@Override
//		protected String getModulePrefix() {
//			return "commons";
//		}
//	}


//	@Nullable
//	private BeanFactoryContribution createContribution(Class<?> type) {
//
////		GenericApplicationContext context = new GenericApplicationContext()
//		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//		beanFactory.registerBeanDefinition("configuration", new RootBeanDefinition(type));
//		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
//		pp.postProcessBeanFactory(beanFactory);
//		pp.contribute(beanFactory);
//
//		RepositorySetupContributor xxx = new RepositorySetupContributor();
//		xxx.postProcessBeanFactory(beanFactory);
//		return xxx.contribute(beanFactory);
//	}

	@EnableRepositories(includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = MyRepo.class) },
			basePackageClasses = AotTests.class, considerNestedRepositories = true)
	static class MyConfig {

	}

	static interface MyRepo extends CrudRepository<String, Person> {

	}

	static class Person {

	}

}
