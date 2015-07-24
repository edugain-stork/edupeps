package umu.eadmin.servicios.umu2stork;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.ResponseMarshaller;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
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
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import eu.stork.peps.exceptions.STORKSAMLEngineException;
import eu.storkWebedu.translator.EGAttribute;
import eu.storkWebedu.translator.Translator;


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
		
		String proxyID = properties.getProperty("proxy.entityID");
		logger.info("proxy.entityID: "+proxyID);
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

                // Get PersonalAttributeList form StorkResponse
                personalAttributeList = authnResponse.getPersonalAttributeList();

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
                byte[] samlreqbase64decoded = Base64.decodeBase64(paramSAMLResponse);
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
                    //XMLObject responseXmlObj;
                    
                    Response responseSAML = null;
                    Response responseSAMLfromPEPS = null;
                    try {
                        responseSAMLfromPEPS = (Response) unmarshaller.unmarshall(samlelement);
                    } catch (UnmarshallingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    Assertion assertionPEPS = responseSAMLfromPEPS.getAssertions().get(0);
                    XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
                    
/*                    SAMLObjectBuilder<Response> responseBuilder = (SAMLObjectBuilder<Response>)builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
                    responseSAML = responseBuilder.buildObject();
                    responseSAML.setID(UUID.randomUUID().toString());
                    responseSAML.setIssueInstant(new DateTime());
                    responseSAML.setInResponseTo(responseSAMLfromPEPS.getInResponseTo());
                    responseSAML.setVersion(SAMLVersion.VERSION_20);

                    SAMLObjectBuilder<Issuer> issuerbuilder = (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
                    Issuer issuerobj = issuerbuilder.buildObject();
                    issuerobj.setValue(proxyID);
                    responseSAML.setIssuer(issuerobj);
                    //assertion.getIssuer().setValue(proxyID);
                    logger.info("issuer 2 response: " + responseSAML.getIssuer().getValue());

                    Status status  = ((SAMLObjectBuilder<Status>) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME)).buildObject();
                    StatusCode statuscode = ((SAMLObjectBuilder<StatusCode>) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME)).buildObject();
                    statuscode.setValue(StatusCode.SUCCESS_URI);
                    status.setStatusCode(statuscode);
                    responseSAML.setStatus(status);

                    responseSAML.setDestination(serviceparam);

                    SAMLObjectBuilder<Assertion> assertionBuilder = (SAMLObjectBuilder<Assertion>)builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
                    Assertion assertion = assertionBuilder.buildObject();
                    assertion.setID(UUID.randomUUID().toString());
                    assertion.setIssueInstant(new DateTime());
                    assertion.setVersion(SAMLVersion.VERSION_20);

                    Issuer assertionIssuer = ((SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME)).buildObject();
                    assertionIssuer.setValue(proxyID);
                    assertion.setIssuer(assertionIssuer);

                    responseSAML.getAssertions().add(assertion);
*/

                    String statusCode = responseSAMLfromPEPS.getStatus().getStatusCode().getValue();
                    logger.info("responseSAMLfromPEPS status: " + statusCode);
                    SAMLObjectBuilder<Issuer> issuerbuilder = (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
                    Issuer issuerobj = issuerbuilder.buildObject();
                    issuerobj.setValue(proxyID);
                    responseSAMLfromPEPS.setIssuer(issuerobj);
                    logger.info("responseSAMLfromPEPS Issuer: " + issuerobj.getValue());
                    responseSAMLfromPEPS.setDestination(returnURLparam);
                    
                    logger.info("assertionPEPS: " + assertionPEPS);
                    String subject = assertionPEPS.getSubject().getNameID().getValue();
                    logger.info("assertionPEPS subject: " + subject);
                    assertionPEPS.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().setRecipient(returnURLparam);
                    logger.info("assertionPEPS subject recipient: " +  assertionPEPS.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getRecipient());
                    Issuer assertionIssuer = ((SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME)).buildObject();
                    assertionIssuer.setValue(proxyID);
                    assertionPEPS.setIssuer(assertionIssuer);
                    logger.info("assertionPEPS issuer: " + assertionPEPS.getIssuer().getValue());
                    assertionPEPS.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).setAudienceURI(serviceparam);
                    logger.info("assertionPEPS audience: " + assertionPEPS.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getAudienceURI());
                    assertionPEPS.getConditions().getConditions().remove(assertionPEPS.getConditions().getOneTimeUse());
                    
                    
                    // Attribute translation from stork format to edugain format
                    logger.info(" personalAttributeList: \n"+personalAttributeList.size());
                    logger.info(" personalAttributeList get(0): "+personalAttributeList.iterator().next().getName());
                    ArrayList<Attribute>[] aledugain = eu.storkWebedu.translator.Translator.translateToOpenSAML(personalAttributeList);
                    if (aledugain != null)
                    	logger.info ("aledugain not null");
                    else 
                    	logger.info ("aledugain is null");
                    
                    logger.info(" aledugain: \ntranslated: " + aledugain[0].size() + "\tpassthrough: "+aledugain[1].size());
                    logger.info(" aledugain get(0): \n"+ aledugain[0].get(0).getName());

                    assertionPEPS.getAttributeStatements().get(0).getAttributes().clear();
                    assertionPEPS.getAttributeStatements().get(0).getAttributes().addAll(aledugain[0]);
                    assertionPEPS.getAttributeStatements().get(0).getAttributes().addAll(aledugain[1]);



                    /***************/
                    
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

                    /**********************************************************/
                    
                    Assertion assertionSign = assertionPEPS;
                    Response responseSign = responseSAMLfromPEPS;
                    //Assertion assertionSign = assertion;
                    //Response responseSign = responseSAML;

                    SigningCredential sign = new SigningCredential();
                    //intializeCredentials(String passwordParam, String certAliasNameParam, String keyAliasNameParam, String fileNameParam) {
                    
                    //sign.intializeCredentials("local-demo", "local-demo-cert", "local-demo",  "/opt/keystores/storkDemoKeys.jks");
                    sign.intializeCredentials("edupeps", "edupeps-cert", "edupeps",  "/opt/keystores/edupeps.jks");

                    
                    Signature signatureAssertion = (Signature) Configuration.getBuilderFactory().getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                            .buildObject(Signature.DEFAULT_ELEMENT_NAME);

                    Signature signatureResponse = (Signature) Configuration.getBuilderFactory().getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                            .buildObject(Signature.DEFAULT_ELEMENT_NAME);

                    signatureAssertion.setSigningCredential(sign.getSigningCredential());
                    signatureResponse.setSigningCredential(sign.getSigningCredential());

                    // This is also the default if a null SecurityConfiguration is specified
                    SecurityConfiguration secConfig = Configuration.getGlobalSecurityConfiguration();
                    // If null this would result in the default KeyInfoGenerator being used
                    String keyInfoGeneratorProfile = "XMLSignature";

                    try {
                        SecurityHelper.prepareSignatureParams(signatureAssertion, sign.getSigningCredential(), secConfig, null);
                        SecurityHelper.prepareSignatureParams(signatureResponse, sign.getSigningCredential(), secConfig, null);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (org.opensaml.xml.security.SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    assertionSign.setSignature(signatureAssertion);
                    responseSign.setSignature(signatureResponse);

                    try {
                        Configuration.getMarshallerFactory().getMarshaller(assertionSign).marshall(assertionSign);
                    } catch (MarshallingException e) {
                        e.printStackTrace();
                    }

                    try {
                        Signer.signObject(signatureAssertion);
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    }

                    try {
                        Configuration.getMarshallerFactory().getMarshaller(assertionSign).marshall(assertionSign);
                        Configuration.getMarshallerFactory().getMarshaller(responseSign).marshall(responseSign);
                    } catch (MarshallingException e) {
                        e.printStackTrace();
                    }

                    try {
                        Signer.signObject(signatureResponse);
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    }

                    ResponseMarshaller marshaller = new ResponseMarshaller();
                    Element plain = null;
                    try {
                        plain = marshaller.marshall(responseSign);
                    } catch (MarshallingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    String samlResponse = XMLHelper.nodeToString(plain);
                    logger.info("********************\n*\n***********\n:");

                    /**************************************************/ 

                    /*
                     * try { //Marshaller marshallerAssertion =
                     * Configuration.getMarshallerFactory
                     * ().getMarshaller(assertion);
                     * //marshallerAssertion.marshall(assertion); //Marshaller
                     * marshallerResponse =
                     * Configuration.getMarshallerFactory().
                     * getMarshaller(responseSAML); //Element
                     * edupepsResponseElement =
                     * marshallerResponse.marshall(responseSAML); //String xml =
                     * XMLHelper.nodeToString(edupepsResponseElement);
                     * 
                     * } catch (MarshallingException e) { // TODO Auto-generated
                     * catch block e.printStackTrace(); }
                     */
                    String xml = samlResponse;
                    logger.info("response-xml:\n" + xml);
                    String eduresponseEncoded = org.opensaml.xml.util.Base64.encodeBytes(xml.getBytes(),
                            org.opensaml.xml.util.Base64.DONT_BREAK_LINES);
                    //logger.info("response-encoded:\n" + eduresponseEncoded);

                    // Parameter saml response to form
                    out.println("<input type='hidden' name='SAMLRequest' value='" + eduresponseEncoded + "'>");

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
