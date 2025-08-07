package org.evergreen.mailsearch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yourcompany.mailsearch.mapper")
public class InternalMailSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternalMailSearchApplication.class, args);
    }

}