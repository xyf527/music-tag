package com.xin.musictag;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BatchMigrationUpgradeTest {
    @Test void v4UpgradesExistingV3WithoutChangingHistoricalRecords() throws Exception {
        String url = "jdbc:h2:mem:upgrade_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("3").load().migrate();
        try (var connection = DriverManager.getConnection(url,"sa",""); var statement = connection.createStatement()) {
            statement.executeUpdate("insert into batch_task(status,created_at,updated_at) values ('DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            statement.executeUpdate("insert into batch_item(batch_task_id,relative_path,kind,status,stage,created_at) values (1,'historical.txt','UNSUPPORTED','UNSUPPORTED','IMPORTED',CURRENT_TIMESTAMP)");
        }
        assertEquals(1,Flyway.configure().dataSource(url,"sa","").load().migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url,"sa",""); var statement = connection.createStatement(); var rows = statement.executeQuery("select relative_path,status,lyrics_item_id from batch_item where id=1")) {
            assertTrue(rows.next()); assertEquals("historical.txt",rows.getString(1)); assertEquals("UNSUPPORTED",rows.getString(2)); assertNull(rows.getObject(3));
        }
        assertTrue(Flyway.configure().dataSource(url,"sa","").load().validateWithResult().validationSuccessful);
    }
}
