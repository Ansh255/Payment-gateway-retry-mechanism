<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@taglib prefix="f" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <link href="css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<f:form action="processing-payment" method="post" modelAttribute="StudentOrderVO">
    <div class="container">
        <table class="table">
            <tr>
                <td>Name:</td>
                <td><f:input path="name" id="name" class="form-control"/></td>
            </tr>
            <tr>
                <td>Email:</td>
                <td><f:input path="email" id="email" class="form-control"/></td>
            </tr>
            <tr>
                <td>Phone Number:</td>
                <td><f:input path="phoneNumber" id="phoneNumber" class="form-control"/></td>
            </tr>
            <tr>
                <td>Select Course:</td>
                <td>
                    <f:select path="course" id="course" class="form-select">
                        <f:option value="python">PYTHON</f:option>
                        <f:option value="java">JAVA</f:option>
                        <f:option value="ml">Machine Learning</f:option>
                    </f:select>
                </td>
            </tr>
            <tr>
                <td>Amount:</td>
                <td><f:input path="amount" id="amount" class="form-control" required="required"/></td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <f:button class="btn btn-primary" id="button" name="button">Proceed To Pay</f:button>
                </td>
            </tr>
        </table>
    </div>
    <script src="js/checkout.js"></script>
    <script src="js/bootstrap.bundle.min.js"></script>
</f:form>
</body>
</html>
