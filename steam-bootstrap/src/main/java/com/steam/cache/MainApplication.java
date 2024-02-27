package com.steam.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication()
@ComponentScan(basePackages = {"com.steam.cache"})
public class MainApplication extends SpringBootServletInitializer {

    public static void main(String[] args){
        SpringApplication.run(MainApplication.class, args);
    }
}
