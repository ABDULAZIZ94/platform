<%@ page import="org.exoplatform.platform.welcomescreens.TrialService" %>
<%
    int rday = TrialService.getNbDaysBeforeExpiration();
    boolean outdated = TrialService.isOutdated();
    String css="backNotOutdated";
    String label1="You have";
    String label2="days left in your evaluation";
    String productCode=TrialService.getProductCode();
    if (outdated)  {
        css="backOutdated";
        label1= "Your evaluation has expired"  ;
        label2= "days ago";
        rday = TrialService.getNbDaysAfterExpiration();
    }

%>
<!DOCTYPE html
PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>Welcome to eXo Platform</title>
    <link rel="shortcut icon" type="image/x-icon"  href="/welcome-screens/favicon.ico" />
    <link rel="stylesheet" type="text/css" href="/welcome-screens/css/Stylesheet.css"/>
    <script type='text/javascript'>
        function submitValidationKey() {
            document.getElementById('submitActionButton').onclick='';
            document.unlockForm.submit();
        }
    </script>
</head>
<body>
<div class="UIBanner">
    <div class="BannerContent ClearFix">
        <h1 class="BannerTitle FR"> <%=label1%> <span class="YellowColor"> <%=rday%> </span><%=label2%></h1>
        <img src="/welcome-screens/css/background/Logo.png" alt="Evaluation"/>
    </div>
</div>
<div class="UIContent">
    <h2 class="CenterTitle">You must own a valid subscription in order to unlock this eXo-Platform</h2>

    <div class="Container ClearFix">
        <span class="TextContainer">Pickup your favourite <a class="" href="<%=TrialService.getRegistrationFormUrl()%>" target="_blank">subscription</a> plan</span>
        <span class="TriangleItem OrangeIcon"></span>
    </div>

    <div class="Container ClearFix">

        <span class="TextContainer">Grab your product code and request an unlock key</span>
        <br>
        <span>Product Code</span> <input type="text" class="Text"  DISABLED="disabled" placeholder="<%=TrialService.getProductCode() %>">  <a class="Botton BlueRect" href="<%=TrialService.getRegistrationFormUrl()%>">Request a Key</a>
        <span class="TriangleItem BlueIcon"></span>
    </div>

    <div class="Container ClearFix">
        <span>

            <span class="TextContainer">Enter the unlock key below to unlock the product <br> </span>
            <div class="FormContainer">
                <form action="/welcome-screens/UnlockServlet" method="post" name="unlockForm">
                    <table>
                        <tr>
                            <td>
                                <label class="TextForm" id="hashMD5">Unlock Key</label>
                            </td>
                            <td>
                                <input class="Text" type="text" name="hashMD5" id="hashMD5">
                            </td>
                            <td>
                                <input type="submit" class="FormSubmit BlueFormRect" value="Unlock Product">
                            </td>
                        </tr>
                        <% if(request.getAttribute("errorMessage") != null && !request.getAttribute("errorMessage").toString().isEmpty()) {%>
                        <tr>
                            <td colspan="3" class="Red">
                                <%=request.getAttribute("errorMessage").toString() %>
                            </td>
                        </tr>
                        <% }%>
                    </table>
                </form>
            </div>
        </span>
        <span class="TriangleItem GreenIcon"></span>
    </div>
</div>
</body>
</html>