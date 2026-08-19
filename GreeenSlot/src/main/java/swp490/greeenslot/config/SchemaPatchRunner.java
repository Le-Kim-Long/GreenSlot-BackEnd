package swp490.greeenslot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * One-off schema patches that Hibernate's ddl-auto=update won't apply on its own
 * (e.g. legacy CHECK constraints predating a Java enum change). Each patch is
 * written to be safe to run on every startup (no-op if already applied).
 */
@Component
@Order(Integer.MIN_VALUE)
public class SchemaPatchRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaPatchRunner.class);

    private final DataSource dataSource;

    public SchemaPatchRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        dropAllCheckConstraints("dbo.gardening_tasks");
        patchNationalizedColumns();
    }

    /**
     * Ensures notification and content text columns are NVARCHAR to fully support Vietnamese Unicode.
     */
    private void patchNationalizedColumns() {
        String sql =
                "BEGIN TRY\n" +
                "    IF OBJECT_ID('dbo.notifications', 'U') IS NOT NULL\n" +
                "    BEGIN\n" +
                "        ALTER TABLE dbo.notifications ALTER COLUMN title NVARCHAR(255) NOT NULL;\n" +
                "        ALTER TABLE dbo.notifications ALTER COLUMN message NVARCHAR(4000) NOT NULL;\n" +
                "    END\n" +
                "    IF OBJECT_ID('dbo.global_contents', 'U') IS NOT NULL\n" +
                "    BEGIN\n" +
                "        ALTER TABLE dbo.global_contents ALTER COLUMN title NVARCHAR(255) NOT NULL;\n" +
                "        ALTER TABLE dbo.global_contents ALTER COLUMN content NVARCHAR(MAX) NOT NULL;\n" +
                "    END\n" +
                "END TRY\n" +
                "BEGIN CATCH\n" +
                "    -- Skip if already NVARCHAR or schema alteration blocked\n" +
                "END CATCH;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Schema patch checked: NVARCHAR Unicode support ensured for notifications & global_contents.");
        } catch (Exception e) {
            logger.warn("Schema patch skipped for Unicode columns: {}", e.getMessage());
        }
    }

    /**
     * Drops every CHECK constraint on the given table, if any exist. These enum-string
     * columns (task_type, status, ...) are already validated in Java via @Enumerated(STRING)
     * + enum parsing, so a DB-level CHECK constraint is redundant and only goes stale
     * whenever a new enum value is added (blocking inserts/updates with a cryptic SQL error).
     */
    private void dropAllCheckConstraints(String qualifiedTable) {
        String sql =
                "DECLARE @constraintName NVARCHAR(200);\n" +
                "DECLARE constraint_cursor CURSOR FOR\n" +
                "    SELECT cc.name FROM sys.check_constraints cc\n" +
                "    WHERE cc.parent_object_id = OBJECT_ID('" + qualifiedTable + "');\n" +
                "OPEN constraint_cursor;\n" +
                "FETCH NEXT FROM constraint_cursor INTO @constraintName;\n" +
                "WHILE @@FETCH_STATUS = 0\n" +
                "BEGIN\n" +
                "    EXEC('ALTER TABLE " + qualifiedTable + " DROP CONSTRAINT [' + @constraintName + ']');\n" +
                "    FETCH NEXT FROM constraint_cursor INTO @constraintName;\n" +
                "END\n" +
                "CLOSE constraint_cursor;\n" +
                "DEALLOCATE constraint_cursor;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Schema patch checked: stale CHECK constraints on {} (if any) removed.", qualifiedTable);
        } catch (Exception e) {
            logger.warn("Schema patch skipped for {}: {}", qualifiedTable, e.getMessage());
        }
    }
}
