/**
 * 
 */
package umu.eadmin.servicios.umu2stork;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * @author Jordi Ortiz
 * In memory H2DB to store session for Stork2 UMU's Proxy
 */
public class Stork2ProxyH2DB {
	
	public class Stork2ProxyH2DBSession {
		String jsessionid;
		String uuid; 
		String appname; 
		String url; 
		String service;
		Timestamp sessiontime;
		String sessiontimestr;
		String lang;
		
		public Stork2ProxyH2DBSession()
		{
		
		}
		public String getJsessionid() {
			return jsessionid;
		}
		public void setJsessionid(String jsessionid) {
			this.jsessionid = jsessionid;
		}
		public String getUuid() {
			return uuid;
		}
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
		public String getAppname() {
			return appname;
		}
		public void setAppname(String appname) {
			this.appname = appname;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getService() {
			return service;
		}
		public void setService(String service) {
			this.service = service;
		}
		public String getLang() {
			return lang;
		}
		public void setLang(String lang) {
			this.lang = lang;
		}
		public Timestamp getSessiontime() {
			return sessiontime;
		}
		public void setSessiontime(Timestamp sessiontime) {
			this.sessiontime = sessiontime;
		}
		public String getSessiontimestr() {
			return sessiontimestr;
		}
		public void setSessiontimestr(String sessiontimestr) {
			this.sessiontimestr = sessiontimestr;
		}
	}
	
	
	private final static Logger logger = Logger
			.getLogger(umu.eadmin.servicios.umu2stork.UMU2StorkProxy.class
					.getName());
	

	private static Stork2ProxyH2DB db;
	private final String H2_IN_MEMORY_DB = "jdbc:h2:mem:UMU2StorkProxySessions";	
	
	Connection h2connection;

	/**
	 * 
	 */
	private Stork2ProxyH2DB() throws Exception {
		// TODO Auto-generated constructor stub
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			logger.severe("Stork2ProxyH2DB() - Unable to load H2 Driver");
			throw new Exception("Stork2ProxyH2DB() - Unable to load H2 Driver");
		}
		// Create In-memory Database
		try {
			h2connection = DriverManager.getConnection(H2_IN_MEMORY_DB);
			PreparedStatement s = h2connection.prepareStatement("CREATE TABLE IF NOT EXISTS JSESSIONSTATUS (JSESSIONID VARCHAR (128), FECHA TIMESTAMP, UUID VARCHAR(512), APP VARCHAR(128), URL VARCHAR(256), SERVICE VARCHAR(256), LANG VARCHAR(128))");
			s.execute();
			s.close();
		} catch (SQLException sqle) {
			logger.severe("Unable to create JSESSIONID,DATE H2 Database in memory");
			throw new Exception(
					"Stork2ProxyH2DB() - Unable to create JSESSIONID,DATE H2 Database in memory\n" + sqle.toString());
		}
	}
	
	static public Stork2ProxyH2DB getInstance() throws Exception
	{
		if (db == null)
			db = new Stork2ProxyH2DB();
		return db;
	}
	
	public boolean saveSession(String jsessionid, String uuid, String appname, String url, String service, String lang) throws Exception
	{
		// SAVE SESSION
		try{
			Statement s = h2connection.createStatement();
			s.execute("DELETE FROM JSESSIONSTATUS WHERE JSESSIONID='" + jsessionid + "'");
			Date sessiontime = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS");
			String sessiontimestr = sdf.format(sessiontime); 
			logger.info("Inserting session: " + "'" + jsessionid + "', parsedatetime('" + sessiontimestr  + "','dd-MM-yyyy hh:mm:ss.SS')" );
			s.executeUpdate("INSERT INTO JSESSIONSTATUS VALUES('" + jsessionid + "', parsedatetime('" + sessiontimestr + "','dd-MM-yyyy hh:mm:ss.SS'),'" + uuid + "','" + appname + "', '" + url + "', '" + service + "', '" + lang + "')");
			s.close();
		}catch(SQLException sqle)
		{
			logger.severe("Unable to insert jsessionid in DB\n" + sqle);
			throw new Exception("Stork2ProxyH2DB:saveSession() - Unable to insert jsessionid in DB\n" + sqle);
		}
		return true;
	}
	
	public boolean deleteSession(String jsessionid)
	{
		try{
			Statement s = h2connection.createStatement();
			s.execute("DELETE FROM JSESSIONSTATUS WHERE JSESSIONID='" + jsessionid + "'");
			s.close();
		}
		catch(SQLException sqle)
		{
			logger.severe("Unable to remove jsessionid from DB\n" + sqle);
			return false;
		}
		return true;
	}
	
	public Stork2ProxyH2DBSession getSession(String jsessionid) throws Exception
	{
		Stork2ProxyH2DBSession session = new Stork2ProxyH2DBSession();
		try{
			ResultSet rs;
			Statement s = h2connection.createStatement();
			rs = s.executeQuery("SELECT FECHA, UUID, APP, URL, SERVICE, LANG FROM JSESSIONSTATUS WHERE JSESSIONID='" + jsessionid + "'");
			boolean data = rs.first();
			if (!data) {
				logger.severe("Session was not found in database " + jsessionid );
				throw new Exception("Stork2ProxyH2DBSession:getSession() - Session was not found JSESSIONID: " + jsessionid);
			}
			String sessiontimestr = rs.getString(1);
			Timestamp sessiontime = rs.getTimestamp(1);
			String uuid = rs.getString(2);
			String app  = rs.getString(3);
			String url = rs.getString(4);
			String service = rs.getString(5);
			String lang = rs.getString(6);
			rs.close();
			s.close();
			
			session.setJsessionid(jsessionid);
			session.setUuid(uuid);
			session.setAppname(app);
			session.setUrl(url);
			session.setService(service);
			session.setSessiontime(sessiontime);
			session.setSessiontimestr(sessiontimestr);
			session.setLang(lang);
		}
		catch(SQLException sqle)
		{
			logger.severe("Unable to check jsessionid in DB\n" + sqle);
			throw new Exception("Stork2ProxyH2DBSession:getSession() - Unable to check jsessionid in DB\n" + sqle);
		}
		return session;
	}
	
	

}
