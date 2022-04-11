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

import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanFactoryPostProcessor;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.Repository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/03
 */
public class RepositorySetupContributor implements AotContributingBeanFactoryPostProcessor, AotContributingBeanPostProcessor {

	@Nullable
	@Override
	public BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory) {


		/*
			for (String beanName : beanFactory.getBeanNamesForType(RepositoryFactoryInformation.class, false, false)) {
				repositoryConfigurations.add(configurationFactory.forBeanName(beanName));
			}
		 */

		System.out.println("contributing from AotContributingBeanFactoryPostProcessor");

		// all beans for FB though must not be one.
		// beanFactory.getBeanNamesForType(Repository.class);

		for(String bdName : beanFactory.getBeanDefinitionNames()) {

			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(bdName);

			if(beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {


				// do we need to write out code here? this would be something we'd need to do for the jpa stuff


				if(rootBeanDefinition.getTargetType() != null) {


					//massage the type we want to


					// how would I find this and why is RepositoryInformation no longer here?



					if (ClassUtils.isAssignable(Repository.class, rootBeanDefinition.getTargetType())) {

						System.out.println("found repo factory bean for: " + rootBeanDefinition.getTargetType());

						// here we could now start to extract stuff on framents
						PropertyValue repositoryBaseClass = rootBeanDefinition.getPropertyValues().getPropertyValue("repositoryBaseClass");
						System.out.println("repositoryBaseClass: " + repositoryBaseClass.getValue());
					}
				}
			}
		}

		System.out.println("RepositorySetupContributor contribute: " + beanFactory);

		return new BeanFactoryContribution() {

			@Override
			public void applyTo(BeanFactoryInitialization initialization) {
				System.out.println("initialization: " + initialization);
			}
		};

	}



	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("RepositorySetupContributor postProcessBeanFactory: " + beanFactory);
		AotContributingBeanFactoryPostProcessor.super.postProcessBeanFactory(beanFactory);
	}

	@Nullable
	@Override
	public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {


		// how to run a classpath scan to scan for stuff - done by boot -

		// for all of this it would be nice to have a fitler of some sort that make sure thing only get called if matched

		if (!ClassUtils.isAssignable(Repository.class, beanDefinition.getTargetType())) {
			return null;
		}

		// this is a RepositoryFactoryInformation


		// do some native stuff down here

		return new BeanInstantiationContribution() {
			@Override
			public void applyTo(CodeContribution contribution) {


				// native: contribution.runtimeHints().
				// aot: contribution.statements().
				// TODO: this is where we provide reflection stuff and could provide code for the entity instantiators
				//contribution.runtimeHints().
			}
		};
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
