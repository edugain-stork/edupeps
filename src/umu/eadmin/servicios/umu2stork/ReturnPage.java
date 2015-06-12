package umu.eadmin.servicios.umu2stork;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

import org.apache.commons.codec.binary.Base64;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import eu.stork.peps.auth.commons.AttributeUtil;
import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSUtil;
import eu.stork.peps.auth.commons.PEPSValues;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import eu.stork.peps.exceptions.STORKSAMLEngineException;

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

	private final long MAX_SESSION_TIME = 10 * 60000 * 2; // 60000 ms in a minute x2

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
			else 
			{
				// Generate output form
                out.println("<center><h1>" + i18n.getProperty("info.return.form") + "</h1></br><h2>" + new Date().toString()
                        + "</h2></center>");
				out.println("<form id='myForm' name='myForm' action='" + returnURLparam + "' method='post'>");

                /******************************************************/
                // Initialise OpenSAML
                BasicParserPool ppMgr = null;

                try {
                    DefaultBootstrap.bootstrap();
                    ppMgr = new BasicParserPool();
                } catch (Exception e) {
                    // TODO
                }
                // InputStream samlrespstream = new
                // ByteArrayInputStream(storkResp.getBytes());
                byte[] samlreqbase64decoded = Base64.decodeBase64(authnResponse.getTokenSaml());
                InputStream samlrespstream = new ByteArrayInputStream(samlreqbase64decoded);

                try {
                    Document samlrespdoc = ppMgr.parse(samlrespstream);
                    Element samlelement = samlrespdoc.getDocumentElement();
                    NamedNodeMap attrmap = samlelement.getAttributes();
                    for (int i = 0; i < attrmap.getLength(); i++) {
                        Node n = attrmap.item(i);
                        if (n.getNodeName().equals("AssertionConsumerServiceURL")) {
                            logger.info("Esta es la assertion url:" + n.getNodeValue());
                        }
                        logger.info(" " + attrmap.item(i));
                    }
                    logger.info(attrmap.toString());

                    UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
                    Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlelement);
                    XMLObject responseXmlObj;
                    Response responseSAML = null;
                    try {
                        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

                        responseXmlObj = unmarshaller.unmarshall(samlelement);
                        responseSAML = (Response) responseXmlObj;
                        Assertion assertion = responseSAML.getAssertions().get(0);
                        logger.info("assertion: " + assertion);
                        String subject = assertion.getSubject().getNameID().getValue();
                        logger.info("subject: " + subject);
                        String issuer = assertion.getIssuer().getValue();
                        logger.info("issuer: " + issuer);
                        String audience = assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getAudienceURI();
                        logger.info("audience: " + audience);
                        String statusCode = responseSAML.getStatus().getStatusCode().getValue();
                        logger.info("status: " + statusCode);
                        Signature sig = responseSAML.getSignature();
                        // SignatureValidator validator = new
                        // SignatureValidator(credential);
                        // validator.validate(sig);

                        // Credential signingCredential =
                        // getSigningCredential(keystore,
                        // storetype, storepass, alias, keypass);
                        //
                        // Signature signature = (Signature)
                        // Configuration.getBuilderFactory()
                        // .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                        // .buildObject(Signature.DEFAULT_ELEMENT_NAME);
                        //
                        // signature.setSigningCredential(signingCredential);
                        // signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
                        // signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
                    } catch (UnmarshallingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
                    Assertion assertion = responseSAML.getAssertions().get(0);

                    SAMLObjectBuilder<Issuer> issuerbuilder = (SAMLObjectBuilder<Issuer>) builderFactory
                            .getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
                    Issuer issuerobj = issuerbuilder.buildObject();
                    issuerobj.setValue("http://edupeps.inf.um.es");
                    responseSAML.setIssuer(issuerobj);
                    assertion.getIssuer().setValue("http://edupeps.inf.um.es");
                    logger.info("issuer 2: " + assertion.getIssuer().getValue());

                    assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0)
                            .setAudienceURI("https://storksp-test.aai.grnet.gr/shibboleth");
                    String audience = assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getAudienceURI();
                    logger.info("audience 2: " + audience);

                    try {
                        Marshaller marshallerAssertion = Configuration.getMarshallerFactory().getMarshaller(assertion);
                        marshallerAssertion.marshall(assertion);
                        Marshaller marshallerResponse = Configuration.getMarshallerFactory().getMarshaller(responseSAML);
                        Element edupepsResponseElement = marshallerResponse.marshall(responseSAML);
                        String xml = XMLHelper.nodeToString(edupepsResponseElement);
                        logger.info("response-xml:\n" + xml);
                        String eduresponseEncoded = org.opensaml.xml.util.Base64.encodeBytes(xml.getBytes(),
                                org.opensaml.xml.util.Base64.DONT_BREAK_LINES);
                        logger.info("response-encoded:\n" + eduresponseEncoded);

                        // Parameter saml response to form
                        out.println("<input type='hidden' name='DATA' value='" + eduresponseEncoded + "'>");

                    } catch (MarshallingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } catch (XMLParserException xmlparsee) {
                    System.err.println("Unable to xml parse SAMLint Request)" + xmlparsee);
                }
                /******************************************************/

                out.println("<input type='hidden' name='" + EduGAIN2StorkProxy.SERVICEHEADERSTR + "' value='" + serviceparam + "'>");
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
