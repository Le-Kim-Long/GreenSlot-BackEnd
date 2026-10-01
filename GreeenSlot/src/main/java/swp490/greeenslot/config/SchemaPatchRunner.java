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
        String[] tablesWithEnums = {
                "gardening_tasks",
                "pillars",
                "slot_rentals",
                "tree_planting_requests",
                "equipment",
                "service_requests",
                "contracts",
                "transactions"
        };
        for (String table : tablesWithEnums) {
            dropAllCheckConstraints(table);
        }
        patchNationalizedColumns();
        patchQuantityColumns();
    }

    /**
     * Ensures quantity column exists in equipment and trees tables across PostgreSQL and SQL Server.
     */
    private void patchQuantityColumns() {
        String sqlPostgres =
                "DO $$ BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='equipment' AND column_name='quantity') THEN " +
                "    ALTER TABLE equipment ADD COLUMN quantity INT DEFAULT 1; " +
                "  END IF; " +
                "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='trees' AND column_name='quantity') THEN " +
                "    ALTER TABLE trees ADD COLUMN quantity INT DEFAULT 100; " +
                "  END IF; " +
                "  UPDATE equipment SET quantity = 1 WHERE quantity IS NULL; " +
                "  UPDATE trees SET quantity = 100 WHERE quantity IS NULL; " +
                "END $$;";

        String sqlSqlServer =
                "BEGIN TRY\n" +
                "    IF COL_LENGTH('dbo.equipment', 'quantity') IS NULL\n" +
                "    BEGIN\n" +
                "        ALTER TABLE dbo.equipment ADD quantity INT NOT NULL DEFAULT 1;\n" +
                "    END\n" +
                "    IF COL_LENGTH('dbo.trees', 'quantity') IS NULL\n" +
                "    BEGIN\n" +
                "        ALTER TABLE dbo.trees ADD quantity INT NOT NULL DEFAULT 100;\n" +
                "    END\n" +
                "    UPDATE dbo.equipment SET quantity = 1 WHERE quantity IS NULL;\n" +
                "    UPDATE dbo.trees SET quantity = 100 WHERE quantity IS NULL;\n" +
                "END TRY\n" +
                "BEGIN CATCH\n" +
                "END CATCH;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbProductName.contains("postgres")) {
                stmt.execute(sqlPostgres);
                logger.info("Schema patch checked: quantity columns ensured on PostgreSQL (equipment & trees).");
            } else {
                stmt.execute(sqlSqlServer);
                logger.info("Schema patch checked: quantity columns ensured on SQL Server (equipment & trees).");
            }
        } catch (Exception e) {
            logger.warn("Schema patch skipped for quantity columns: {}", e.getMessage());
        }
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
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbProductName.contains("postgres")) {
                // PostgreSQL varchar/text natively supports full Unicode (UTF-8), no NVARCHAR patch needed
                return;
            }
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
        String cleanTableName = qualifiedTable.replace("dbo.", "").trim();

        String sqlPostgres =
                "DO $$ \n" +
                "DECLARE \n" +
                "    r RECORD;\n" +
                "BEGIN \n" +
                "    FOR r IN (\n" +
                "        SELECT con.conname, rel.relname \n" +
                "        FROM pg_constraint con \n" +
                "        INNER JOIN pg_class rel ON rel.oid = con.conrelid \n" +
                "        WHERE con.contype = 'c' \n" +
                "          AND rel.relname = '" + cleanTableName + "'\n" +
                "    ) LOOP \n" +
                "        EXECUTE 'ALTER TABLE ' || quote_ident(r.relname) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname); \n" +
                "    END LOOP; \n" +
                "END $$;";

        String sqlSqlServer =
                "DECLARE @constraintName NVARCHAR(200);\n" +
                "DECLARE constraint_cursor CURSOR FOR\n" +
                "    SELECT cc.name FROM sys.check_constraints cc\n" +
                "    WHERE cc.parent_object_id = OBJECT_ID('dbo." + cleanTableName + "');\n" +
                "OPEN constraint_cursor;\n" +
                "FETCH NEXT FROM constraint_cursor INTO @constraintName;\n" +
                "WHILE @@FETCH_STATUS = 0\n" +
                "BEGIN\n" +
                "    EXEC('ALTER TABLE dbo." + cleanTableName + " DROP CONSTRAINT [' + @constraintName + ']');\n" +
                "    FETCH NEXT FROM constraint_cursor INTO @constraintName;\n" +
                "END\n" +
                "CLOSE constraint_cursor;\n" +
                "DEALLOCATE constraint_cursor;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbProductName.contains("postgres")) {
                stmt.execute(sqlPostgres);
                logger.info("Schema patch checked (PostgreSQL): stale CHECK constraints on {} (if any) removed.", cleanTableName);
            } else {
                stmt.execute(sqlSqlServer);
                logger.info("Schema patch checked (SQL Server): stale CHECK constraints on {} (if any) removed.", cleanTableName);
            }
        } catch (Exception e) {
            logger.warn("Schema patch skipped for {}: {}", cleanTableName, e.getMessage());
        }
    }
}
