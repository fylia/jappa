package org.fylia.jappa.test.dao;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.fylia.jappa.test.util.DbUnitUtil;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Abstract base class for dao tests
 * @author fylia
 *
 */
@ContextConfiguration(locations = {"/core-spring.xml"})
public abstract class AbstractDaoTest  extends AbstractJUnit4SpringContextTests {
    @Autowired
    private DataSource dataSource;


    /**
     * Set up a test db
     * @throws IOException in case of trouble
     * @throws SQLException in case of trouble
     */
    @Before
    public void setup() throws IOException, SQLException {
        DbUnitUtil.clearDb(dataSource);
        DbUnitUtil.fillDb(dataSource);
    }
}
