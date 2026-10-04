package com.xin.musictag;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@EnabledIfEnvironmentVariable(named="MYSQL_IT_URL",matches=".+")
@SpringBootTest(properties={
        "spring.datasource.url=${MYSQL_IT_URL}","spring.datasource.username=${MYSQL_IT_USER}","spring.datasource.password=${MYSQL_IT_PASSWORD}",
        "spring.flyway.enabled=true","music.storage.root=.test-data/mysql-phase04-${random.uuid}"
})
@ActiveProfiles(value="mysql",inheritProfiles=false)
class MySqlPhase04IntegrationTest extends Phase04WorkflowIntegrationTest { }
