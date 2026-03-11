package ro.unibuc.prodeng;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.CreateTodoRequest;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.service.TodoService;
import ro.unibuc.prodeng.service.UserService;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableMongoRepositories
@EnableMongoAuditing
public class ProdEngApplication {

	@Autowired
	private UserService userService;

	@Autowired
	private TodoService todoService;

	@Autowired
	private UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(ProdEngApplication.class, args);
	}

	@PostConstruct
	public void runAfterObjectCreated() {
		if (userRepository.findByEmail("frodo@theshire.me").isEmpty()) {
			CreateUserRequest userRequest = new CreateUserRequest("Frodo Baggins", "frodo@theshire.me", new ArrayList<>());
			userService.createUser(userRequest);
			todoService.createTodo(new CreateTodoRequest("Take the ring to Mordor", "frodo@theshire.me"));
		}
	}
}
