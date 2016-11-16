package org.fylia.jappa.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Abstract Dao for Spring Jdbc
 * @author fylia
 */
@Transactional
public abstract class AbstractSpringJdbcDao {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Initialize the dataSource
     * @param dataSource the datasource
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
    
    protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

    /**
     * Simple adapter for PreparedStatementCreator, allowing to use a plain SQL statement.
     */
    public static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
        /**
         * Interface for ParameterSetter. Allows to set parameters in the prepared statement
         */
        public interface ParameterSetter {
            /**
             * Set the parameters on the prepared statement
             * @param ps the prepared statement
             * @throws SQLException in case of trouble
             */
            void setParameters(PreparedStatement ps) throws SQLException;
        }
        private final String sql;
        private final ParameterSetter ps;
        /**
         * Constructor
         * @param sql the sql statement
         * @param ps the (optional) parameterSetter
         */
        public SimplePreparedStatementCreator(String sql, ParameterSetter ps) {
            Assert.notNull(sql, "SQL must not be null");
            this.sql = sql;
            this.ps=ps;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            final PreparedStatement prepareStatement = con.prepareStatement(this.sql);
            if (ps!=null) {
                ps.setParameters(prepareStatement);
            }
            return prepareStatement;

        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }

    /**
     * Generic Row mapper for object[]
     */
    public static class ObjectArrayRowMapper implements RowMapper<Object[]> {
        @Override
        public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Object[] row = new Object[rs.getMetaData().getColumnCount()];
            for (int col=1; col<=rs.getMetaData().getColumnCount(); col++) {
                row[col-1]=rs.getObject(col);
            }
            return row;
        }
    }

}
