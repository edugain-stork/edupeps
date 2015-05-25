<%@page import="java.util.Date"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<% String lang=request.getParameter("lang"); %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <% if (lang == null)
            lang = "en";
       if (lang.equals("en")) { %>
            <title>Country Selection Form</title>
    <% } else { %>
            <title>Formulario de selecci&oacute;n de nacionalidad</title>
    <% } %>
  </head>
  <!--<body onload="document.createElement('form').submit.call(document.getElementById('myForm'))"> -->
  <body style="background-image:url(img/background.png); background-size:scale; background-repeat: no-repeat;background-position: center top">
    <% if (lang.equals("en")) { %>
    <h2>Select your country</h2>
    <% } else { %>
    <h2>Seleccione su pa&iacute;s</h2>
    <% } %>
    <br>
    <form id="myForm" name="myForm" action='http://edupeps.inf.um.es:8080/edupeps/EduGAIN2StorkProxy' method='post'>
      <input type='hidden' name='lang' value='<%= lang %>'>
      <input type='hidden' name='SAMLRequest' value='<%= request.getParameter("SAMLRequest") %>'>
      <center>
	<input type="radio" name="CountryCode" value="ES"><img src="img/Flags/Spain.png" width=100><br/>
	<!-- <input type="radio" name="CountryCode" value="IT"><img src="img/Flags/Italy.png" width=100><br/> -->
	<!-- <input type="radio" name="CountryCode" value="SE"><img src="img/Flags/Sweden.png" width=100><br/> -->
	<!-- <input type="radio" name="CountryCode" value="PT"><img src="img/Flags/Portugal.png" width=100><br/> -->
	<input type="radio" name="CountryCode" value="GR"><img src="img/Flags/Greece.png" width=100><br/>
	<input type="radio" name="CountryCode" value="REMOTE" checked="checked"><img src="img/eduGAINLogo.png" width=100 style="background-color:gray;">REMOTE eduGAIN Test<br/>
	<% if (lang.equals("en")) { %>
	<button type="submit" value="Accept" method="post"><img src="img/send.png" width=25 border=3></button>
	Send Form
	<% } else { %>
	<button type="submit" value="Aceptar" method="post"><img src="img/send.png" width=25 border=3></button>
	Enviar Formulario
	<% } %>
      </center>
    </form>

  </body>
</html>
