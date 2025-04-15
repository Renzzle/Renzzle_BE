package com.renzzle.backend.support;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.global.config.QueryDSLConfig;
import com.renzzle.backend.global.init.DataInitializer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@Import({DataInitializer.class, QueryDSLConfig.class})
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface DataJpaTestWithInitContainers {
}
