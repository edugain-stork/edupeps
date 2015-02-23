<%@page import="java.util.Date"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>demoSP_aula</title>
</head>
<body onload="document.createElement('form').submit.call(document.getElementById('myForm'))">
<h2>Hi There!!</h2>
<br>
<h3>Date=<%= new Date() %>
</h3>

<form id="myForm" name="myForm" action='https://stork2.um.es/UMU2StorkProxy/UMU2StorkProxy' method='post'>
<input type='hidden' name='ap' value='https://stork2.um.es/UMUStorkProxy/UMU2StorkProxy'>
<input type='hidden' name='ap_ms_input' value='LAP'>
<input type='hidden' name='apUrl' value='https://stork2.um.es/UMU2StorkProxy/UMU2StorkProxy'>
<input type='hidden' name='returnUrl' value='https://stork2.um.es/UMU2StorkProxy/ReturnPage'>
<input type='hidden' name='eidentifier' value='ES/ES/02909146Z'>
<input type='hidden' name='eIdentifier' value='eIdentifier'>
<input type='hidden' name='eIdentifierType' value='true'>
<input type='hidden' name='isStudent' value='isStudent'>
<input type='hidden' name='isStudentType' value='true'>
<input type='hidden' name='isTeacherOf' value='isTeacherOf'>
<input type='hidden' name='isTeacherOfType' value='true'>
<input type='hidden' name='isCourseCoordinator' value='isCourseCoordinator'>
<input type='hidden' name='isCourseCoordinatorType' value='true'>
<input type='hidden' name='eMail' value='eMail'>
<input type='hidden' name='eMailType' value='true'>
</form>

</body>
</html>