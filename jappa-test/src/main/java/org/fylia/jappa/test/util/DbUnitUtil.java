package org.fylia.jappa.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.dbunit.operation.DatabaseOperation;
import org.hsqldb.DatabaseManager;
import org.hsqldb.persist.LobManager;

/**
 * Utility class for db unit.
 * Can read handle files of different formats.
 * <ul>
 * <li>the .xml files used by DBUnit.</li>
 * <li>.sql files for executing plain sql statements </li>
 * <li>.script files. 
 *  These files hold per line 1 other file to execute. 
 *  This again can be a .xml file, a .sql file or .script file</li>
 *  </ul>
 * 
 * Files starting with / are considered outside the classpath and measured from the root
 * of the file system. For more info see {@link ClassLoader#getResource(String)}.
 * 
 * A line can be commented out using a --, # or // at the beginning of a line
 * 
 * A line starting with 
 * <ul>
 * <li><b>clear</b> (followed by a filename) will only be used when clearing the database (clearDb methods)</li>
 * <li><b>fill</b> (followed by a filename) will only be used when filling the database (fillDb methods)</li>
 * <li>Other lines are always considered to contain only the name of a file</li>
 * </ul>
 * 
 *  The fillDb and clearDb methods where an input stream is given as input assume the
 *  input is a DBUnit .xml file.
 * 
 */
public final class DbUnitUtil {
    private static final String CLEAR_PREFIX = "clear ";
	private static final String FILL_PREFIX = "fill ";
	private static final Log LOGGER = LogFactory.getLog(DbUnitUtil.class);

    /**
     * private constructor to avoid instantiation
     */
    private DbUnitUtil() {
    }
    
