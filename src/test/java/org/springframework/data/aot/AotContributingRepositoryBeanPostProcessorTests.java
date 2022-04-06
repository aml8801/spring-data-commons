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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.aot.RepositoryBeanContributionAssert.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.DecoratingProxy;
import org.springframework.data.aot.sample.ConfigWithCustomImplementation;
import org.springframework.data.aot.sample.ConfigWithFragments;
import org.springframework.data.aot.sample.ReactiveConfig;
import org.springframework.data.aot.sample.RepositoryConfigWithCustomBaseClass;
import org.springframework.data.aot.sample.SimpleCrudRepository;
import org.springframework.data.aot.sample.SimpleTxComponentCrudRepository;
import org.springframework.data.aot.sample.SimpleTxCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.transaction.interceptor.TransactionalProxy;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotContributingRepositoryBeanPostProcessorTests {

	@Test
	void simpleRepositoryNoTxManagerNoKotlinNoReactiveNoComponent() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(SimpleCrudRepository.class)
				.forRepository(SimpleCrudRepository.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(SimpleCrudRepository.MyRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(SimpleCrudRepository.MyRepo.class) // repository interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(SimpleCrudRepository.Person.class) // repository domain type
							.contributesJdkProxy(SimpleCrudRepository.MyRepo.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class) //
							.doesNotContributeJdkProxy(SimpleCrudRepository.MyRepo.class, Repository.class, TransactionalProxy.class,
									Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(SimpleCrudRepository.MyRepo.class, Repository.class, TransactionalProxy.class,
									Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test
	void simpleRepositoryWithTxManagerNoKotlinNoReactiveNoComponent() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(SimpleTxCrudRepository.class)
				.forRepository(SimpleTxCrudRepository.MyTxRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(SimpleTxCrudRepository.MyTxRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(SimpleTxCrudRepository.MyTxRepo.class) // repository interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(SimpleTxCrudRepository.Person.class) // repository domain type

							// proxies
							.contributesJdkProxy(SimpleTxCrudRepository.MyTxRepo.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class)
							.contributesJdkProxy(SimpleTxCrudRepository.MyTxRepo.class, Repository.class, TransactionalProxy.class,
									Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(SimpleTxCrudRepository.MyTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test
	void simpleRepositoryWithTxManagerNoKotlinNoReactiveButComponent() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(SimpleTxComponentCrudRepository.class)
				.forRepository(SimpleTxComponentCrudRepository.MyComponentTxRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(SimpleTxComponentCrudRepository.MyComponentTxRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(SimpleTxComponentCrudRepository.MyComponentTxRepo.class) // repository
																																																					// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(SimpleTxComponentCrudRepository.Person.class) // repository domain type

							// proxies
							.contributesJdkProxy(SimpleTxComponentCrudRepository.MyComponentTxRepo.class, SpringProxy.class,
									Advised.class, DecoratingProxy.class)
							.contributesJdkProxy(SimpleTxComponentCrudRepository.MyComponentTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.contributesJdkProxy(SimpleTxComponentCrudRepository.MyComponentTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test
	void contributesFragmentsCorrectly() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(ConfigWithFragments.class)
				.forRepository(ConfigWithFragments.RepositoryWithFragments.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithFragments.RepositoryWithFragments.class) //
				.hasFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithFragments.RepositoryWithFragments.class) // repository
							// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithFragments.Person.class) // repository domain type

							// fragments
							.contributesReflectionFor(ConfigWithFragments.CustomImplInterface1.class,
									ConfigWithFragments.CustomImplInterface1Impl.class)
							.contributesReflectionFor(ConfigWithFragments.CustomImplInterface2.class,
									ConfigWithFragments.CustomImplInterface2Impl.class)

							// proxies
							.contributesJdkProxy(ConfigWithFragments.RepositoryWithFragments.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class)
							.doesNotContributeJdkProxy(SimpleTxComponentCrudRepository.MyComponentTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(SimpleTxComponentCrudRepository.MyComponentTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test
	void contributesCustomImplementationCorrectly() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(ConfigWithCustomImplementation.class)
				.forRepository(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class) //
				.hasFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class) // repository
							// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithCustomImplementation.Person.class) // repository domain type

							// fragments
							.contributesReflectionFor(ConfigWithCustomImplementation.CustomImplInterface.class,
									ConfigWithCustomImplementation.RepositoryWithCustomImplementationImpl.class);

				});
	}

	@Test
	void contributesReactiveRepositoryCorrectly() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(ReactiveConfig.class)
				.forRepository(ReactiveConfig.CustomerRepositoryReactive.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ReactiveConfig.CustomerRepositoryReactive.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					// interface
					contribution.contributesReflectionFor(ReactiveConfig.CustomerRepositoryReactive.class) // repository
							.contributesReflectionFor(ReactiveSortingRepository.class) // base repo class
							.contributesReflectionFor(ReactiveConfig.Person.class); // repository domain type
				});
	}

	@Test
	void contributesRepositoryBaseClassCorrectly() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(
				RepositoryConfigWithCustomBaseClass.class)
						.forRepository(RepositoryConfigWithCustomBaseClass.CustomerRepositoryWithCustomBaseRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(RepositoryConfigWithCustomBaseClass.CustomerRepositoryWithCustomBaseRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					// interface
					contribution
							.contributesReflectionFor(RepositoryConfigWithCustomBaseClass.CustomerRepositoryWithCustomBaseRepo.class) // repository
							.contributesReflectionFor(RepositoryConfigWithCustomBaseClass.RepoBaseClass.class) // base repo class
							.contributesReflectionFor(RepositoryConfigWithCustomBaseClass.Person.class); // repository domain type
				});
	}

	BeanContributionBuilder computeConfiguration(Class<?> configuration) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configuration);
		ctx.refreshForAotProcessing();

		return it -> {

			String[] repoBeanNames = ctx.getBeanNamesForType(it);
			assertThat(repoBeanNames).describedAs("Unable to find repository %s in configuration %s.", it, configuration)
					.hasSize(1);

			String beanName = repoBeanNames[0];
			BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);

			AotContributingRepositoryBeanPostProcessor postProcessor = new AotContributingRepositoryBeanPostProcessor();
			postProcessor.setBeanFactory(ctx.getDefaultListableBeanFactory());

			return postProcessor.contribute((RootBeanDefinition) beanDefinition, it, beanName);
		};
	}

	interface BeanContributionBuilder {
		RepositoryBeanContribution forRepository(Class<?> repositoryInterface);
	}
}
