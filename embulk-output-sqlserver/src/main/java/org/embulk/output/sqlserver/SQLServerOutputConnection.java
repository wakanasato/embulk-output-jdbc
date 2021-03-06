package org.embulk.output.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.embulk.output.jdbc.JdbcColumn;
import org.embulk.output.jdbc.JdbcOutputConnection;
import org.embulk.output.jdbc.JdbcSchema;

public class SQLServerOutputConnection
        extends JdbcOutputConnection
{
    public SQLServerOutputConnection(Connection connection, String schemaName, boolean autoCommit)
            throws SQLException
    {
        super(connection, schemaName);
        connection.setAutoCommit(autoCommit);
    }

    @Override
    protected String buildRenameTableSql(String fromTable, String toTable)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("EXEC sp_rename ");
        sb.append(quoteIdentifierString(fromTable));
        sb.append(", ");
        sb.append(quoteIdentifierString(toTable));
        sb.append(", 'OBJECT'");
        return sb.toString();
    }

    @Override
    protected String buildColumnTypeName(JdbcColumn c)
    {
        switch(c.getSimpleTypeName()) {
        case "BOOLEAN":
            return "BIT";
        case "CLOB":
            return "TEXT";
        case "TIMESTAMP":
            return "DATETIME2";
        default:
            return super.buildColumnTypeName(c);
        }
    }

    @Override
    protected void setSearchPath(String schema) throws SQLException
    {
        // NOP
    }

    @Override
    public void dropTableIfExists(String tableName) throws SQLException
    {
        if (tableExists(tableName)) {
            dropTable(tableName);
        }
    }

    @Override
    protected void dropTableIfExists(Statement stmt, String tableName) throws SQLException
    {
        if (tableExists(tableName)) {
            dropTable(stmt, tableName);
        }
    }

    @Override
    public void createTableIfNotExists(String tableName, JdbcSchema schema) throws SQLException
    {
        if (!tableExists(tableName)) {
            createTable(tableName, schema);
        }
    }

    public void createTable(String tableName, JdbcSchema schema) throws SQLException
    {
        Statement stmt = connection.createStatement();
        try {
            String sql = buildCreateTableSql(tableName, schema);
            executeUpdate(stmt, sql);
            commitIfNecessary(connection);
        } catch (SQLException ex) {
            throw safeRollback(connection, ex);
        } finally {
            stmt.close();
        }
    }

    protected String buildCreateTableSql(String name, JdbcSchema schema)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE ");
        quoteIdentifierString(sb, name);
        sb.append(buildCreateTableSchemaSql(schema));
        return sb.toString();
    }

    private static final String[] SIMPLE_TYPE_NAMES = {
        "BIT", "FLOAT",
    };

    @Override
    protected ColumnDeclareType getColumnDeclareType(String convertedTypeName, JdbcColumn col)
    {
        if (Arrays.asList(SIMPLE_TYPE_NAMES).contains(convertedTypeName)) {
            return ColumnDeclareType.SIMPLE;
        }
        return super.getColumnDeclareType(convertedTypeName, col);
    }

    /*
    @Override
    public Charset getTableNameCharset() throws SQLException
    {
        return getOracleCharset().getJavaCharset();
    }

    */
}
