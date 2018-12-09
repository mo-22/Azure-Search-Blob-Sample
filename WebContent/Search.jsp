<%@page import="model.BlobFile"%>
<%@page import="servlets.SearchServlet"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Azure Search</title>
</head>
<body>
<body>
	<h2>Search TEST (BLOB)</h2>
	<form action="SearchServlet" method="POST">
		<input type="text" name="SearchQuery" size="100"> <input
			type="submit" value="Search" />
	</form>

	<%
		List<BlobFile> blobFileList = (List<BlobFile>) request.getAttribute("blobFileList");

		if (blobFileList != null) {
	%>
	<br />
	<table border="1">
		<tr>
			<td>name</td>
			<td>path</td>
		</tr>
		<%
			for (int i = 0; i < blobFileList.size(); i++) {
		%>
		<tr>
			<td><%=blobFileList.get(i).getName()%></td>
			<td><%=blobFileList.get(i).getPath()%></td>
		</tr>
		<%
			}
		%>
	</table>
	<%
		}
	%>
</body>

</html>