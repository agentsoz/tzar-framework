package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.TzarException;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Factory to create database connections. Uses a connection pool for the connections.
 */
class ConnectionFactory {
  private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());
  private final BoneCP connectionPool;

  public ConnectionFactory(String dbString) throws TzarException {
    // we write out the DB string to the log, but blank out the db password for security reasons.
    LOG.info("Creating connection to DB: " + dbString.replaceAll("password=.*?(&|$)", "password=xxxxxx"));
    try {
      Class.forName("org.postgresql.Driver"); 	// load the DB driver
    } catch (ClassNotFoundException e) {
      throw new TzarException("Couldn't load postgres driver", e);
    }
    BoneCPConfig config = new BoneCPConfig();
    config.setJdbcUrl(dbString);
    config.setDefaultAutoCommit(false);
    config.setMaxConnectionsPerPartition(10);
    config.setStatementsCacheSize(20);
    config.setExternalAuth(true);

    try {
      connectionPool = new BoneCP(config);
    } catch (SQLException e) {
      if (e.getCause() != null) {
        throw new TzarException(e.getCause()); // because something weird happens in one of these libraries, and
        // the top level exception is junk.
      } else {
        throw new TzarException(e);
      }
    }
  }

  /**
   * This method will request a connection from the connection pool. If one is
   * available it will be returned, otherwise the pool will create a new one.
   * If there are no connections available, and already a maximum amount of
   * connections in the pool, this method will wait for one to become available,
   * if a connection doesn't become available after a time, an exception will
   * be thrown.
   * @return a database connection
   * @throws TzarException if the connection couldn't be obtained
   */
  public Connection createConnection() throws TzarException {
    try {
      return connectionPool.getConnection();
    } catch (SQLException e) {
      throw new TzarException(e);
    }
  }
}
