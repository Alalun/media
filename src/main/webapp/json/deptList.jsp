<%@ page import="other.aakepi.bdfjfaackcpic.api.media.DeptSearch" %>
<%@ page import="other.aakepi.bdfjfaackcpic.util.JSONUtil" %>
<%@ page import="java.util.Enumeration" %>
<%@ page contentType="text/json;charset=UTF-8" language="java" %>
<%

  //初始化请求
  com.rkhd.platform.sdk.http.Request rkhdRequest = new com.rkhd.platform.sdk.http.Request();
  Enumeration paramNames = request.getParameterNames();
  while (paramNames.hasMoreElements()) {
    String paramName = (String) paramNames.nextElement();
    String[] paramValues = request.getParameterValues(paramName);
    rkhdRequest.putParameter(paramName,paramValues);
  }


  DeptSearch apiSearch = new DeptSearch();
  //返回的结果
  String json = apiSearch.execute(rkhdRequest,null,null);

  String newJson = JSONUtil.string2Json(json);

%>
{"status":"0","result":"<%= newJson%>"}