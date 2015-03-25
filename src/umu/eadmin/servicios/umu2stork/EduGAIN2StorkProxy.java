package umu.eadmin.servicios.umu2stork;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.util.encoders.Base64;

import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
//import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.STORKAuthnRequest;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import eu.stork.peps.exceptions.STORKSAMLEngineException;

/**
 * Servlet implementation class edupeps
 */
@WebServlet("/EduGAIN2StorkProxy")
public class EduGAIN2StorkProxy extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    public static final String PRIVATE_KEY_FILE_PARAM = "proxy.privatekey";
    
	public static final String DATAHEADERSTR = "DATA";
	public static final String URLHEADERSTR = "URL";
	public static final String APPHEADERSTR = "APP";
	public static final String SERVICEHEADERSTR = "service";
	public static final String LANGHEADERSTR = "lang";
	
	private static final String PROPERTIES_APP_PARAM_PREFIX = "proxy.apps";
	private static final String PROPERTIES_MANDPROP_PARAM_POSTFIX = ".mandatoryattributes";
	private static final String PROPERTIES_OPTPROP_PARAM_POSTFIX = ".optionalattributes";
	private static final String PROPERTIES_APP_URL_POSTFIX = ".url";
	
	private static final String PROPERTIES_PEPS_URL_PARAM = "proxy.peps";
	private static final String PROPERTIES_RETURNPAGE_URL_PARAM = "proxy.return";
	private static final String PROPERTIES_SPNAME_PARAM = "proxy.spName";
	private static final String PROPERTIES_SPSECTOR_PARAM = "proxy.spSector";
	private static final String PROPERTIES_SPINSTITUTION_PARAM = "proxy.spInstitution";
	private static final String PROPERTIES_SPAPP_PARAM = "proxy.spApplication";
	private static final String PROPERTIES_SPCOUNTRY_PARAM = "proxy.spCountry";
	private static final String PROPERTIES_SPID_PARAM = "proxy.spId";

	
	
	public static final String HTML_START = "<html>";
	public static final String HTML_END = "</html>";
	public static final String BODY_START = "<body>";
	public static final String BODY_END = "</body>";
	public static final String HTML_HEAD = "<head><title>edupeps SAML Adapter</title></head>";
	
	//Attributes
	private static Properties properties;
	private static Properties i18n;
	private static Map<String,String[]> mandattributesxapp;
	private static Map<String,String[]> optattributesxapp;
	private static String returnPageUrl;
	private static String PEPSPageUrl;
	private static String spname;
	private static String spsector;
	private static String spinstitution;
	private static String spapp;
	private static String spcountry;
	private static String spid;
	
	private Stork2ProxyH2DB proxyH2db;

	private final static Logger logger = Logger
			.getLogger(umu.eadmin.servicios.umu2stork.EduGAIN2StorkProxy.class
					.getName());


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public EduGAIN2StorkProxy() throws Exception {
		super();
		// TODO Auto-generated constructor stub
		mandattributesxapp = new HashMap<String, String[]>();
		optattributesxapp = new HashMap<String, String[]>();

		try {
	        	properties = new Properties();
	    		properties.load(ReturnPage.class.getClassLoader().getResourceAsStream("proxyconfig.properties"));
	    		// Load app configuration
	    		logger.info(PROPERTIES_APP_PARAM_PREFIX);
	    		String appsproperty = properties.getProperty(PROPERTIES_APP_PARAM_PREFIX);
	    		logger.info("ATTR:" + appsproperty);
	    		String []appsarray = appsproperty.split(";");
	    		for(String app: appsarray)
	    		{
	    			logger.info(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_MANDPROP_PARAM_POSTFIX);
	    			String mandappattributesproperty = properties.getProperty(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_MANDPROP_PARAM_POSTFIX);
	    			if (mandappattributesproperty != null){
		    			logger.info("MANDATTRPROP:" + mandappattributesproperty);
		    			String []mandpropertiesarray = mandappattributesproperty.split(";");
	    				mandattributesxapp.put(app, mandpropertiesarray);
	    			}
	    			logger.info(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_OPTPROP_PARAM_POSTFIX);
	    		
	    			String appattributesproperty = properties.getProperty(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_OPTPROP_PARAM_POSTFIX);
	    			if (appattributesproperty != null)
	    			{
		    			logger.info("MANDATTRPROP:" + appattributesproperty);
		    			String []propertiesarray = appattributesproperty.split(";");
	    				optattributesxapp.put(app, propertiesarray);
	    			}
	    		}
	    		
	    		EduGAIN2StorkProxy.PEPSPageUrl = properties.getProperty(PROPERTIES_PEPS_URL_PARAM);
	    		EduGAIN2StorkProxy.returnPageUrl = properties.getProperty(PROPERTIES_RETURNPAGE_URL_PARAM);
	    		EduGAIN2StorkProxy.spname = properties.getProperty(PROPERTIES_SPNAME_PARAM);
	    		EduGAIN2StorkProxy.spsector = properties.getProperty(PROPERTIES_SPSECTOR_PARAM);
	    		EduGAIN2StorkProxy.spinstitution = properties.getProperty(PROPERTIES_SPINSTITUTION_PARAM);
	    		EduGAIN2StorkProxy.spapp= properties.getProperty(PROPERTIES_SPAPP_PARAM);
	    		EduGAIN2StorkProxy.spcountry = properties.getProperty(PROPERTIES_SPCOUNTRY_PARAM);
	    		EduGAIN2StorkProxy.spid = properties.getProperty(PROPERTIES_SPID_PARAM);
	    		
	    		
			} catch (IOException e) {
				throw new Exception("Could not load configuration file " + e.getMessage());
			}
	}

	/**
	 * @see HttpServlet#init()
	 */
	public void init() throws ServletException {
		try{
		proxyH2db = Stork2ProxyH2DB.getInstance();
		}catch (Exception e)
		{
			throw new ServletException("Unable to start in memory DB");
		}

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Date date = new Date();

		PrintWriter out = response.getWriter();
		out.println(HTML_START + BODY_START + "<h2>Hello world! (GET) </h2><br/><h3>Date="
				+ date + "</h3>" + BODY_END + HTML_END);

	} // TODO Auto-generated method stub

	private void closeWithError(PrintWriter out, Properties i18n, String key)
	{
		out.println(i18n.getProperty(key));
		out.println("<b>" + i18n.getProperty("error.proxy.abort") + "</b>");
		out.println("</body>");
		out.println(HTML_END);
	}
	
	private String printXMLNode(Node n, int depth)
	{
		String result = "";
		for( int i=0; i < depth; i++)
			result += "&emsp;";
		result += n.getNodeName() + ": " + n.getNodeValue();
		if (n.hasChildNodes())
		{
			for (int i=0; i < n.getChildNodes().getLength(); i++)
			{
				result += "</BR>" + printXMLNode(n.getChildNodes().item(i), depth + 1);
			}
		}
		return result;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		logger.info("---- edupeps::EduGAIN2StorkProxy::doPost() ----");

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		i18n = new Properties();
		i18n.load(ReturnPage.class.getClassLoader().getResourceAsStream("en.properties")); //default
		
		UtilesRsa encoder = new UtilesRsa();
		logger.info("Cert file: " + request.getSession().getServletContext().getRealPath("WEB-INF/classes/" + properties.getProperty(PRIVATE_KEY_FILE_PARAM)).toString());
		String keyfile = request.getSession().getServletContext().getRealPath("WEB-INF/classes/" + properties.getProperty(PRIVATE_KEY_FILE_PARAM));
		
		encoder.readPrivateKey(keyfile);
		
		out.println(HTML_START);
		out.println(HTML_HEAD);
		// AUTO LOAD FORM out.println("<body style=\"background-image:url(webapp/img/background.png); background-size:scale; background-repeat: no-repeat;background-position: center top\" onload=\"document.createElement('form').submit.call(document.getElementById('myForm'))\">");
		out.println("<body style=\"background-image:url(webapp/img/background.png); background-size:scale; background-repeat: no-repeat;background-position: center top\">");

		// Enumeration <String> params = request.getParameterNames();

		Map<String, String> headerparammap = new HashMap<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			headerparammap.put(key, value);
		}
		for (String aux : headerparammap.values()) {
			logger.info("\tparam: " + aux);

		}

		String jsessionid = "";
		try {
			String cookie = headerparammap.get("cookie");
			String []cookiesplt = cookie.split("=");
			if (cookiesplt.length < 1)
				throw new ServletException("Unable to recover jsessionid, regex problem over: " + cookie);
			jsessionid = cookiesplt[1];
		} catch (ClassCastException cce) {
			logger.severe("Unable to recover jsessionid\n" + cce);
			throw new ServletException("eduGAIN2StorkProxy::DoPost() - Unable to recover jsessionid (InvalidCast)\n" + cce);
		} catch (NullPointerException npe) {
			logger.severe("Unable to recover jsessionid\n" + npe);
			throw new ServletException("eduGAIN2StorkProxy::DoPost() - Unable to recover jsessionid (null)\n" + npe);
		} catch(java.lang.IndexOutOfBoundsException iobe)
		{
			logger.severe("Unable to recover jsessionid - Malformed cookie\n" + iobe);
			throw new ServletException("eduGAIN2StorkProxy::DoPost() - Unable to recover jsessionid (IndexOutOfBoundsException)\n" + iobe);
		}
		
		// Carga de i18n de otros idiomas
		String langparam = request.getParameter(LANGHEADERSTR);
		if (langparam != null)
			if (langparam.equals("es"))
			{
				i18n = new Properties();
				i18n.load(ReturnPage.class.getClassLoader().getResourceAsStream("es.properties"));
			}
			else
				langparam="en";
		{
			i18n = new Properties();
			i18n.load(ReturnPage.class.getClassLoader().getResourceAsStream("es.properties"));
		}
		else
			langparam="en";
		
		// Recuperar atributo de país
		String countryCodeParam = request.getParameter("CountryCode");
		if (countryCodeParam == null) {
			logger.severe("FATAL ERROR: Missing Country Code Parameter, abort!");
			this.log("FATAL ERROR: Missing Country Code Parameter, abort!");
			closeWithError(out,i18n,"error.proxy.contrycode");
			return;
		}
		logger.info("CountryCode: " + countryCodeParam);
		
		// Recuperando atributo de APP
		String appparam = java.net.URLDecoder.decode(encoder.decode(request.getParameter(APPHEADERSTR)), "UTF-8");
		if  (appparam == null || appparam=="null")
		{
			logger.severe("FATAL ERROR: Missing appname, abort!");
			this.log("FATAL ERROR: Missing appname, abort!");
			closeWithError(out,i18n,"error.proxy.appextra");
			return;
		}
		logger.info("APP: " + appparam);
		
		String dataparam = java.net.URLDecoder.decode(encoder.decode(request.getParameter(DATAHEADERSTR)), "UTF-8");
		if (dataparam == null)
		{
			logger.severe("FATAL ERROR: Missing DATA, abort!");
			this.log("FATAL ERROR: Missing DATA, abort!");
			closeWithError(out,i18n,"error.proxy.data");
			return;
		}
		String dataparamdecoded = dataparam;
		logger.info("DATA: " + dataparam);
		
		String returnURLparam = java.net.URLDecoder.decode(encoder.decode(request.getParameter(URLHEADERSTR)), "UTF-8");
		if (returnURLparam == null)
		{
			logger.severe("FATAL ERROR: Missing URL, abort!");
			this.log("FATAL ERROR: Missing URL, abort!");
			closeWithError(out,i18n,"error.proxy.url");
			return;
		}
		logger.info("URL: " + returnURLparam);
		
		String serviceparam = java.net.URLDecoder.decode(encoder.decode(request.getParameter(SERVICEHEADERSTR)), "UTF-8");
		if (serviceparam==null)
		{
			logger.severe("FATAL ERROR: Missing service, abort!");
			this.log("FATAL ERROR: Missing service, abort!");
			closeWithError(out,i18n,"error.proxy.service");
			return;
		}
		logger.info("URL: " + serviceparam);
		
	
		
		logger.info("Creando Personal Attribute List para consulta");
		PersonalAttributeList pal = new PersonalAttributeList();
		PersonalAttribute pa = null;
	
		//Seleccion de atributos basado en service url
		boolean appfound = false;
		// Buscar la app con la url indicada
		for (String app:optattributesxapp.keySet()) {
			logger.info("Checking: " + properties.getProperty(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_APP_URL_POSTFIX) + " vs " + serviceparam + "</BR>");
			if (properties.getProperty(PROPERTIES_APP_PARAM_PREFIX + "." + app + PROPERTIES_APP_URL_POSTFIX).equals(serviceparam))
			{
				if (optattributesxapp.containsKey(app))
				{
					String []mandattrs = mandattributesxapp.get(app);
					for (String attr: mandattrs)
					{
						logger.info("Mandatory " + app + " : " + attr);
						pa = new PersonalAttribute();
						pa.setName(attr);
						pa.setIsRequired(true);
						pal.add(pa);
					}
					
					String []attrs = optattributesxapp.get(app);
					for (String attr: attrs)
					{
						logger.info("Optional " + app + " : " + attr);
						pa = new PersonalAttribute();
						pa.setName(attr);
						pa.setIsRequired(false);
						pal.add(pa);
					}
					appfound = true;
				}
			}
		}
		if (!appfound)
		{
			logger.info("Servicio Desconocido " + serviceparam);
			closeWithError(out,i18n,"error.proxy.appunk");
			return;
			
		}
		
		
		int QAA = 1;

		final String destinationURL = EduGAIN2StorkProxy.PEPSPageUrl;
		final String assertConsumerUrl = EduGAIN2StorkProxy.returnPageUrl;
		
	
		final String spName = EduGAIN2StorkProxy.spname;
		final String spSector = EduGAIN2StorkProxy.spsector;
		final String spInstitution = EduGAIN2StorkProxy.spinstitution;
		final String spApplication = EduGAIN2StorkProxy.spapp;
		final String spCountry = EduGAIN2StorkProxy.spcountry;
		final String spId = EduGAIN2StorkProxy.spid;

		final STORKAuthnRequest authRequest = new STORKAuthnRequest();

		logger.info("Generando STORK Auth Request");

		authRequest.setDestination(destinationURL);
		authRequest.setProviderName(spName);
		authRequest.setQaa(QAA);
		authRequest.setPersonalAttributeList(pal);
		authRequest.setAssertionConsumerServiceURL(assertConsumerUrl);

		// new parameters
		authRequest.setSpSector(spSector);
		authRequest.setSpInstitution(spInstitution);
		authRequest.setSpApplication(spApplication);
		authRequest.setSpCountry(spCountry);
		authRequest.setSPID(spId);
		authRequest.setCitizenCountryCode(countryCodeParam);
		authRequest.setPersonalAttributeList(pal);

		logger.info("Recuperando STORK Engine");

		STORKSAMLEngine engine = STORKSAMLEngine.getInstance("SP"); // SP -
																	// Magic
																	// Number?

		String authReqSTORKString = "";
		final STORKAuthnRequest saml;
		try {
			// engine.generateSTORKAuthnRequest(authRequest);
			logger.info("Generar STORK SAML Auth Request desde el auth request");
			saml = engine.generateSTORKAuthnRequest(authRequest);
			byte[] authReqSTORKbytes = Base64.encode(saml.getTokenSaml());
			authReqSTORKString = new String(authReqSTORKbytes);
			logger.info("STORKAuthnRequest Size: " + authReqSTORKbytes);
			logger.info("STORKAuthnRequest String: " + authReqSTORKString);
			logger.info("STORKAuthnRequest Id: " + saml.getSamlId());
			
			out.println("<form id='myForm' name='myForm' action='" + destinationURL + "' method='post'>");
			out.println("<input type='hidden' name='country' value='" + countryCodeParam + "'>");
			out.println("<input type='hidden' name='SAMLRequest' value='"+ authReqSTORKString + "'>");
			out.println("</form>");

		} catch (STORKSAMLEngineException e) {
			out.println(i18n.getProperty("error.proxy.saml") +" " + e);
			logger.severe("Engine error generating the Stork Authn Request");
			e.printStackTrace();
		}

