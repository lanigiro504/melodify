package com.melodify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.melodify.mapper")
public class MelodifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelodifyBackendApplication.class, args);
	}

}
