package com.xin.musictag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the same batch HTTP flows against the injected MySQL test database, without cleaning it. */
@SpringBootTest(properties={
        "spring.datasource.url=${MYSQL_IT_URL}", "spring.datasource.username=${MYSQL_IT_USER}",
        "spring.datasource.password=${MYSQL_IT_PASSWORD}", "spring.flyway.enabled=true",
        "music.storage.root=.test-data/mysql-phase02-${random.uuid}",
        "logging.level.org.flywaydb=OFF","logging.level.com.zaxxer.hikari=OFF"
})
@EnabledIfEnvironmentVariable(named="MYSQL_IT_URL",matches=".+")
class MySqlPhase02IntegrationTest extends BatchImportIntegrationTest {
    @Test void existingSchemaMigratedToV4AndHistoryRemainsValid() {
        assertEquals(4,jdbc.queryForObject("select count(*) from flyway_schema_history where success=1 and version in ('1','2','3','4')",Integer.class));
        assertEquals("4",jdbc.queryForObject("select version from flyway_schema_history where success=1 and version='4'",String.class));
    }
}
