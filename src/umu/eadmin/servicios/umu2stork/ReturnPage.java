package umu.eadmin.servicios.umu2stork;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.stork.peps.auth.commons.AttributeUtil;
import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.PEPSValues;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import eu.stork.peps.exceptions.STORKSAMLEngineException;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ReturnPage
 */
@WebServlet("/ReturnPage")
public class ReturnPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    public static final String HTML_START="<html>";
    public static final String HTML_END="</body></html>";
    public static final String HTML_HEAD = "<head><title>edupeps SAML Adapter</title></head>";

    private static Properties properties;
    private static Properties i18n;
        
	private final static Logger logger = Logger.getLogger(umu.eadmin.servicios.umu2stork.ReturnPage.class.getName());

	private final long MAX_SESSION_TIME = 10 * 60000; // 60000 ms in a minute

	private Stork2ProxyH2DB proxyH2db;
	   
    /**
     * @throws Exception 
     * @see HttpServlet#HttpServlet()
     */
    public ReturnPage() throws Exception {
        super();
        // TODO Auto-generated constructor stub
        try {
        	properties = new Properties();
    		properties.load(ReturnPage.class.getClassLoader().getResourceAsStream("proxyconfig.properties"));    		
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
		}catch(Exception e)
		{
			throw new ServletException("ReturnPage::init() - Unable to open Proxy H2 DB" + e);
		}

	}
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Date date = new Date();
		
		PrintWriter out = response.getWriter();
		out.println(HTML_START + "<h2>Byebye world! (GET) </h2><br/><h3>Date="+date +"</h3>"+ HTML_END);
		// TODO Auto-generated method stub
	}
	
	private void closeWithError(PrintWriter out, Properties i18n, String key)
	{
		out.println(i18n.getProperty(key));
		out.println("<b>" + i18n.getProperty("error.proxy.abort") + "</b>");
		out.println("</body>");
		out.println(HTML_END);
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		i18n = new Properties();
		i18n.load(ReturnPage.class.getClassLoader().getResourceAsStream("en.properties")); //default
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println(HTML_START);
		out.println(HTML_HEAD);
		// Auto-Send Form header
		//out.println("<body style=\"background-image:url(webapp/img/background.png); background-size:scale; background-repeat: no-repeat;background-position: center top\" onload=\"document.createElement('form').submit.call(document.getElementById('myForm'))\">");
		// Header without auto send
		out.println("<body style=\"background-image:url(webapp/img/background.png); background-size:scale; background-repeat: no-repeat;background-position: center top\">");

			
		logger.info("---- edupeps::ReturnPage::doPost() ----");
		Map<String, String> map = new HashMap<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}
        String jsessionid = "";
		try {
			String cookie = map.get("cookie");
			String []cookiesplt = cookie.split("=");
			if (cookiesplt.length < 1)
				throw new ServletException("Unable to recover jsessionid, regex problem over: " + cookie);
			jsessionid = cookiesplt[1];
		} catch (ClassCastException cce) {
			logger.severe("Unable to recover jsessionid\n" + cce);
			throw new ServletException("ReturnPage::DoPost() - Unable to recover jsessionid (InvalidCast)\n" + cce);
		} catch (NullPointerException npe) {
			logger.severe("Unable to recover jsessionid\n" + npe);
			throw new ServletException("ReturnPage::DoPost() - Unable to recover jsessionid (null)\n" + npe);
		} catch(java.lang.IndexOutOfBoundsException iobe)
		{
			logger.severe("Unable to recover jsessionid - Malformed cookie\n" + iobe);
			throw new ServletException("ReturnPage::DoPost() - Unable to recover jsessionid (IndexOutOfBoundsException)\n" + iobe);		
		}
				
		String paramSAMLResponse = null;
		
		
		logger.info("Parameters Received:");
		Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
        	String paramName = parameterNames.nextElement();
        	logger.info(paramName + ":");        	
        	String[] paramValues = request.getParameterValues(paramName);
        	for (int i = 0; i < paramValues.length; i++) {
	        	String paramValue = paramValues[i];
	        	logger.info("    " + paramValue);
	       	}
        	if (paramName.compareTo("SAMLResponse") == 0)
        		paramSAMLResponse = paramValues[0];
    	}
        
		
		String appparam  = "";
		String dataparam = "";
		String returnURLparam = "";
		String serviceparam = "";
		
		// Recover session or abort if timeout
		Stork2ProxyH2DB.Stork2ProxyH2DBSession session = null;
		try{
			session = proxyH2db.getSession(jsessionid);
		}catch(Exception e)
		{
			closeWithError(out, i18n, "error.return.jsession");
			throw new ServletException("Unable to recover session: " + jsessionid);
		}
		// Carga de i18n de otros idiomas
		String langparam = session.getLang();
		if (langparam.equals("es"))
		{
			i18n = new Properties();
			i18n.load(ReturnPage.class.getClassLoader().getResourceAsStream("es.properties"));
		}
		
		Date maxallowedtime = new Date(session.getSessiontime().getTime() + MAX_SESSION_TIME);
		Date actualtime = new Date();
		out.println("ActualTime: " + actualtime + "\nsessiontime: " + session.getSessiontime() + "/" + session.getSessiontimestr() + "\n maxallowedtime: " + maxallowedtime);
		if (actualtime.after(maxallowedtime))
		{
			// closeWithError(out, i18n, "error.return.timeout");
			logger.severe("Session TimedOut " + jsessionid );
			if(!proxyH2db.deleteSession(jsessionid))
				logger.severe("Unable to remove session: " + jsessionid);
			out.println("WARNING: Ignoring session timeout!");
			// return;
		}
		logger.info("Session OK, deleting session from database");
		if(!proxyH2db.deleteSession(jsessionid))
		{
			logger.severe("Unable to remove session after successfully found: " + jsessionid);
			closeWithError(out, i18n, "error.return.jsession");
			return;
		}
		
		appparam = session.getAppname();
		dataparam = session.getUuid();
		returnURLparam = session.getUrl();
		serviceparam = session.getService();

        //STORKAttrQueryResponse response = new STORKAttrQueryResponse();
		STORKAuthnResponse authnResponse = null;
		IPersonalAttributeList personalAttributeList = null;
		
		/// String spUrl = properties.getProperty("proxy.url");
		//Decodes SAML Response
		byte[] decSamlToken = PEPSUtil.decodeSAMLToken(paramSAMLResponse);
      	
		//Get SAMLEngine instance
		STORKSAMLEngine engine = STORKSAMLEngine.getInstance("SP");	
		if (engine != null)
		{		
			try {
				//validate SAML Token
				authnResponse = engine.validateSTORKAuthnResponse(decSamlToken, (String) request.getRemoteHost());
				logger.info("STORKAuthnResponse Id: " + authnResponse.getSamlId());
				logger.info("Base64 SAML Response: " + new String(authnResponse.getTokenSaml()));
				
				
				//response = engine.validateSTORKAttrQueryResponse(decSamlToken, (String) request.getRemoteHost());
			} catch(STORKSAMLEngineException e){			
				logger.severe("Could not validate token for Saml Response: \n"+ e.getErrorMessage());
			}
			
			if(authnResponse.isFail()){
				logger.severe("Saml Response is fail:\n"+ authnResponse.getMessage());
				closeWithError(out, i18n, "error.return.saml");
				return;
			}
		}
		
		// GENERAR POST AL CAS
		if (engine != null)
		{		
			try {
				//validate SAML Token
				authnResponse = engine.validateSTORKAuthnResponse(decSamlToken, (String) request.getRemoteHost());
				logger.info("STORKAuthnResponse Id: " + authnResponse.getSamlId());
				logger.info("Base64 SAML Response: " + new String(authnResponse.getTokenSaml()));
				
				
				//response = engine.validateSTORKAttrQueryResponse(decSamlToken, (String) request.getRemoteHost());
			} catch(STORKSAMLEngineException e){			
				logger.severe("Could not validate token for Saml Response: \n"+ e.getErrorMessage());
			}
			
			if(authnResponse.isFail()){
				logger.severe("Saml Response is fail:\n"+ authnResponse.getMessage());						
			}
			else 
			{

				// Generate output form
				out.println("<center><h1>" + i18n.getProperty("info.return.cas") + "</h1></br><h2>" + new Date().toString() + "</h2></center>");
				out.println("<form id='myForm' name='myForm' action='" + returnURLparam + "' method='post'>");

				// Todos los parámetros, incluyendo los iniciales del CAS (DATA, URL, APP y SERVCE) se meten en un ArrayJSON que codificado con rsa
				JSONArray parametros = new JSONArray();
				JSONObject parametro = new JSONObject();
				try{
					parametro.put(EduGAIN2StorkProxy.APPHEADERSTR, appparam);
					parametros.put(parametro);
					parametro = new JSONObject();
					parametro.put(EduGAIN2StorkProxy.URLHEADERSTR, returnURLparam);
					parametros.put(parametro);
					parametro = new JSONObject();
					parametro.put(EduGAIN2StorkProxy.DATAHEADERSTR, dataparam);
					parametros.put(parametro);
					parametro = new JSONObject();
					parametro.put(EduGAIN2StorkProxy.SERVICEHEADERSTR, serviceparam);
					parametros.put(parametro);

					personalAttributeList = authnResponse.getPersonalAttributeList();
					Map<String, JSONArray> atributosComplejos = new HashMap<String, JSONArray>();
					ArrayList<PersonalAttribute> attrList = new ArrayList<PersonalAttribute>(personalAttributeList.values());
					for (PersonalAttribute pa: attrList) {
						if (!pa.isEmptyComplexValue())
						{
							Map<String,String>complex = pa.getComplexValue();
							// Crear JSON para atributos complejos
							JSONObject complexjson = new JSONObject();
							for (String key: complex.keySet())
							{
								try {
									complexjson.put(key, complex.get(key));
								}
								catch(JSONException jse)
								{
									logger.severe("Imposible introducir JSON STORK Complex attr " + key + ": " + complex.get(key));
									throw new ServletException(jse);
								}
							}
							if (atributosComplejos.containsKey(pa.getName()))
							{
								JSONArray array = atributosComplejos.get(pa.getName());
								array.put(complexjson);
							}
							else
							{
								JSONArray array = new JSONArray();
								array.put(complexjson);
								atributosComplejos.put(pa.getName(), array);
							}
						}
						else
						{
							parametro = new JSONObject();
							if (pa.getValue().size() > 1)
								parametro.put(pa.getName(), AttributeUtil.listToString(pa.getValue(),PEPSValues.ATTRIBUTE_VALUE_SEP.toString()));
							else if (pa.getValue().size() == 1 && pa.getValue().get(0).length() > 0)
								parametro.put(pa.getName(), pa.getValue().get(0));
							parametros.put(parametro);
						}
					}

					//Ahora tenemos que meter los atributos complejos
					for(String key: atributosComplejos.keySet())
					{
						parametro=new JSONObject();
						parametro.put(key, atributosComplejos.get(key));
						parametros.put(parametro);
					}

				} catch (JSONException jse)
				{
					logger.severe("Imposible introducir parámetros del CAS en JSON: " + jse);
					throw new ServletException(jse);
				}
				logger.info("Parametros: " + parametros.toString());
				out.println("Received Values: " + parametros.toString());

				// Parametros a form
				out.println("<input type='hidden' name='DATA' value='"+ Base64.encodeBase64String(parametros.toString().getBytes()) + "'>");
				out.println("<input type='hidden' name='" + EduGAIN2StorkProxy.SERVICEHEADERSTR + "' value='"+ serviceparam + "'>");

				//out.println("<center><input type='submit' value='Send' method='post'></center>");
				out.println("<center><button type='submit' value='Send' method='post'><img src='webapp/img/send.png' width=25 border=3></button></center>");
				out.println("</form>");
			}

		}
		out.println(HTML_END);
	}

	/**
	   * Copied from branches/dev_branch/Commons/src/main/java/eu/stork/peps/auth/commons/PersonalAttribute.java
	   * 
	   * Prints the PersonalAttribute in the following format.
	   * name:required:[v,a,l,u,e,s]|[v=a,l=u,e=s]:status;
	   * 
	   * @return The PersonalAttribute as a string.
	   */
	  public String printPersonalAttribute(PersonalAttribute pa) {
	    final StringBuilder strBuild = new StringBuilder();

	    AttributeUtil.appendIfNotNull(strBuild, "Stork."+pa.getName());
	    strBuild.append(PEPSValues.ATTRIBUTE_TUPLE_SEP.toString());
	    AttributeUtil.appendIfNotNull(strBuild, String.valueOf(pa.isRequired()));
	    strBuild.append(PEPSValues.ATTRIBUTE_TUPLE_SEP.toString());
	    strBuild.append('[');

	    if (pa.isEmptyValue()) {
	      if (!pa.isEmptyComplexValue()) {
	        AttributeUtil.appendIfNotNull(strBuild, AttributeUtil.mapToString(pa.getComplexValue(), PEPSValues.ATTRIBUTE_VALUE_SEP.toString()));	    	
	      }
	    } else {
	    	AttributeUtil.appendIfNotNull (strBuild, AttributeUtil.listToString(pa.getValue(), PEPSValues.ATTRIBUTE_VALUE_SEP.toString()));	    	      
	    }

	    strBuild.append(']');
	    strBuild.append(PEPSValues.ATTRIBUTE_TUPLE_SEP.toString());
	    AttributeUtil.appendIfNotNull(strBuild, pa.getStatus());
	    strBuild.append(PEPSValues.ATTRIBUTE_SEP.toString());

	    return strBuild.toString();
	  }
}
