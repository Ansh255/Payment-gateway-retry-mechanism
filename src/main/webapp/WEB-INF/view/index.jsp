<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <link href="css/bootstrap.min.css"
          rel="stylesheet">
</head>
<body>
<form action="processing-payment" method="post">
    <div class="container">
        <table class="table">
            <tr>
                <td>Name:</td>
                <td><input type="text" name="name" id="name" class="form-control"></td>
            </tr>
            <tr>
                <td>Email</td>
                <td><input type="email" name="email" id="email" class="form-control"></td>
            </tr>
            <tr>
                <td>Phone Number</td>
                <td><input type="number" name="phoneNumber" id="phoneNumber" class="form-control">
                </td>
            </tr>
            <tr>
                <td>Select Course</td>
                <td><select class="form-select" name="course" id="course">
                    <option value="python">PYTHON</option>
                    <option value="java">JAVA</option>
                    <option value="ml">Machine Learning</option>
                </select></td>
            </tr>
            <tr>
                <td>Amount</td>
                <td><input type="number" name="amount" id="amount" class="form-control" required>
                </td>
            </tr>

            <tr>
                <td></td>
                <td>
                    <button class="btn btn-primary" id="button" name="button">Proceed To Pay
                    </button>
                </td>
            </tr>
        </table>
    </div>
    <script src="js/checkout.js"></script>
    <script src="js/bootstrap.bundle.min.js"></script>

</form>
</body>
</html>
