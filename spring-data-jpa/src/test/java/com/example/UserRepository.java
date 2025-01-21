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
package com.example;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Christoph Strobl
 */
public interface UserRepository extends CrudRepository<User, Integer> {

	@Query("select u from User u where u.firstname = ?1")
	List<User> findAllUsingAnnotatedJpqlQuery(String firstname);

	List<User> findByLastname(String lastname);

	List<User> findByLastname(String lastname, Sort sort);

	List<User> findByLastname(String lastname, Pageable page);

	List<User> findByLastnameOrderByFirstname(String lastname);

	User findByEmailAddress(String emailAddress);
}
