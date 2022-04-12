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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.projection.EntityProjectionIntrospector.ProjectionPredicate;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.kotlin.CoroutineCrudRepository;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class RepositoryBeanContribution implements BeanInstantiationContribution {

	private final RepositoryBeanContext context;
	private final RepositoryInformation repositoryInformation;
	private Set<Class<?>> discoveredTypes;
	private Lazy<Set<MergedAnnotation<Annotation>>> discoveredAnnotations = Lazy.of(this::discoverAnnotations);
	private BiConsumer<AotRepositoryContext, CodeContribution> moduleContribution;

	public RepositoryBeanContribution(RepositoryBeanContext context, RepositoryInformation repositoryInformation,
			Collection<Class<?>> discoveredTypes) {

		this.context = context;
		this.repositoryInformation = repositoryInformation;
		this.discoveredTypes = new LinkedHashSet<>(discoveredTypes);
	}

	public RepositoryBeanContribution setModuleContribution(
			BiConsumer<AotRepositoryContext, CodeContribution> moduleContribution) {
		this.moduleContribution = moduleContribution;
		return this;
	}

	private Set<MergedAnnotation<Annotation>> discoverAnnotations() {

		Set<MergedAnnotation<Annotation>> annotations = new LinkedHashSet<>(discoveredTypes.stream().flatMap(type -> {
			return TypeUtils.resolveUsedAnnotations(type).stream();
		}).collect(Collectors.toSet()));
		annotations.addAll(TypeUtils.resolveUsedAnnotations(repositoryInformation.getRepositoryInterface()));
		return annotations;
	}

	@Override
	public void applyTo(CodeContribution contribution) {

		writeRepositoryInfo(contribution);

		if (moduleContribution != null) {
			moduleContribution.accept(new AotRepositoryContext() {
				@Override
				public RepositoryInformation getRepositoryInformation() {
					return repositoryInformation;
				}

				@Override
				public Set<Class<?>> getResolvedTypes() {
					return discoveredTypes;
				}

				@Override
				public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
					return discoveredAnnotations.get();
				}
			}, contribution);
		}
	}

	private void writeRepositoryInfo(CodeContribution contribution) {

		contribution.runtimeHints().reflection() //
				.registerType(repositoryInformation.getRepositoryInterface(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
				}) //
				.registerType(repositoryInformation.getRepositoryBaseClass(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
				}) //
				.registerType(repositoryInformation.getDomainType(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
				});

		// fragments
		for (RepositoryFragment<?> fragment : getRepositoryInformation().getFragments()) {

			contribution.runtimeHints().reflection() //
					.registerType(fragment.getSignatureContributor(), hint -> {

						hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
						if (!fragment.getSignatureContributor().isInterface()) {
							hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
						}
					});
		}

		// the surrounding proxy
		contribution.runtimeHints().proxies() // repository proxy
				.registerJdkProxy(repositoryInformation.getRepositoryInterface(), SpringProxy.class, Advised.class,
						DecoratingProxy.class);

		context.ifTransactionManagerPresent(txMgrBeanNames -> {

			contribution.runtimeHints().proxies() // transactional proxy
					.registerJdkProxy(transactionalRepositoryProxy());

			if (AnnotationUtils.findAnnotation(repositoryInformation.getRepositoryInterface(), Component.class) != null) {

				TypeReference[] source = transactionalRepositoryProxy();
				TypeReference[] txProxyForSerializableComponent = Arrays.copyOf(source, source.length + 1);
				txProxyForSerializableComponent[source.length] = TypeReference.of(Serializable.class);
				contribution.runtimeHints().proxies().registerJdkProxy(txProxyForSerializableComponent);
			}
		});

		// reactive repo
		if (repositoryInformation.isReactiveRepository()) {
			// TODO: do we still need this and how to configure it?
			// registry.initialization().add(NativeInitializationEntry.ofBuildTimeType(configuration.getRepositoryInterface()));
		}

		// Kotlin
		if (ClassUtils.isAssignable(CoroutineCrudRepository.class, repositoryInformation.getRepositoryInterface())) {

			contribution.runtimeHints().reflection() //

					.registerType(CoroutineCrudRepository.class, hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(Repository.class, hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(Iterable.class, hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(TypeReference.of("kotlinx.coroutines.flow.Flow"), hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(TypeReference.of("kotlin.collections.Iterable"), hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(TypeReference.of("kotlin.Unit"), hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(TypeReference.of("kotlin.Long"), hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					}) //
					.registerType(TypeReference.of("kotlin.Boolean"), hint -> {
						hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
					});
		}

		// repository methods
		repositoryInformation.getQueryMethods().map(repositoryInformation::getReturnedDomainClass)
				.filter(Class::isInterface).forEach(type -> {
					if (ProjectionPredicate.typeHierarchy().test(type, repositoryInformation.getDomainType())) {
						contributeProjection(type, contribution);
					}
				});
	}

	private TypeReference[] transactionalRepositoryProxy() {

		return new TypeReference[] { TypeReference.of(repositoryInformation.getRepositoryInterface()),
				TypeReference.of(Repository.class),
				TypeReference.of("org.springframework.transaction.interceptor.TransactionalProxy"),
				TypeReference.of("org.springframework.aop.framework.Advised"), TypeReference.of(DecoratingProxy.class) };
	}

	protected boolean contributeTypeInfo(Class<?> type) {
		return true;
	}

	// protected void contributeType(Class<?> type, CodeContribution contribution) {
	// if (type.isAnnotation()) {
	// contribution.runtimeHints().reflection().registerType(type, hint -> {
	// hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
	// });
	// return;
	// }
	// if (type.isInterface()) {
	// contribution.runtimeHints().reflection().registerType(type, hint -> {
	// hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
	// });
	// return;
	// }
	// if (type.isPrimitive()) {
	// return;
	// }
	// contribution.runtimeHints().reflection().registerType(type, hint -> {
	// hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
	// });
	// }

	protected void contributeProjection(Class<?> type, CodeContribution contribution) {

		contribution.runtimeHints().proxies().registerJdkProxy(type, TargetAware.class, SpringProxy.class,
				DecoratingProxy.class);
	}

	protected void contributeRepositoryInformation(RepositoryInformation repositoryInformation,
			CodeContribution contribution) {
		// todo
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public Set<Class<?>> getDiscoveredTypes() {
		return discoveredTypes;
	}

	public void setDiscoveredTypes(Set<Class<?>> discoveredTypes) {
		this.discoveredTypes = discoveredTypes;
	}
}
