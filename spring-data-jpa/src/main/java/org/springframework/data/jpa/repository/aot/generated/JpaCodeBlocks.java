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

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.QueryEnhancerFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;
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

	static class QueryBlockBuilder {

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName;
		private String queryString;

		public QueryBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryBlockBuilder usingQueryVariableName(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		QueryBlockBuilder filter(String queryString) {
			this.queryString = queryString;
			return this;
		}

		CodeBlock build() {

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");
			String queryStringNameVariableName = "%sString".formatted(queryVariableName);
			builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, queryString);

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

			// parameters
			// TODO: refactor into parameter builder
			int i = 1;

			for (Parameter parameter : context.getMethod().getParameters()) {
				if (ClassUtils.isAssignable(Sort.class, parameter.getType())
						|| ClassUtils.isAssignable(Pageable.class, parameter.getType()) || ClassUtils.isAssignable(Limit.class, parameter.getType())) {
					// skip
				} else {
					// TODO: check the parameter binding
					if(!context.getMethod().isAnnotationPresent(org.springframework.data.jpa.repository.Query.class)) {
						PartTree partTree = new PartTree(context.getMethod().getName(), context.getRepositoryInformation().getDomainType());
						List<Part> list = partTree.getParts().stream().toList();

						// So we need to know about the property type and part assignment to figure out if we need to wrap the string for starting/ending/containing stuff
						// TODO: we basicall need to have StringQuery.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery
						if (i-1 < list.size()) {
							Type type = Type.SIMPLE_PROPERTY;
							Part thePart = null;
							int argCount = 0;
							for(Part part : partTree.getParts()) {
								if(argCount == i) {
									type = part.getType();
									thePart = part;
								}
								int added = 0;
								while(added != part.getNumberOfArguments()) {
									argCount++;
									added++;
									if(argCount == i) {
										type = part.getType();
										thePart = part;
									}
								}
							}
							if(thePart != null && thePart.getProperty().getTypeInformation().getType() == String.class) {
								if (Type.STARTING_WITH == type) {
									builder.addStatement("$L.setParameter(" + i + ", $T.format($S, $L))", queryVariableName, String.class, "%s%%", parameter.getName());
								} else if (Type.ENDING_WITH == type) {
									builder.addStatement("$L.setParameter(" + i + ", $T.format($S, $L))", queryVariableName, String.class, "%%%s", parameter.getName());
								} else if (Type.CONTAINING == type) {
									builder.addStatement("$L.setParameter(" + i + ", $T.format($S, $L))", queryVariableName, String.class, "%%%s%%", parameter.getName());
								} else {
									builder.addStatement("$L.setParameter(" + i + ", $L)", queryVariableName, parameter.getName());
								}
							} else {
								builder.addStatement("$L.setParameter(" + i + ", $L)", queryVariableName, parameter.getName());
							}
						} else {
							builder.addStatement("$L.setParameter(" + i + ", $L)", queryVariableName, parameter.getName());
						}
					} else {
						builder.addStatement("$L.setParameter(" + i + ", $L)", queryVariableName, parameter.getName());
					}
				}
				i++;
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
					//Long.valueOf(1L).intValue();
					builder.addStatement("$L.setFirstResult(Long.valueOf($L.getOffset()).intValue())", queryVariableName, pageableParamterName);
					builder.addStatement("$L.setMaxResults($L.getPageSize())", queryVariableName, pageableParamterName);
					builder.endControlFlow();
				}
			}

			return builder.build();
		}
	}
}
