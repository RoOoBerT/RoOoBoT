package fr.rooobert.energy.rooobot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/** Database connection wrapper */
public class Database {
	// --- Constants
	private static final Logger logger = LogManager.getLogger(Database.class);
	
	// --- Attributes
	private static Database instance;
	
	private Connection connection;
	
	// --- Methods
	public static synchronized void initialize(Properties props) throws Exception {
		if (instance == null) {
			String driver = props.getProperty("db.driver", "org.sqlite.JDBC");
			String url = props.getProperty("db.url", "jdbc:sqlite:rooobot.sqlite");
			
			instance = new Database(driver, url);
		} else {
			throw new Exception("Database already initialized !");
		}
	}
	
	private Database(String driver, String url) throws Exception {
		// Load the sqlite-JDBC driver using the current class loader
		logger.info("Loading JDBC driver : " + driver);
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			throw new Exception("Error loading JDBC driver : " + e.getMessage(), e);
		}

		// Create a database connection
		logger.info("Connecting to database at " + url);
		try {
			this.connection = DriverManager.getConnection(url);
			
			logger.info("Connected !");
		} catch (SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			this.connection = null;
			throw new Exception("Error opening connection" + e.getMessage(), e);
		}
		
		// Initialize tables
		/*try {
			//
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(5);  // set timeout to 30 sec.

			// === TABLE USER ===
			// = Structure =
//			statement.executeUpdate("drop table if exists user");
			statement.executeUpdate("create table if not exists user (id integer, name string, score integer)");
			
			// = Contents =
			ResultSet rs = statement.executeQuery("select * from user");
			while(rs.next()) {
				User user = new User(rs.getString("name"));
				user.addScore(rs.getInt("score"));
				
				this.users.put(user.getName(), user);
			}
			
			// === TABLE QUESTIONS ===
			// = Structure =
//			statement.executeUpdate("drop table if exists questions");
			statement.executeUpdate("create table if not exists question (id integer, score integer, autor string, message string, msgdate date)");
			
			// = Contents =
			rs = statement.executeQuery("select * from question");
			while(rs.next()) {
				this.questions.add(new CompleteUserMessage(
						rs.getInt("score"),
						rs.getString("autor"),
						rs.getString("message"),
						rs.getDate("msgdate")
				));
			}
			
			statement.close();
		} catch (SQLException e) {
			throw new Exception("SQL exception : " + e.getMessage());
		}*/
	}

	/** @return The single instance of the database 
	 * @throws Exception */
	public static Database getInstance() {
		return Database.instance;
	}
	
	/** @return Connection to the database */
	public Connection getConnection() {
		return this.connection;
	}
	
	/** Close the database connection 
	 * @throws Exception */
	public void close() throws Exception {
		/*System.out.println("Saving data to database...");
		try {
			// === TABLE USERS ===
			Statement statement = this.connection.createStatement();
			statement.setQueryTimeout(5);  // set timeout to 30 sec.
			statement.executeUpdate("delete from user");
			statement.close();
			
			PreparedStatement ps = connection.prepareStatement("insert into user (name, score) values (?, ?)");
			for (User u : this.users.values()) {
				ps.setString(1, u.getName());
				ps.setInt(2, u.getScore());
				ps.executeUpdate();
			}
			ps.close();
			
			// === TABLE QUESTION ===
			statement = connection.createStatement();
			statement.setQueryTimeout(5);  // set timeout to 30 sec.
			statement.executeUpdate("delete from question");
			statement.close();
			
			// 
			ps = connection.prepareStatement("insert into question (score, autor, message, msgdate) values (?, ?, ?, ?)");
			for (AbstractQuestion q : this.questions) {
				if (q instanceof CompleteUserMessage) {
					CompleteUserMessage c = (CompleteUserMessage) q;
					ps.setInt(1, c.getScore());
					ps.setString(2, c.getAutor());
					ps.setString(3, c.getMessage());
					ps.setDate(4, new Date(c.getDate().getTime()));
					
					ps.executeUpdate();
				}
			}
		} catch (SQLException e) {
			throw new Exception("SQL exception : " + e.getMessage(), e);
		}*/
		
		logger.info("Closing database connection...");
		try {
			this.connection.close();
		} catch (SQLException e) {
			logger.error("Error closing database connection : " + e.getMessage(), e);
		}
		this.connection = null;
	}
}
