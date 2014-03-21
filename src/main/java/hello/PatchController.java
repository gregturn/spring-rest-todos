/*
 * Copyright 2014 the original author or authors.
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
package hello;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.diff.JsonDiff;

/**
 * @author Roy Clarkson
 * @author Craig Walls
 */
@RestController
@RequestMapping("/todos")
public class PatchController {
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	private TodoRepository repository;
	
	@Autowired
	public PatchController(TodoRepository repository) {
		this.repository = repository;
	}

	@RequestMapping(method=RequestMethod.PATCH, 
					consumes={"application/json", "application/json-patch+json"}, 
					produces = "application/json")
	@ResponseStatus(HttpStatus.NO_CONTENT) // TODO: Consider what we *should* be returning here.
	public void patch(JsonPatch patch) {
		try {
			JsonNode patchedTodos = patch.apply(getTodosJson());
			updateTodosFromJson(patchedTodos);
		} catch (JsonPatchException e) {
			logger.error("Failed to apply patch! Returning unmodified list.", e);
		}
	}

	@RequestMapping(value = "/diff", 
					method = RequestMethod.POST, 
					consumes="application/json", 
					produces = {"application/json", "application/json-patch+json"})
	public JsonNode diff(@RequestBody JsonNode modifiedTodos) {
		return JsonDiff.asJson(getTodosJson(), modifiedTodos);
	}

	
	
	// private helpers
	private JsonNode getTodosJson() {
		List<Todo> allTodos = repository.findAll();
		return objectMapper.convertValue(allTodos, JsonNode.class);
	}

	private void updateTodosFromJson(JsonNode todosJson) {
		Todo[] todoArray = objectMapper.convertValue(todosJson, Todo[].class);
		List<Todo> todoList = Arrays.asList(todoArray);
		repository.save(todoList);
	}

}
