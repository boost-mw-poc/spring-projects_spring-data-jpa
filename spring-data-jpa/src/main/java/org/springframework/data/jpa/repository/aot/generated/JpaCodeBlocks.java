/*
 * Copyright 2025 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.QueryEnhancerFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class JpaCodeBlocks {

	private static final Pattern PARAMETER_BINDING_PATTERN = Pattern.compile("\\?(\\d+)");

	static QueryBlockBuilder queryBlockBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryBlockBuilder(context);
	}

	static QueryExecutionBlockBuilder queryExecutionBlockBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryExecutionBlockBuilder(context);
	}

	static class QueryExecutionBlockBuilder {

		AotRepositoryMethodGenerationContext context;
		private String queryVariableName;

		public QueryExecutionBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryExecutionBlockBuilder referencing(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		CodeBlock build() {

			Builder builder = CodeBlock.builder();

			boolean isProjecting = context.getActualReturnType() != null
					&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
							context.getActualReturnType());
			Object actualReturnType = isProjecting ? context.getActualReturnType()
					: context.getRepositoryInformation().getDomainType();

			builder.add("\n");

			if (context.returnsSingleValue()) {
				if (context.returnsOptionalValue()) {
					builder.addStatement("return $T.ofNullable(($T) $L.getSingleResultOrNull())", Optional.class,
							actualReturnType, queryVariableName);
				} else if (context.isCountMethod()) {
					// TODO: count
					builder.addStatement("return ($T) $L.getSingleResultOrNull()", context.getReturnType(), queryVariableName);
				} else if (context.isExistsMethod()) {
					// TODO: exists
					builder.addStatement("return ($T) $L.getSingleResultOrNull()", context.getReturnType(), queryVariableName);
				} else {
					builder.addStatement("return ($T) $L.getSingleResultOrNull()", context.getReturnType(), queryVariableName);
				}
			} else if (context.returnsPage()) {
				// TODO: page
				builder.addStatement("return ($T) query.getResultList()", context.getReturnType());
			} else if (context.returnsSlice()) {
				// TODO: slice
				builder.addStatement("return ($T) query.getResultList()", context.getReturnType());
			} else {

				builder.addStatement("return ($T) query.getResultList()", context.getReturnType());
			}

			return builder.build();

		}
	}

	static class QueryBlockBuilder {

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName;
		private AotStringQuery query;

		public QueryBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryBlockBuilder usingQueryVariableName(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		QueryBlockBuilder filter(String queryString) {
			return filter(AotStringQuery.of(queryString));
		}

		QueryBlockBuilder filter(AotStringQuery query) {
			this.query = query;
			return this;
		}

		CodeBlock build() {

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");
			String queryStringNameVariableName = "%sString".formatted(queryVariableName);
			builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, query.getQueryString());

			// sorting
			// TODO: refactor into sort builder
			{
				String sortParameterName = context.getSortParameterName();
				if (sortParameterName == null && context.getPageableParameterName() != null) {
					sortParameterName = "%s.getSort()".formatted(context.getPageableParameterName());
				}

				if (StringUtils.hasText(sortParameterName)) {
					builder.beginControlFlow("if($L.isSorted())", sortParameterName);

					builder.addStatement("$T declaredQuery = $T.of($L, false)", DeclaredQuery.class, DeclaredQuery.class,
							queryStringNameVariableName);
					builder.addStatement("$L = $T.forQuery(declaredQuery).applySorting($L)", queryStringNameVariableName,
							QueryEnhancerFactory.class, sortParameterName);

					builder.endControlFlow();
				}
			}

			builder.addStatement("$T $L = this.$L.createQuery($L)", Query.class, queryVariableName,
					context.fieldNameOf(EntityManager.class), queryStringNameVariableName);

			for (ParameterBinding binding : query.parameterBindings()) {

				Object prepare = binding.prepare("s");
				if (prepare instanceof String prepared && !prepared.equals("s")) {
					String format = prepared.replaceAll("%", "%%").replace("s", "%s");
					if (binding.getIdentifier().hasPosition()) {
						builder.addStatement("$L.setParameter($L, $S.formatted($L))", queryVariableName,
								binding.getIdentifier().getPosition(), format,
								context.getParameterNameOfPosition(binding.getIdentifier().getPosition() - 1));
					} else {
						builder.addStatement("$L.setParameter($S, $S.formatted($L))", queryVariableName,
								binding.getIdentifier().getName(), format, binding.getIdentifier().getName());
					}
				} else {
					if (binding.getIdentifier().hasPosition()) {
						builder.addStatement("$L.setParameter($L, $L)", queryVariableName, binding.getIdentifier().getPosition(),
								context.getParameterNameOfPosition(binding.getIdentifier().getPosition() - 1));
					} else {
						builder.addStatement("$L.setParameter($S, $L)", queryVariableName, binding.getIdentifier().getName(),
								binding.getIdentifier().getName());
					}
				}
			}

			{
				String limitParameterName = context.getLimitParameterName();

				if (StringUtils.hasText(limitParameterName)) {
					builder.beginControlFlow("if($L.isLimited())", limitParameterName);
					builder.addStatement("$L.setMaxResults($L.max())", queryVariableName, limitParameterName);
					builder.endControlFlow();
				}
			}

			{
				String pageableParamterName = context.getPageableParameterName();
				if (StringUtils.hasText(pageableParamterName)) {
					builder.beginControlFlow("if($L.isPaged())", pageableParamterName);
					builder.addStatement("$L.setFirstResult(Long.valueOf($L.getOffset()).intValue())", queryVariableName,
							pageableParamterName);
					builder.addStatement("$L.setMaxResults($L.getPageSize())", queryVariableName, pageableParamterName);
					builder.endControlFlow();
				}
			}

			return builder.build();
		}
	}
}