    /**
     * Fill the database with the default data file. 
     * DefaultTestData.script
     * DefaultTestData.sql
     * DefaultTestData.xml
     * are searched on the classpath. The first one found
     * is used to fill the database.
     * 
     * @param dataSource
     *            the datasource to write to
     *            
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(DataSource dataSource) throws SQLException, IOException {
        return fillDb(getDefaultDataFile(),dataSource);
    }

    /**
     * Fill the database with the default data file
     * DefaultTestData.script
     * DefaultTestData.sql
     * DefaultTestData.xml
     * are searched on the classpath. The first one found
     * is used to fill the database.
     * 
     * @param dataSource
     *            the datasource to write to
     * @param useSchemas
     *            Use schema prefixes in table names
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(DataSource dataSource,
            boolean useSchemas) throws SQLException, IOException {
        return fillDb(getDefaultDataFile(),dataSource,useSchemas);
    }
    

    /**
     * Fill the database with data from a resource file.
     * 
     * @param fileName
     *            the .script, .sql or .xml resource containing the dataset
     * @param dataSource
     *            the datasource to write to
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(String fileName, DataSource dataSource)
            throws SQLException, IOException {
        return fillDb(fileName, dataSource, false);
    }

    /**
     * Fill the database with data from a resource file.
     * 
     * @param fileName
     *            the .xml, .sql, or .script resource containing the dataset or sql to execute
     * @param dataSource
     *            the datasource to write to
     * @param useSchemas
     *            Use schema prefixes in table names
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(String fileName, DataSource dataSource,
            boolean useSchemas) throws SQLException, IOException {
        URL url = (fileName==null)?null:Thread.currentThread().getContextClassLoader()
                        .getResource(fileName.trim());
        if (url == null) {
            LOGGER.error("File " + (fileName==null?null:fileName.trim()) + " not found.");
            return dataSource;
        }
        if (fileName.trim().endsWith(".sql")) {
            readSql(url, dataSource);
        } else if (fileName.trim().endsWith(".xml")) {
            fillDb(url, dataSource, useSchemas);
        } else if (fileName.trim().endsWith(".script")) {
            readScript(url, dataSource, useSchemas, false);
        } else {
            LOGGER.error("Unknown extension of File " + fileName.trim() + ".");
        }
        return dataSource;
    }

    /**
     * Fill the database with data from an inputStream
     * 
     * @param inputStream
     *            the stream from a DBUnit .xml file
     * @param dataSource
     *            the datasource to write to
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(InputStream inputStream, DataSource dataSource)
            throws SQLException, IOException {
        return fillDb(inputStream, dataSource, false);
    }

    /**
     * Fill the database with data from an inputStream
     * 
     * @param inputStream
     *            the stream from a DBUnit .xml file
     * @param dataSource
     *            the datasource to write to
     * @param useSchemas
     *            Use schema prefixes in table names
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
     */
    public static DataSource fillDb(InputStream inputStream, DataSource dataSource,
            boolean useSchemas) throws SQLException, IOException {
        Connection connection = dataSource.getConnection();
        Statement statement = null;
        try {
            FlatXmlDataSet dataSet = new FlatXmlDataSet(inputStream);
            IDatabaseConnection dbUnitConn = new DatabaseConnection(connection);
            if (useSchemas) {
                dbUnitConn.getConfig().setFeature(
                        "http://www.dbunit.org/features/qualifiedTableNames",
                        true);
            }
            LOGGER.debug("Filling table");
            statement = dbUnitConn.getConnection().createStatement();
            statement
                    .executeUpdate("SET DATABASE REFERENTIAL INTEGRITY FALSE");
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);
            statement
                    .executeUpdate("SET DATABASE REFERENTIAL INTEGRITY TRUE");
            LOGGER.debug("Filled table");
        } catch (DataSetException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } catch (DatabaseUnitException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } finally {
            closeStatement(statement);
            closeConnection(connection);
        }
        return dataSource;
    }
    
    /**
     * This method cleans the all schemas in a hsqldb.
     * This can be used to make sure the entire db is cleared
     * during testing
     * 
     * @param dataSource the datasource
     * @throws SQLException in case of an sql exception
     */
    public static void clearEntireHsqlDb(DataSource dataSource) throws SQLException {
        Connection connection = null;
        try {
          connection = dataSource.getConnection();
              try {
                Statement stmt = connection.createStatement();
                try {
                  ResultSet rs = stmt.executeQuery("select SCHEMA_NAME " +
                          "from INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_OWNER!='_SYSTEM'");
                  List<String> schemas = new ArrayList<String>();
                  while (rs.next()) {
                      schemas.add(rs.getString(1));
                  }
                  rs.close();
                  stmt.executeUpdate("SET DATABASE REFERENTIAL INTEGRITY FALSE");
                  for (String schema : schemas) {
                      LOGGER.info("Clearing schema "+schema);
                      /*
                      rs = stmt.executeQuery("select t.TABLE_NAME " +
                              "from INFORMATION_SCHEMA.TABLES t where t.TABLE_SCHEMA='"+schema+"'");
                      List<String> tables= new ArrayList<String>();
                      while (rs.next()) {
                          tables.add(rs.getString(1));
                      }
                      rs.close();
                      for (String table: tables) {
                          LOGGER.error(" Truncating "+schema+"."+table);
                          stmt.execute("DELETE FROM "+schema+"."+table);
                          stmt.execute("TRUNCATE TABLE "+schema+"."+table+" RESTART IDENTITY AND COMMIT NO CHECK");
                      }*/
                      stmt.execute("TRUNCATE SCHEMA "+schema+ " RESTART IDENTITY AND COMMIT NO CHECK");
                  }
                  stmt.executeUpdate("SET DATABASE REFERENTIAL INTEGRITY TRUE");
                  rs = stmt.executeQuery("select LOB_ID from SYSTEM_LOBS.LOB_IDS ");
                  final LobManager lobManager = DatabaseManager.getDatabase(0).lobManager;
                  LOGGER.error("LOB COUNT "+lobManager.getLobCount());
                  while (rs.next()) {
                      for (int col = 1; col<=rs.getMetaData().getColumnCount(); col++) {
                          LOGGER.error("lob "+rs.getMetaData().getColumnName(col)+" " +rs.getLong(col));
                          
                      }
                      lobManager.deleteLob(rs.getLong(1));
                  }

                  LOGGER.error("LOB COUNT2 "+lobManager.getLobCount());
                  connection.commit();
                } finally {
                  stmt.close();
                }
              } catch (SQLException e) {
                  LOGGER.error("Problem "+e);
                  connection.rollback();
              }
          } finally {
              if (connection != null) {
                  connection.close();
              }
          }
        clearHsqldbLobs(dataSource);
    }

    /**
     * This method cleans the all schemas in a hsqldb.
     * This can be used to make sure the entire db is cleared
     * during testing
     * 
     * @param dataSource the datasource
     * @throws SQLException in case of an sql exception
     */
    public static void clearEntireH2Db(DataSource dataSource) throws SQLException {
        Connection connection = null;
        try {
          connection = dataSource.getConnection();
              try {
                Statement stmt = connection.createStatement();
                try {
                  ResultSet rs = stmt.executeQuery("select SCHEMA_NAME " +
                          "from INFORMATION_SCHEMA.SCHEMATA WHERE ID >=0");
                  List<String> schemas = new ArrayList<String>();
                  while (rs.next()) {
                      schemas.add(rs.getString(1));
                  }
                  rs.close();
                  stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                  for (String schema : schemas) {
                      LOGGER.error("Clearing schema "+schema);
                      rs = stmt.executeQuery("select t.TABLE_NAME " +
                              "from INFORMATION_SCHEMA.TABLES t where t.TABLE_SCHEMA='"+schema+"'");
                      List<String> tables= new ArrayList<String>();
                      while (rs.next()) {
                          tables.add(rs.getString(1));
                      }
                      rs.close();
                      
                      for (String table: tables) {
                          LOGGER.error(" Truncating "+schema+"."+table);
                          stmt.execute("TRUNCATE TABLE "+schema+"."+table);
                      }
                      
                      rs = stmt.executeQuery("select t.SEQUENCE_NAME " +
                              "from INFORMATION_SCHEMA.SEQUENCES t where t.SEQUENCE_SCHEMA='"+schema+"'");
                      List<String> sequences= new ArrayList<String>();
                      while (rs.next()) {
                          sequences.add(rs.getString(1));
                      }
                      rs.close();
                      for (String sequence: sequences) {
                          LOGGER.error(" Resetting sequence "+schema+"."+sequence);
                          stmt.execute("ALTER SEQUENCE "+schema+"."+sequence+" RESTART WITH 1");
                      }
                  }
                  stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                  connection.commit();
                } finally {
                  stmt.close();
                }
              } catch (SQLException e) {
                  LOGGER.error("Problem "+e, e);
                  connection.rollback();
              }
          } finally {
              if (connection != null) {
                  connection.close();
              }
              LOGGER.error("Cleared entire hsqldb");
          }
    }
    
    /**
     * It seems that hsqldb doesn't clear the lobs from memory.
     * This method explicitely asks hsqldb to delete all lobs
     * @param dataSource the datasource
     * @throws SQLException in case of an sql exception
     */
    public static final void clearHsqldbLobs(DataSource dataSource)
            throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try {
                Statement stmt = connection.createStatement();
                try {
                    ResultSet rs = stmt
                            .executeQuery("select LOB_ID from SYSTEM_LOBS.LOB_IDS ");
                    final LobManager lobManager = DatabaseManager
                            .getDatabase(0).lobManager;
                    while (rs.next()) {
                        lobManager.deleteLob(rs.getLong(1));
                    }
                    connection.commit();
                } finally {
                    stmt.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Problem " + e, e);
                connection.rollback();
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
            LOGGER.error("Cleared entire hsqldb");
        }

    }

    /**
     * Clears the database with the default data file. 
     * DefaultTestData.script
     * DefaultTestData.sql
     * DefaultTestData.xml
     * are searched on the classpath. The first one found
     * is used to clear the database.
     * 
     * @param dataSource
     *            the datasource to write to
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(DataSource dataSource) throws SQLException, IOException {
        clearDb(getDefaultDataFile(),dataSource);
    }
    
    /**
     * Clears the database with the default data file
     * DefaultTestData.script
     * DefaultTestData.sql
     * DefaultTestData.xml
     * are searched on the classpath. The first one found
     * is used to clear the database.
     * 
     * @param dataSource
     *            the datasource to write to
     * @param useSchemas
     *            Use schema prefixes in table names
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(DataSource dataSource,
            boolean useSchemas) throws SQLException, IOException {
        clearDb(getDefaultDataFile(),dataSource,useSchemas);
    }
    
    

    /**
     * Removes all data from the database, based on the DBUnit table
     * descriptions in the given input .xml file, or using the files
     * in the .script file or .sql files.
     * 
     * @param fileName
     *            the data file used for defining tables.
     * @param dataSource
     *            the datasource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(String fileName, DataSource dataSource)
            throws SQLException, IOException {
    	clearDb(fileName,dataSource, false);
    }

   /**
     * Removes all data from the database, based on the DBUnit table
     * descriptions in the given input .xml file, or using the files
     * in the .script file or .sql files.
     * 
     * @param fileName
     *            the data file used for defining tables.
     * @param dataSource
     *            the datasource
     * @param useSchemas
     *            Use schema prefixes in table names
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(String fileName, DataSource dataSource, boolean useSchemas)
            throws SQLException, IOException {
    	
        URL url = (fileName==null)?null:Thread.currentThread().getContextClassLoader()
                .getResource(fileName.trim());
		if (url == null) {
		    LOGGER.error("File " + (fileName==null?null:fileName.trim()) + " not found.");
		    return;
		}
		if (fileName.trim().endsWith(".sql")) {
		    readSql(url, dataSource);
		} else if (fileName.trim().endsWith(".xml")) {
		    clearDb(url, dataSource, useSchemas);
		} else if (fileName.trim().endsWith(".script")) {
		    readScript(url, dataSource, useSchemas, true);
		} else {
		    LOGGER.error("Unknown extension of File " + fileName.trim() + ".");
		}    	
    }
    
    /**
     * Removes all data from the database, based on the DBUnit table
     * descriptions in the given input .xml file.
     * 
     * 
     * @param file
     *            the sql file to execute
     * @param dataSource
     *            the data source to execute the file on
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(URL file, DataSource dataSource) throws SQLException, IOException {
    	clearDb(file,dataSource,false);
    }

   /**
     * Reads the DBUnit xml datasource from file.
     * 
     * @param file
     *            the sql file to execute
     * @param dataSource
     *            the data source to execute the file on
     * @param useSchemas
     *            Use schema prefixes in table names
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(URL file, DataSource dataSource, boolean useSchemas) throws SQLException, IOException {
        // Now read line bye line
        InputStream inputStream = null;
        try {
            LOGGER.info("Reading " + file);
            inputStream = file.openStream();
            clearDb(inputStream, dataSource,useSchemas);
        } finally {
            closeStream(inputStream);
        }
    }    

    /**
     * Removes all data from the database, based on the DBUnit table
     * descriptions in the given input .xml file.
     * 
     * @param inputStream
     *            the stream from a DBUnit .xml file
     * @param dataSource
     *            the datasource to write to
     * @param useSchemas
     *            Use schema prefixes in table names
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void clearDb(InputStream inputStream, DataSource dataSource, boolean useSchemas)
            throws SQLException, IOException {
        Connection connection = dataSource.getConnection();
        Statement statement = null;
        try {
            FlatXmlDataSet dataSet = new FlatXmlDataSet(inputStream);
            IDatabaseConnection dbUnitConn = new DatabaseConnection(connection);
            LOGGER.debug("Emptying tables on "
                    + connection.getMetaData().getURL());
            if (useSchemas) {
                dbUnitConn.getConfig().setFeature(
                        "http://www.dbunit.org/features/qualifiedTableNames",
                        true);
            }
            
            statement = dbUnitConn.getConnection().createStatement();
            statement
                    .executeUpdate("SET DATABASE REFERENTIAL INTEGRITY FALSE");
            DatabaseOperation.DELETE_ALL.execute(dbUnitConn, dataSet);
            statement
                    .executeUpdate("SET DATABASE REFERENTIAL INTEGRITY TRUE");
            LOGGER.debug("Emptied table  on "
                    + connection.getMetaData().getURL());
        } catch (DataSetException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } catch (DatabaseUnitException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } finally {
            closeStatement(statement);
            closeConnection(connection);
        }
    }

    /**
     * Creates a DTD for a given data resource
     * 
     * @param dataSource
     *            the data source
     * @return the dtd
     * @throws SQLException in case of an sql exception
      */
    public static String createDtd(DataSource dataSource, boolean useSchemas) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            IDatabaseConnection dbUnitConn = new DatabaseConnection(connection);
            if (useSchemas) {
                dbUnitConn.getConfig().setFeature(
                        "http://www.dbunit.org/features/qualifiedTableNames",
                        true);
            }
            // write DTD file
            StringWriter stringWriter = new StringWriter();
            FlatDtdDataSet.write(dbUnitConn.createDataSet(), stringWriter);
            return stringWriter.toString();
        } catch (DataSetException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } catch (IOException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } finally {
            connection.close();
        }
    }

    /**
     * Exports the data in the DB to a given file
     * 
     * @param dataSource
     *            the datasource to read
     * @param file
     *            the file to write.
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
     */
    public static void exportDb(DataSource dataSource, File file)
            throws SQLException, IOException {
        exportDb(dataSource, new FileOutputStream(file));
    }

    /**
     * Exports the data in the DB to a given file
     * 
     * @param dataSource
     *            the datasource to read
     * @param stream
     *            the stream to write to.
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
     */
    public static void exportDb(DataSource dataSource, OutputStream stream)
            throws SQLException, IOException {
        Connection connection = dataSource.getConnection();
        try {
            IDatabaseConnection dbUnitConn = new DatabaseConnection(connection);
            // write DTD file
            FlatXmlWriter datasetWriter = new FlatXmlWriter(stream);
            datasetWriter.write(dbUnitConn.createDataSet());
        } catch (DataSetException e) {
            SQLException exception = new SQLException();
            exception.initCause(e);
            throw exception;
        } finally {
            connection.close();
        }
    }

    /**
     * Get the location of the DBUnit data file with test data.
     * 
     * The default location is a classpath location and is determined by using
     * the name of a class. When the <code>testClass</code> is given at
     * construction time, this class will be used to determine the name of the
     * XML file, otherwise the class name of this test case will be used.
     * 
     * @param clazz
     *            the test clazz to get a data file for
     * @return the file to use (from the classpath)
     */
    public static String getDataFile(Class<?> clazz) {
        String fileName = getDataFile(clazz.getName().replaceAll("\\.", "/"));
        if (fileName==null) {
            fileName=getDefaultDataFile();
        }
        return fileName;
    }

    /**
     * Retrieves the DefaultTestData file on the classpath. Can be a .script .sql or .xml file
     * @return the file name to use.
     */
    public static String getDefaultDataFile() {
        String fileName= getDataFile("DefaultTestData");
        if (fileName == null) {
            LOGGER.error("No Test Data file found .");
        }
        return fileName;
    }
    
    /**
     * Checks if a file with given baseName and extensions .script, .sql or .sql exist.
     * @param baseName the base file name (path from classpath + fileName, no extension) 
     * @return the name of a file that exists (path+fileName+extension) or null if no
     *   matching file is found
     */
    private static String getDataFile(String baseName) {
        if (existsDataFile(baseName+".script")) 
            return baseName+".script";
        if (existsDataFile(baseName+".sql")) 
            return baseName+".sql";
        if (existsDataFile(baseName+".xml")) 
            return baseName+".xml";
        return null;
    }
    /**
     * Checks if a file with given name exists on the classpath
     * @param fileName the file name to check
     * @return if it exists
     */
    private static boolean existsDataFile(String fileName) {
        if (null==Thread.currentThread().getContextClassLoader().getResource(fileName)) {
            LOGGER.debug("Cannot read file " + fileName + "." );
            return false;
        }
        LOGGER.info("Test Data file : " + fileName);
        return true;
    }
    
    
    /**
     * Retrieves a dataSource by jndiDataSource name
     * 
     * @param dataSourceName
     *            the jndiDataSource name
     * @return the dataSource
     * @throws NamingException
     *             if the datasource is not found
     */
    public static DataSource getJndiDataSource(String dataSourceName)
            throws NamingException {
        return (DataSource) new InitialContext().lookup(dataSourceName);
    }

    /**
     * Reads and executes an sql file.
     * 
     * @param file
     *            the sql file to execute
     * @param dataSource
     *            the data source to execute the file on
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
     */
    public static void readSql(URL file, DataSource dataSource)
            throws SQLException, IOException {
        // Now read line bye line
        Connection connection = null;
        Reader reader = null;
        LOGGER.info("Executing " + file);
        try {
            connection = dataSource.getConnection();
            reader = new BufferedReader(new InputStreamReader(file.openStream(), "UTF-8"));
            // Split per statement
            String[] statements = SqlUtil.getStatements(reader);
            int lineNr = 0;
            for (String statement:statements) {
                lineNr++;
                // Skip empty lines
                statement = statement.trim();
                if (StringUtils.isEmpty(statement)) {
                    continue;
                }
                Statement cs=null;
                try {
                    if (statement.toLowerCase().startsWith("call ")) {
                        LOGGER.debug("Calling SP : "+statement.replaceAll("\\s\\s*"," "));
                        cs = connection.prepareCall("{"+statement+"}"); 
                        ((CallableStatement)cs).executeQuery(); 
                    } else {
                        LOGGER.debug("Executing statement : "+statement.replaceAll("\\s\\s*"," "));
                        cs = connection.createStatement();
                        cs.execute(statement);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error executing statement number "
                            + lineNr + "("+file+"):" + statement);
                    LOGGER.error(ex.getClass().getName()+":"+ex.getMessage());
                    if (ex.getCause()!=null) {
                        LOGGER.error(" caused by "+ex.getCause().getClass().getName()+":"+ex.getCause().getMessage());
                        
                    }
                } finally {
                    closeStatement(cs);
                }
            }
        } finally {
            closeConnection(connection);
            closeReader(reader);
        }
    }

    /**
     * Reads the DBUnit xml datasource from file.
     * 
     * @param file
     *            the sql file to execute
     * @param dataSource
     *            the data source to execute the file on
     * @return the given dataSource
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static DataSource fillDb(URL file, DataSource dataSource,
            boolean useSchemas) throws SQLException, IOException {
        // Now read line bye line
        InputStream inputStream = null;
        try {
            LOGGER.info("Reading " + file);
            inputStream = file.openStream();
            return fillDb(inputStream, dataSource, useSchemas);
        } finally {
            closeStream(inputStream);
        }
    }

    /**
     * Reads a .script file.
     * This file holds per line another file to execute.
     * The file types allowed are 
     * <ul>
     * <li>other .script files,
     * <li>.sql files
     * <li>.xml files
     * </ul>
     * the base path is the root of the classpath.
     * 
     * Files starting with / are considered outside the classpath and measured from the root
     * of the file system. For more info see {@link ClassLoader#getResource(String)}.
     * 
     * A line can be commented out using a --, # or // at the beginning of a line
     * 
     * A line starting with 
     * <ul>
     * <li><b>clear</b> (followed by a filename) will only be used when clearing the database</li>
     * <li><b>fill</b> (followed by a filename) will only be used when filling the database</li>
     * <li>Other lines are always considered to contain only the name of a file</li>
     * </ul>
     * 
     * @param file
     *            the sql file to execute
     * @param dataSource
     *            the data source to execute the file on
     * @throws SQLException in case of an sql exception
     * @throws IOException in case of problems reading the file
      */
    public static void readScript(URL file, DataSource dataSource,
            boolean useSchemas, boolean clear) throws SQLException, IOException {
        // Now read line bye line
        String thisLine;
        BufferedReader reader = null;
        LOGGER.info("Reading script file : " + file);
        int lineNr = 0;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(file.openStream()));
            while ((thisLine = reader.readLine()) != null) {
                lineNr++;
                thisLine = thisLine.trim();
                // Skip comments and empty lines
                if (StringUtils.isBlank(thisLine) 
                        || thisLine.startsWith("--")
                        || thisLine.startsWith("//")
                        || thisLine.startsWith("#")) {
                    continue;
                }
                if (!clear) {
                	if (thisLine.startsWith(CLEAR_PREFIX)) {
                		continue;
                	} else if (thisLine.startsWith(FILL_PREFIX)) {
                		thisLine = thisLine.substring(FILL_PREFIX.length());
                	}
                	fillDb(thisLine, dataSource, useSchemas);
                } else {
                	if (thisLine.startsWith(FILL_PREFIX)) {
                		continue;
                	} else if (thisLine.startsWith(CLEAR_PREFIX)) {
                		thisLine = thisLine.substring(CLEAR_PREFIX.length());
                	}
                	clearDb(thisLine, dataSource, useSchemas);
                }
            }
        } finally {
            closeReader(reader);
            LOGGER.info("Read "+lineNr + " lines from "+ file);
        }
    }
    
    /**
     * Closes an input stream and logs (but ignores) further exceptions
     * @param inputStream the input stream to close
     */
    public static void closeStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.error("Error closing inputStream " + inputStream);
            }
        }
    }

    /**
     * Closes a readerand logs (but ignores) further exceptions
     * @param reader the reader to close
     */
    public static void closeReader(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("Error closing reader " + reader);
            }
        }
    }
    
    /**
     * Closes a db statement and logs (but ignores) further exceptions
     * @param statement the Statement to close
     */
    public static void closeStatement(Statement statement) {
        if (statement != null) {
            try { 
                statement.close(); 
            } catch (SQLException e) {
                LOGGER.error("Failed closing the statement. Ignoring exception ",e);
            }
        }
    }
    
    /**
     * Closes a connection and logs (but ignores) further exceptions
     * @param connection the connection to close
     */
    public static void closeConnection (Connection connection) {
        if (connection != null) {
            try { 
                connection.close(); 
            } catch (SQLException e) {
                LOGGER.error("Failed closing the connection. Ignoring exception ",e);
            }
        }
    }

}