//		// SAVE SESSION
		try {
			this.proxyH2db.saveSession(jsessionid, dataparamdecoded, appparam, returnURLparam, serviceparam, langparam);
		}
		catch (Exception e)
		{
			throw new ServletException("DB Problem: " + e);
		}
//		try{
//			Statement s = h2connection.createStatement();
//			s.execute("DELETE FROM JSESSIONSTATUS WHERE JSESSIONID='" + jsessionid + "'");
//			Date sessiontime = new Date();
//			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS");
//			String sessiontimestr = sdf.format(sessiontime); 
//			logger.info("Inserting session: " + "'" + jsessionid + "', parsedatetime('" + sessiontimestr  + "','dd-MM-yyyy hh:mm:ss.SS')" );
//			s.executeUpdate("INSERT INTO JSESSIONSTATUS VALUES('" + jsessionid + "', parsedatetime('" + sessiontimestr + "','dd-MM-yyyy hh:mm:ss.SS'),'" + dataparamdecoded + "','" + appname + "', '" + returnURLparam + "', '" + serviceparam + "')");
//			s.close();
//		}catch(SQLException sqle)
//		{
//			logger.severe("Unable to insert jsessionid in DB\n" + sqle);
//			throw new ServletException("eduGAIN2StorkProxy::DoPost() - Unable to insert jsessionid in DB\n" + sqle);
//		}


		Date date = new Date();

		out.println("<h2>" + i18n.getProperty("info.proxy.wait") + "</h2><br/><h3>Date="
				+ date + "</h3><br>");

		out.println("</body>");
		out.println(HTML_END);
		out.close();

	}

}
