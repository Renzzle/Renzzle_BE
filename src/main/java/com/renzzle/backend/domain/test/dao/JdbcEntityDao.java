package com.renzzle.backend.domain.test.dao;

import com.renzzle.backend.domain.test.domain.JdbcEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class JdbcEntityDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public long save(String name) {
        String sql = "INSERT INTO jdbc_entity (name) VALUES (:name)";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("name", name);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, param, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public JdbcEntity findById(Long id) {
        String sql = "SELECT * FROM jdbc_entity WHERE id=:jdbc_entity_id";
        Map<String, Object> param = Map.of("jdbc_entity_id", id);
        return jdbcTemplate.queryForObject(sql, param, (rs, rowNum) -> new JdbcEntity(
                Long.parseLong(rs.getString("id")),
                rs.getString("name")
        ));
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM jdbc_entity WHERE id=:jdbc_entity_id";
        Map<String, Object> param = Map.of("jdbc_entity_id", id);
        jdbcTemplate.update(sql, param);
    }

    public void deleteAll() {
        String sql = "DELETE FROM jdbc_entity";
        Map<String, Object> param = new HashMap<>();
        jdbcTemplate.update(sql, param);
    }

}
