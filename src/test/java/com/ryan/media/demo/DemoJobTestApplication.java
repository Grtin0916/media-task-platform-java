package com.ryan.media.demo;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
@Configuration(proxyBeanMethods=false)
@Profile("demo-job-it")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class,RedisRepositoriesAutoConfiguration.class})
@ComponentScan(basePackageClasses=DemoJobController.class)
@Import(DemoJobTestSecurity.class)
public class DemoJobTestApplication {}
