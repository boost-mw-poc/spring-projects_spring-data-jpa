/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.util.Lazy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.UserRepository;

/**
 * @author Christoph Strobl
 */
class JpaRepositoryContributorUnitTests {

	private static Verifyer generated;

	@BeforeAll
	static void beforeAll() {

		TestJpaAotRepsitoryContext aotContext = new TestJpaAotRepsitoryContext(UserRepository.class, null);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);

		new JpaRepsoitoryContributor(aotContext).contribute(generationContext);

		AbstractBeanDefinition emBeanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator")
				.setFactoryMethod("createSharedEntityManager").addConstructorArgReference("entityManagerFactory")
				.setLazyInit(true).getBeanDefinition();

		AbstractBeanDefinition aotGeneratedRepository = BeanDefinitionBuilder
				.genericBeanDefinition("com.example.UserRepositoryImpl__Aot")
				.addConstructorArgReference("jpaSharedEM_entityManagerFactory").getBeanDefinition();

		generated = generateContext(generationContext) //
				.registerBeansFrom(new ClassPathResource("infrastructure.xml"))
				.register("jpaSharedEM_entityManagerFactory", emBeanDefinition)
				.register("aotUserRepository", aotGeneratedRepository);
	}

	@BeforeEach
	public void beforeEach() {

		generated.doWithBean(EntityManager.class, em -> {

			em.createQuery("DELETE FROM %s".formatted(User.class.getName())).executeUpdate();

			User luke = new User("Luke", "Skywalker", "luke@jedi.org");
			em.persist(luke);

			User leia = new User("Leia", "Organa", "leia@resistance.gov");
			em.persist(leia);

			User han = new User("Han", "Solo", "han@smuggler.net");
			em.persist(han);

			User chewbacca = new User("Chewbacca", "n/a", "chewie@smuggler.net");
			em.persist(chewbacca);

			User yoda = new User("Yoda", "n/a", "yoda@jedi.org");
			em.persist(yoda);

			User vader = new User("Anakin", "Skywalker", "vader@empire.com");
			em.persist(vader);

			User kylo = new User("Ben", "Solo", "ben@new-empire.com");
			em.persist(kylo);
		});
	}

	@Test
	public void testMulti() {
		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastname", "Skywalker").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org",
					"vader@empire.com");
		});
	}

	@Test
	public void testMultiSorted() {
		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastnameOrderByFirstname", "Skywalker").onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("vader@empire.com", "luke@jedi.org");
		});
	}

	@Test
	public void testMultiDynamicSorted() {
		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastname", "Skywalker", Sort.by("firstname")).onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("vader@empire.com", "luke@jedi.org");
		});
	}

	@Test
	public void testMultiDynamicPaged() {
		generated.verify(methodInvoker -> {

			List<User> users = methodInvoker.invoke("findByLastname", "Skywalker", PageRequest.of(0, 10, Sort.by("firstname"))).onBean("aotUserRepository");
			assertThat(users).extracting(User::getEmailAddress).containsExactly("vader@empire.com", "luke@jedi.org");
		});
	}

	@Test
	public void testSingle() {
		generated.verify(methodInvoker -> {

			User user = methodInvoker.invoke("findByEmailAddress", "luke@jedi.org").onBean("aotUserRepository");
			assertThat(user.getLastname()).isEqualTo("Skywalker");
		});
	}

	static GeneratedContextBuilder generateContext(TestGenerationContext generationContext) {
		return new GeneratedContextBuilder(generationContext);
	}

	static class GeneratedContextBuilder implements Verifyer {

		TestGenerationContext generationContext;
		Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
		Resource xmlBeanDefinitions;
		Lazy<DefaultListableBeanFactory> lazyFactory;

		public GeneratedContextBuilder(TestGenerationContext generationContext) {

			this.generationContext = generationContext;
			this.lazyFactory = Lazy.of(() -> {
				DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
				TestCompiler.forSystem().with(generationContext).compile(compiled -> {

					freshBeanFactory.setBeanClassLoader(compiled.getClassLoader());
					if (xmlBeanDefinitions != null) {
						XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(freshBeanFactory);
						beanDefinitionReader.loadBeanDefinitions(xmlBeanDefinitions);
					}

					for (Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
						freshBeanFactory.registerBeanDefinition(entry.getKey(), entry.getValue());
					}
				});
				return freshBeanFactory;
			});
		}

		GeneratedContextBuilder register(String name, BeanDefinition beanDefinition) {
			this.beanDefinitions.put(name, beanDefinition);
			return this;
		}

		GeneratedContextBuilder registerBeansFrom(Resource xmlBeanDefinitions) {
			this.xmlBeanDefinitions = xmlBeanDefinitions;
			return this;
		}

		public Verifyer verify(Consumer<GeneratedContext> methodInvoker) {
			methodInvoker.accept(new GeneratedContext(lazyFactory));
			return this;
		}

	}

	interface Verifyer {
		Verifyer verify(Consumer<GeneratedContext> methodInvoker);

		default <T> void doWithBean(Class<T> type, Consumer<T> runit) {
			verify(ctx -> {

				boolean isEntityManager = type == EntityManager.class;
				T bean = ctx.delegate.get().getBean(type);

				if (!isEntityManager) {
					runit.accept(bean);
				} else {

					PlatformTransactionManager txMgr = ctx.delegate.get().getBean(PlatformTransactionManager.class);
					new TransactionTemplate(txMgr).execute(action -> {
						runit.accept(bean);
						return "ok";
					});

				}
			});
		}
	}

	static class GeneratedContext {

		private Supplier<DefaultListableBeanFactory> delegate;

		public GeneratedContext(Supplier<DefaultListableBeanFactory> defaultListableBeanFactory) {
			this.delegate = defaultListableBeanFactory;
		}

		InvocationBuilder invoke(String method, Object... arguments) {

			return new InvocationBuilder() {
				@Override
				public <T> T onBean(String beanName) {
					DefaultListableBeanFactory defaultListableBeanFactory = delegate.get();

					Object bean = defaultListableBeanFactory.getBean(beanName);
					return ReflectionTestUtils.invokeMethod(bean, method, arguments);
				}
			};
		}

		interface InvocationBuilder {
			<T> T onBean(String beanName);
		}

	}

}
