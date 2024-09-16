package com.renzzle.backend.test;

import com.renzzle.backend.domain.test.dao.JdbcEntityDao;
import com.renzzle.backend.domain.test.dao.TestRepository;
import com.renzzle.backend.domain.test.domain.JdbcEntity;
import com.renzzle.backend.domain.test.domain.TestEntity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import java.util.Optional;

@SpringBootTest
public class DBTest {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private JdbcEntityDao jdbcEntityDao;

    @Test
    @Transactional
    public void jpaCRDTest() {
        final String testName = "test";
        // create
        TestEntity entity = TestEntity.builder().name(testName).build();
        TestEntity saveResult = testRepository.save(entity);

        // read
        Optional<TestEntity> readResult = testRepository.findById(saveResult.getId());
        Assertions.assertTrue(readResult.isPresent());
        Assertions.assertEquals(readResult.get().getId(), saveResult.getId());
        Assertions.assertEquals(readResult.get().getName(), saveResult.getName());

        // delete
        testRepository.deleteById(saveResult.getId());
        readResult = testRepository.findById(saveResult.getId());
        Assertions.assertTrue(readResult.isEmpty());
    }

    @Test
    @Transactional
    public void jdbcCRDTest() {
        final String testName = "test";
        // create
        long resultId = jdbcEntityDao.save(testName);

        // read
        JdbcEntity readResult = jdbcEntityDao.findById(resultId);
        Assertions.assertEquals(readResult.getId(), resultId);
        Assertions.assertEquals(testName, readResult.getName());

        // delete
        jdbcEntityDao.deleteById(resultId);
        Assertions.assertThrows(EmptyResultDataAccessException.class, () -> {
            jdbcEntityDao.findById(resultId);
        });
    }

    @Test
    @Transactional
    public void jdbcClearTest() {
        final String testName1 = "test1";
        final String testName2 = "test2";
        // create jdbc, jpa test data
        long resultId1 = jdbcEntityDao.save(testName1);
        long resultId2 = jdbcEntityDao.save(testName2);

        // delete all
        jdbcEntityDao.deleteAll();

        // assert
        Assertions.assertThrows(EmptyResultDataAccessException.class, () -> {
            jdbcEntityDao.findById(resultId1);
        });
        Assertions.assertThrows(EmptyResultDataAccessException.class, () -> {
            jdbcEntityDao.findById(resultId2);
        });
    }

}
