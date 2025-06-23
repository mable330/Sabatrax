package com.example.demo; // Make sure this package declaration matches your folder structure

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication; // Make sure this import is present

@SpringBootApplication // <--- THIS ANNOTATION MUST BE PRESENT AND CORRECT
public class DemoApplication {

	public static void main(String[] args) { // <--- THIS METHOD MUST BE PRESENT AND CORRECT
		SpringApplication.run(DemoApplication.class, args);
	}

}