
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="../../common/header.jsp"%>
<%@ include file="../../common/menu.jsp"%>
<!--body wrapper start-->
<html>
<head>
<style>
.table th {
	text-align: center;
}
</style>
<script type="text/javascript">
	function lockuser(paramstoreId) {
		//alert(paramstoreId);
		document.getElementById("curUserId1").value = paramstoreId;

	}

	function activityuser(paramstoreId) {

		document.getElementById("curUserId2").value = paramstoreId;

	}

	function deluser(paramstoreId) {
		document.getElementById("curUserId").value = paramstoreId;

	}
	function agreement(paramstoreId) {
		document.getElementById("curUserId0").value = paramstoreId;

	}
	
	function refuse(paramstoreId) {
		document.getElementById("curUserId4").value = paramstoreId;

	}
	function detail(paramstoreId) {
		window.location.href="${pageContext.request.contextPath}/admin/user/merchantInfo?id="+paramstoreId;

	}
	function check() {
		var txt_phone = $.trim($("#phone").attr("value"));
		if (txt_phone.length > 15) {
			alert("查询依据不能超过15个字");
			return false;
		} else {
			return true;
		}
	}
</script>
</head>

<body>


	<form class="searchform"
		action="${pageContext.request.contextPath}/admin/user/merchant/search"
		method="GET" onsubmit="return check();">
		<input type="text" class="form-control" name="merchant_name"
			value="${merchant_name}" id="phone" placeholder="请输入商人名称" /> <input
			type="submit" class="form-control btn btn-success" value="查找" />
	</form>
	<div class="container-fluid">
		<div class="wrapper">
			<div class="row ">
				<div class="col-xs-12 col-md-12">
					<c:if test="${empty page.list}">
						<div style="color: #F66; text-align: center;">对不起，没有找到您想要的用户</div>
					</c:if>
					<c:if test="${not empty page.list}">
						<table class="table table-hover">
							<tr>
								<th>#</th>
								<th>用户名</th>
								<th>电话</th>
								<th>商城帐号</th>
								<th>联系人</th>
								<th>创建时间</th>
								<th>操作</th>
							</tr>


							<c:forEach items="${page.list}" var="list" varStatus="vs">

								<tr id="mer_${list.user_id}">
									<td align="center">${vs.index+1}</td>
									<td align="center">${list.userLogin}</td>
									<td align="center">${list.merchant_phone}</td>
									<td id="text_${list.user_id}" align="center">${list.merchant_account}</td>
									<td id="text_${list.user_id}" align="center">${list.service_man}</td>

									<td align="center"><fmt:formatDate
											value="${list.merchant_add_time}"
											pattern="yyyy-MM-dd HH:mm:ss" type="both" /></td>

									<c:if test="${list.user_type!='4'}">
										<td id="btn_${list.user_id}" align="center"><c:if
												test="${list.user_state=='2'}">
												<button class="btn btn-info" data-toggle="modal"
													data-target="#lockModal"
													onclick="lockuser('${list.user_id}');">锁定</button>	
																&nbsp;&nbsp;		<button class="btn btn-danger " data-toggle="modal"
													data-target="#deleteModal"
													onclick="deluser('${list.user_id}');">删除</button>
											</c:if> 
											<c:if test="${list.user_state=='0'}">
												<button class="btn btn-success" data-toggle="modal"
													data-target="#agreeModal"
													onclick="agreement('${list.user_id}');">同意</button>	
													&nbsp;&nbsp;		<button class="btn btn-danger " data-toggle="modal"
													data-target="#refuseModal"
													onclick="refuse('${list.user_id}');">拒绝</button>
													&nbsp;&nbsp;		<button class="btn btn-warning "
													onclick="detail('${list.user_id}');">详情</button>
											</c:if> 
											<c:if test="${list.user_state=='4'}">
												<button class="btn btn-success" data-toggle="modal"
													data-target="#agreeModal"
													onclick="agreement('${list.user_id}');">同意</button>	
													&nbsp;&nbsp;			
													<button class="btn btn-danger " data-toggle="modal"
													data-target="#deleteModal"
													onclick="deluser('${list.user_id}');">删除</button>
											</c:if> 
											<c:if test="${list.user_state=='1'}">
												<button class="btn btn-success" data-toggle="modal"
													data-target="#releaseModal"
													onclick="activityuser('${list.user_id}');">解锁</button>
													&nbsp;&nbsp;	 	<button class="btn btn-danger " data-toggle="modal"
													data-target="#deleteModal"
													onclick="deluser('${list.user_id}');">删除</button>
											</c:if></td>
									</c:if>
									<c:if test="${list.user_type=='4'}">
										<td></td>
									</c:if>
								</tr>
							</c:forEach>
						</table>
					</c:if>
					<c:if test="${page.pages>1}">
						<!-- 分页 -->
						<jsp:include page="../../common/pager.jsp">
							<jsp:param value="phone" name="paramKey" />
							<jsp:param value="${store_phone}" name="paramVal" />
						</jsp:include>
					</c:if>
					<c:choose>
						<c:when test="${page.pages>1&&page.pageSize>3}">
							<%@ include file="../../common/footer.jsp"%>
						</c:when>
						<c:otherwise>
							<div style="position: fixed; bottom: 0px; width: 80%;">
								<%@ include file="../../common/footer.jsp"%>
							</div>
						</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>
	</div>
	<!--删除模态框  -->
	<div class="modal fade" id="deleteModal" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title" id="myModalLabel">删除确认</h4>
				</div>

				<form class="form-horizontal col-sm-offset-2"
					action="${pageContext.request.contextPath}/admin/user/merchant/deluser"
					method="get" enctype="multipart/form-data">
					<input id="curUserId" name="curUserId" type="hidden">
					<div class="modal-body">是否确认删除该用户吗？</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
						<button type="submit" class="btn btn-primary">确认删除</button>
					</div>
				</form>
			</div>
		</div>
	</div>
	<!--锁定模态框  -->
	<div class="modal fade" id="lockModal" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title" id="myModalLabel">锁定确认</h4>
				</div>

				<form class="form-horizontal col-sm-offset-2"
					action="${pageContext.request.contextPath}/admin/user/merchant/lockuser"
					method="get" enctype="multipart/form-data">
					<input id="curUserId1" name="curUserId" type="hidden">
					<div class="modal-body">是否确认锁定该改用户吗？</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
						<button type="submit" class="btn btn-primary">确认锁定</button>
					</div>
				</form>
			</div>
		</div>
	</div>
	<!--解锁模态框  -->
	<div class="modal fade" id="releaseModal" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title" id="myModalLabel">解锁确认</h4>
				</div>

				<form class="form-horizontal col-sm-offset-2"
					action="${pageContext.request.contextPath}/admin/user/merchant/activityuser"
					method="get" enctype="multipart/form-data">
					<input id="curUserId2" name="curUserId" type="hidden">
					<div class="modal-body">是否确认解锁该改用户吗？</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
						<button type="submit" class="btn btn-primary">确认解锁</button>
					</div>
				</form>
			</div>
		</div>
	</div>
	<!--同意模态框  -->
	<div class="modal fade" id="agreeModal" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title" id="myModalLabel">同意申请</h4>
				</div>

				<form class="form-horizontal col-sm-offset-2"
					action="${pageContext.request.contextPath}/admin/user/merchant/agreement"
					method="get" enctype="multipart/form-data">
					<input id="curUserId0" name="curUserId" type="hidden">
					<div class="modal-body">是否同意该商家入驻？</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
						<button type="submit" class="btn btn-primary">同意</button>
					</div>
				</form>
			</div>
		</div>
	</div>
	
	<!--同意模态框  -->
	<div class="modal fade" id="refuseModal" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title" id="myModalLabel">拒绝申请</h4>
				</div>

				<form class="form-horizontal col-sm-offset-2"
					action="${pageContext.request.contextPath}/admin/user/merchant/refuse"
					method="get" enctype="multipart/form-data">
					<input id="curUserId4" name="curUserId" type="hidden">
					<div class="modal-body">是否同意该商家入驻？</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
						<button type="submit" class="btn btn-primary">拒绝</button>
					</div>
				</form>
			</div>
		</div>
	</div>

</body>
</html>


