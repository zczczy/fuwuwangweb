package com.platform.web.controller.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.platform.common.contants.Constants;
import com.platform.common.utils.DateUtil;
import com.platform.common.utils.Md5;
import com.platform.common.utils.New_token;
import com.platform.common.utils.Tools;
import com.platform.common.utils.UUIDUtil;
import com.platform.common.utils.UploadUtil;
import com.platform.entity.User;
import com.platform.entity.User_token;
import com.platform.mapper.UserMapper;
import com.platform.service.UserService;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/*import com.sun.mail.util.MailLogger;*/
@Controller
@RequestMapping("/app/")
public class AppUserController {

	@Autowired
	private UserService userService;

	@Autowired
	private UserMapper mapper;

	String path = "http://124.254.56.58:8007/api/";

	OkHttpClient client = new OkHttpClient();

	public static final MediaType JSONTPYE = MediaType.parse("application/json; charset=utf-8");

	Gson gson = new Gson();

	/**
	 * 功能：用户注册
	 * 
	 * @param userLogin
	 *            用户名
	 * @param passWord
	 *            密码
	 * @param passWordConfirm
	 *            确认密码 以下二个参数 根据不同用户类型传一个
	 * @param zy
	 *            专员（会员注册传）
	 * @param email
	 *            邮箱（普通用户传）
	 * @param type
	 *            用户类别 参数 3.普通用户 4.vip会员
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "register", method = RequestMethod.POST, consumes = { "application/json" })
	@ResponseBody
	public BaseModelJson<String> register(@RequestBody User user) {
		BaseModelJson<String> result = new BaseModelJson<>();
		if (user.getUserLogin() == null || user.getUserLogin().isEmpty()) {
			result.Error = "用户名不能为空";
			return result;
		}
		if (user.getPassWord() == null || user.getPassWord().isEmpty()) {
			result.Error = "密码不能为空";
			return result;
		}
		if (user.getPassWordConfirm() == null || user.getPassWordConfirm().isEmpty()) {
			result.Error = "确认密码不能为空";
			return result;
		}
		if (user.getUser_type() == null || user.getUser_type().isEmpty()) {
			result.Error = "用户类型不能为空";
			return result;
		}
		if (!user.getPassWordConfirm().equals(user.getPassWord())) {
			result.Error = "两次密码不一致";
			return result;
		}
		if ("3".equals(user.getUser_type()) && (user.getUser_email() == null || user.getUser_email().isEmpty())) {
			result.Error = "邮箱不能为空";
			return result;
		}
		if ("4".equals(user.getUser_type()) && (user.getZy() == null || user.getZy().isEmpty())) {
			result.Error = "服务专员不能为空";
			return result;
		}
		if (userService.chechUserIsExist(user.getUserLogin()) > 0) {
			result.Error = "您输入的用户昵称已经存在";
			return result;
		}
		if ("3".equals(user.getUser_type()) && userService.chechUserEmailIsExist(user.getUser_email()) > 0) {
			result.Error = "您输入的邮箱已经存在";
			return result;
		}
		BaseModel bm = checkIsExist(user.getUserLogin());
		if (!bm.Successful) {
			result.Error = bm.Error;
			return result;
		}
		if ("4".equals(user.getUser_type())) {
			BaseModelJson<String> bmj = register(user.getUserLogin(), user.getPassWord(), user.getPassWordConfirm(),
					user.getZy());
			if (bmj.Successful) {
				result.Data = bmj.Data;
				result.Successful = true;
			} else {
				result.Error = bmj.Error;
			}
		}
		if ("3".equals(user.getUser_type())) {
			user.setUser_id(UUIDUtil.getRandom32PK()); // id
			user.setPassWord(Md5.getVal_UTF8(user.getPassWord())); // 密码md5 加密
			user.setUser_state(Constants.USER_ACTIVE); // 活跃
			user.setUser_type(Constants.USER_); // 普通用户
			userService.adduser(user);
			result.Successful = true;
			result.Data = "注册成功";
		}
		return result;
	}

	/**
	 * 功能：用户登录
	 * 
	 * @param userLogin
	 *            用户名
	 * @param passWord
	 *            密码
	 * @param type
	 *            用户类别 参数 3.普通用户 4.vip会员
	 * @return
	 */
	@RequestMapping(value = "login", method = RequestMethod.GET)
	@ResponseBody
	public BaseModelJson<User> login(String userLogin, String passWord, String type) {
		BaseModelJson<User> result = new BaseModelJson<User>();
		if (null == userLogin || null == passWord || null == type) {
			result.Error = "登录信息不全";
			return result;
		}
		if ("4".equals(type)) {
			BaseModelJson<String> bmj = sigIn(userLogin, passWord);
			if (bmj.Successful) {
				User uu = userService.findUser(userLogin);
				if (uu == null) {
					uu = new User();
					uu.setUser_id(UUIDUtil.getRandom32PK()); // id
					uu.setUserLogin(userLogin); // 帐号
					uu.setPassWord(Md5.getVal_UTF8(passWord)); // 密码md5
					uu.setUser_type(Constants.USER_VIP); // 会员用户
					uu.setUser_state(Constants.USER_ACTIVE); // 活跃
					mapper.userrigester_user(uu); // 注册的会员插入到
					BaseModelJson<FwwUser> fww = getFwwUserInfo(bmj.Data);
					if (fww.Successful) {
						uu.setRealName(fww.Data.getM_realrName());
						uu.setIdCard(fww.Data.getM_idCard());
						uu.setDq("--");
						uu.setUser_email(fww.Data.getM_Email());
					}
					uu.setUser_img(".jpg");
					mapper.userrigester_userinfo(uu);
					uu.setToken(bmj.Data);
					userService.add_token(uu); // 插入token
				} else {
					User_token lTokens = userService.select_UsertokenByUserid(uu.getUser_id());
					lTokens.setToken(bmj.Data);
					userService.update_token(lTokens);
				}
				result.Data = uu;
				result.Data.setToken(bmj.Data);
				result.Successful = true;
			} else {
				result.Error = bmj.Error;
			}
			return result;
		}
		if ("3".equals(type)) {
			Map<String, String> map = new HashMap<>();
			map.put("userLogin", userLogin);
			map.put("passWord", Md5.getVal_UTF8(passWord));
			User uu = userService.login(map);
			if (uu == null) {
				result.Error = "用户名或者密码错误";
			} else if ("2".equals(uu.getUser_state())) {
				result.Error = "用户名已被锁定";
			} else {
				String txt = uu.getUserLogin() + uu.getPassWord() + DateUtil.getDays();
				String token = New_token.newToken(txt);
				uu.setToken(uu.getToken());
				User_token lTokens = userService.select_UsertokenByUserid(uu.getUser_id());
				if (lTokens != null) {
					lTokens.setToken(token);
					userService.update_token(lTokens);
				} else {
					userService.add_token(uu); // 插入token
				}
				result.Successful = true;
				result.Data = uu;
			}
		}
		result.Error = "参数错误";
		return result;
	}

	/**
	 * 获取用户信息
	 * 
	 * @param token
	 * @return
	 */
	@RequestMapping(value = "getUserInformation", method = RequestMethod.GET)
	@ResponseBody
	public BaseModelJson<User> getUserInformation(@RequestHeader String token) {
		BaseModelJson<User> result = new BaseModelJson<>();

		if (null == token || "".equals(token)) {
			result.Error = "没有权限访问";
			return result;
		}
		User user = userService.getUserInforByToken(token);
		if (null == user) {
			result.Error = "该账号已在其他客户端登录，请重新登陆";
			return result;
		}
		if ("2".equals(user.getUser_state())) {
			result.Error = "该账号已被锁定，请联系客服";
			return result;
		}
		result.Data = user;
		result.Successful = true;
		return result;
	}

	/**
	 * 修改密码
	 * 
	 * @param token
	 * @param user
	 *            ---- passWord:旧密码 passWordConfirm:新密码
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "changePassword", method = RequestMethod.POST)
	@ResponseBody
	public BaseModelJson<String> changePassword(@RequestHeader String token, @RequestBody User user) throws Exception {
		BaseModelJson<String> result = new BaseModelJson<>();

		if (token == null) {
			result.Error = "没有权限访问";
			return result;
		}
		if (user == null) {
			result.Error = "参数错误";
			return result;
		}
		if (user.getPassWord() == null) {
			result.Error = "旧密码不能为空";
			return result;
		}
		if (user.getPassWordConfirm() == null) {
			result.Error = "新密码不能为空";
			return result;
		}
		User tokenUser = userService.getUserInforByToken(token);
		if (tokenUser == null) {
			result.Error = "身份验证失败";
			return result;
		}
		if (Md5.getVal_UTF8(user.getPassWord()).equals(tokenUser.getPassWord())) {
			tokenUser.setPassWord(Md5.getVal_UTF8(user.getPassWordConfirm()));
			userService.updatepass(tokenUser);
			tokenUser.setResults("成功");
			result.Successful = true;
			result.Data = "修改成功";
		} else {
			result.Error = "旧密码不正确";
		}
		return result;
	}

	/**
	 * updateEmail 功能：修改app普通用户邮箱
	 * 
	 * @param token
	 *            令牌
	 * @param userLogin
	 *            用户名
	 * @param user_email
	 *            新邮箱
	 * @param passWord
	 *            密码
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "updateEmail", method = RequestMethod.POST)
	@ResponseBody
	public BaseModelJson<String> updateEmail(@RequestHeader String token, @RequestBody User user) throws Exception {
		BaseModelJson<String> result = new BaseModelJson<>();
		if (token == null) {
			result.Error = "没有权限访问";
			return result;
		}
		if (user == null) {
			result.Error = "参数错误";
			return result;
		}
		if (user.getUser_email() == null || user.getUser_email().isEmpty()) {
			result.Error = "新邮箱不能为空";
			return result;
		}
		if (user.getPassWord() == null || user.getPassWord().isEmpty()) {
			result.Error = "密码不能为空";
			return result;
		}
		User u = userService.getUserInforByToken(token);
		if (null == u) {
			result.Error = "该账号已在其他客户端登录，请重新登陆";
			return result;
		}
		if ("2".equals(user.getUser_state())) {
			result.Error = "该账号已被锁定，请联系客服";
			return result;
		}
		if (u.getPassWord().equals(Md5.getVal_UTF8(user.getPassWord()))) {
			u.setUser_email(user.getUser_email());
			userService.updateEmail(u);
			result.Successful = true;
			result.Data = "修改成功";
		}
		return result;
	}

	/**
	 * 发验证码
	 * 
	 * @param user
	 *            --- userLogin 和 user_email
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "sendCode", method = RequestMethod.POST)
	public BaseModelJson<String> sendCode(@RequestBody User user) {
		BaseModelJson<String> result = new BaseModelJson<>();
		if (user == null) {
			result.Error = "参数错误";
			return result;
		}
		if (user.getUserLogin() == null || user.getUserLogin().isEmpty()) {
			result.Error = "用户名不能为空";
			return result;
		}
		if (user.getUser_email() == null || user.getUser_email().isEmpty()) {
			result.Error = "邮箱不能为空";
			return result;
		}
		User u = userService.getUserInforByUserNameAndEmail(user);
		if (u == null) {
			result.Error = "用户名和邮箱不正确";
			return result;
		}
		int yzm = Tools.getRandomNum();
		String sender = "fuwuwang86@126.com";// fuwuwangapp@163.com
		//// bjfww86@126.com
		String subject = "服务网找回密码";
		String userName = sender;
		String password = "86fuwuwang";
		Properties props = new Properties();
		props.put("mail.debug", "true");
		props.put("mail.smtp.host", "smtp.126.com");
		props.put("mail.smtp.port", "25");
		props.put("mail.smtp.auth", "true");
		// 授权用户和密码可以在这设置，也可以在发送时直接设置
		Session session1 = Session.getDefaultInstance(props, null);
		Message message = new MimeMessage(session1);
		try {
			Address sendAddress = new InternetAddress(sender);
			message.setFrom(sendAddress);
			// 支持发送多个收件人
			// String [] recipients = recipient.split(";");
			// Address[] recipientAddress = new Address[recipients.length];
			// for (int i = 0; i < recipients.length; i++) {
			// recipientAddress[i] = new InternetAddress(recipients[i]);
			// }
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getUser_email()));
			message.setSubject(subject);
			// 以html发送
			BodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent("<h5>" + "尊敬的用户，您的服务网的验证码为：" + yzm + "<h5>", "text/html; charset=utf-8");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(bodyPart);
			// 单纯发送文本的用setText即可
			// message.setText("服务网找回密码的验证码：1234");
			message.setContent(multipart);
			Transport.send(message, userName, password);
			User_token YZMuser = new User_token();
			YZMuser.setUser_id(u.getUser_id());
			YZMuser.setYZM(String.valueOf(yzm));
			userService.update_YZM(YZMuser);
			result.Successful = true;
			result.Data = "发送成功";
		} catch (MessagingException e) {
			e.printStackTrace();
			result.Error = "发送邮件失败";
			return result;
		}
		return result;
	}

	/**
	 * 找回密码
	 * 
	 * @param model
	 *            应该传的值为下面
	 * 
	 * @param userLogin
	 *            用户名
	 * @param code
	 *            验证码
	 * @param passWord
	 *            密码
	 * @param repassWord
	 *            确认密码
	 * @return
	 */
	@RequestMapping(value = "resetPassword", method = RequestMethod.POST)
	@ResponseBody
	public BaseModelJson<String> resetPassword(@RequestBody BodyModel model) {
		BaseModelJson<String> result = new BaseModelJson<>();
		if (model == null) {
			result.Error = "参数错误";
			return result;
		}
		if (model.getUserLogin() == null || model.getUserLogin().isEmpty()) {
			result.Error = "帐号不能为空";
			return result;
		}
		if (model.getPassword() == null || model.getPassword().isEmpty()) {
			result.Error = "密码不能为空";
			return result;
		}
		if (model.getConfirmPassword() == null || model.getConfirmPassword().isEmpty()) {
			result.Error = "确认密码不能为空";
			return result;
		}
		if (model.getCode() == null || model.getCode().isEmpty()) {
			result.Error = "验证码不能为空";
			return result;
		}
		if (!model.getConfirmPassword().equals(model.getPassword())) {
			result.Error = "两次密码输入不一致不能为空";
			return result;
		}
		Map<String, String> map = new HashMap<>();
		map.put("userLogin", model.getUserLogin());
		map.put("code", model.getCode());
		User user = userService.getUserInforByUserNameAndCode(map);
		if (user == null) {
			result.Error = "验证码错误";
			return result;
		}
		try {
			user.setPassWord(Md5.getVal_UTF8(model.getPassword()));
			userService.updatepass(user);
			result.Successful = true;
			result.Data = "密码修改成功";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result.Data = "服务器繁忙";
		}
		return result;
	}

	/**
	 * sendYZM 功能：找回密码第一步，发送邮件
	 * 
	 * @param userLogin
	 *            用户名
	 * @param email
	 *            邮件地址
	 * @return
	 */
	@RequestMapping(value = "sendYZM", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendYZM(@RequestBody String userLogin, @RequestBody String email) {

		Map<String, Object> map = new HashMap<String, Object>();
		User user = null;
		Boolean flag = false;
		int RandomNum = Tools.getRandomNum();
		// session.setAttribute("user_session", session.getId());
		// session.setAttribute("User_RandomNum", RandomNum);
		// map.put("Data", session.getId());
		if (null != userLogin && !("").equals(userLogin)) {
			user = userService.usermsg(userLogin);
			if (null != user) {
				if (!user.getUser_type().equals("3")) {
					flag = false;
					map.put("Successful", false);
					map.put("Error", "该用户暂时不能找回密码！");
					return map;
				}
				User_token lTokens = userService.select_UsertokenByUserid(user.getUser_id());
				User_token YZMuser = new User_token();
				YZMuser.setUser_id(user.getUser_id());
				YZMuser.setYZM(RandomNum + "");
				if (lTokens == null) {

					userService.add_YZM(YZMuser);
				} else {
					userService.update_YZM(YZMuser);
				}

			} else {
				map.put("Successful", false);
				map.put("Error", "用户不存在！");
				return map;
			}

		} else {
			flag = false;
		}
		/*
		 * if (null != realName && !("").equals(realName)) { if
		 * (realName.equals(user.getRealName())) { flag = true; } else { flag =
		 * false; } } if (null != idCard && !("").equals(idCard)) { if
		 * (idCard.equals(user.getIdCard())) { flag = true; } else { flag =
		 * false; } }
		 */
		if (null != email && !("").equals(email)) {
			if (email.equals(user.getUser_email())) {
				flag = true;
			} else {
				flag = false;
			}
		}
		if (flag) {
			// String recipient = request.getParameter("recipient");
			String sender = "j349388188@126.com";// fuwuwangapp@163.com
			//// bjfww86@126.com
			String subject = "服务网找回密码";// request.getParameter("subject");
			// String content = request.getParameter("content");
			// double size = recipient.length() + sender.length() +
			// subject.length() + content.length();
			String userName = sender;
			String password = "lijiawei";
			// qapqmyapxwgsbxfe
			// verxhwgnuqjctihu
			Properties props = new Properties();
			props.put("mail.debug", "true");
			props.put("mail.smtp.host", "smtp.126.com");
			props.put("mail.smtp.port", "25");
			props.put("mail.smtp.auth", "true");
			// 授权用户和密码可以在这设置，也可以在发送时直接设置
			Session session1 = Session.getDefaultInstance(props, null);
			Message message = new MimeMessage(session1);
			try {
				Address sendAddress = new InternetAddress(sender);
				message.setFrom(sendAddress);
				// 支持发送多个收件人
				// String [] recipients = recipient.split(";");
				// Address[] recipientAddress = new Address[recipients.length];
				// for (int i = 0; i < recipients.length; i++) {
				// recipientAddress[i] = new InternetAddress(recipients[i]);
				// }

				message.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getUser_email()));
				message.setSubject(subject);
				// 以html发送
				BodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent("<h5>" + "尊敬的用户，您的服务网的验证码为：" + RandomNum + "<h5>", "text/html; charset=utf-8");
				Multipart multipart = new MimeMultipart();
				multipart.addBodyPart(bodyPart);
				// 单纯发送文本的用setText即可
				// message.setText("服务网找回密码的验证码：1234");
				message.setContent(multipart);
				Transport.send(message, userName, password);
				map.put("Successful", true);
				map.put("Error", "");
				return map;
			} catch (MessagingException e) {
				e.printStackTrace();
				map.put("Successful", false);
				map.put("Error", "发送邮件失败");
				return map;
			}

		} else {
			map.put("Successful", false);
			map.put("Error", "所填信息有误,请重新填写！");
			return map;

		}
	}

	/**
	 * ResetPassword 功能：找回密码第二步
	 * 
	 * @param userLogin
	 *            用户名
	 * @param YZM
	 *            验证码
	 * @param passWord
	 *            密码
	 * @param repassWord
	 *            确认密码
	 * @return
	 */
	@RequestMapping(value = "ResetPassword", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> ResetPassword(@RequestBody String YZM, @RequestBody String passWord,
			@RequestBody String repassWord, @RequestBody String userLogin) {
		Map<String, Object> map = new HashMap<String, Object>();
		// String session_id = (String) session.getAttribute("user_session");
		// map.put("Data", null);
		User user = new User();
		System.out.println(YZM);
		boolean flag = false;
		if (null != userLogin && !("").equals(userLogin) && null != passWord && !("").equals(passWord)
				&& null != repassWord && !("").equals(repassWord) && null != YZM) {
			user = userService.usermsg(userLogin);
			if (null == user) {
				flag = false;
				map.put("Successful", false);
				map.put("Error", "用户信息不匹配,密码重置失败");
				System.out.println("用户信息不匹配,密码重置失败");
				return map;
			}
			System.out.println("找到用户信息根据用户名");
		} else {
			flag = false;
			map.put("Successful", false);
			map.put("Error", "用户信息不匹配,密码重置失败");
			System.out.println("用户信息不匹配,密码重置失败");
			return map;
		}
		if (!passWord.equals(repassWord)) {
			flag = false;
			map.put("Successful", false);
			map.put("Error", "两次密码不一致,重置密码失败");
			System.out.println("两次密码不一致,重置密码失败");
			return map;
		} else {
			flag = true;
		}
		/*
		 * if (session_id.equals(String.valueOf(session.getId()))) { flag =
		 * true; } else { flag = false; map.put("Successful", false);
		 * map.put("Error", "用户信息不匹配,密码重置失败"); System.out.println(
		 * "session_id 匹配"); return map; }
		 */

		String verify_YZM = userService.select_YZM(user.getUser_id()).getYZM();

		if ((YZM).equals(verify_YZM)) {
			flag = true;
			System.out.println("验证码 匹配");
		} else {
			flag = false;
			map.put("Successful", false);
			map.put("Error", "验证码错误,密码重置失败");
			System.out.println("验证码不 匹配");
			System.out.println("验证码不 匹配");
			return map;
		}
		if (flag) {
			User user1 = new User();
			user1.setUserLogin(user.getUserLogin());
			user1.setPassWord(Md5.getVal_UTF8(passWord));
			try {
				map.put("Successful", true);
				map.put("Error", "密码重置成功");
				userService.updatepass(user1);
				System.out.println("更新密码成功");
				User_token userToken = new User_token();
				userToken.setUser_id(user.getUser_id());
				userToken.setYZM("aaaaaa");
				userService.update_YZM(userToken);
				return map;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				map.put("Successful", false);
				map.put("Error", "信息不匹配,密码重置失败");
				System.out.println("信息不匹配,密码重置失败");
				return map;
			}
		} else {
			map.put("Successful", false);
			map.put("Error", "信息不匹配,密码重置失败");
			System.out.println("信息不匹配,密码重置失败");
			return map;
		}

	}

	/**
	 * uploadAvatar 功能：上传头像
	 * 
	 * @param file
	 *            图片文件
	 * @param token
	 *            令牌
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "uploadAvatar", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> uploadAvatar(@RequestBody MultipartFile file, @RequestHeader String token,
			HttpSession session) throws IOException {

		System.out.println("上传头像...............");
		Map<String, Object> map = new HashMap<String, Object>();
		System.out.println(" 得到的token ： " + token);

		User agui_token1 = userService.select_token(token);

		if (null == agui_token1) {

			map.put("Successful", false);
			map.put("Data", "");
			map.put("Error", "该账号已在其他客户端登录，请重新登陆！");
			return map;
		}
		User agui_token2 = userService.finduserById(agui_token1.getUser_id());

		if (null == token || null == file) {
			map.put("Successful", false);
			map.put("Error", "");
			map.put("Data", "上传信息不全");
			return map;
		}

		String type = file.getOriginalFilename().indexOf(".") != -1 ? file.getOriginalFilename()
				.substring(file.getOriginalFilename().lastIndexOf("."), file.getOriginalFilename().length()) : null;
		System.out.println("未处理文件后缀" + type);

		type = type.toLowerCase();
		System.out.println("乙处理文件后缀" + type);
		if (type.equals(".jpg") || type.equals(".JPG ") || type.equals(".jpeg") || type.equals(".png")) {

			User user = userService.findUser(agui_token2.getUserLogin());
			// 存放路径
			String filepath = session.getServletContext().getRealPath(Constants.UPLOAD_USER_IMG_PATH);
			String id = user.getUser_id();
			// 存放到指定路径
			UploadUtil.saveFile(file, filepath, id);

			UploadUtil.img_01(file, filepath, id + "-1", id);
			System.out.println("图片1 完事");
			UploadUtil.img_02(file, filepath, id + "-2", id);
			System.out.println("图片二 完事");
			map.put("Successful", true);
			map.put("Data", "完成");
			map.put("Error", "");
			User u123 = new User();
			u123.setUser_id(id);
			u123.setUser_img(type);
			userService.updateuser_img(u123);
			return map;

		} else {

			map.put("Data", "");
			map.put("Successful", false);
			map.put("Error", "图片格式不对");
		}

		return map;
	}

	/** 验证会员帐号是否存在 */
	public static String yanzheng_userLogin(String userLogin) {

		String url = "http://124.254.56.58:8007/api/Content/GetUserNameByUlogin?ulogin=" + userLogin;

		String R = null;
		HttpResponse httpResponse = null;
		try {
			HttpGet httpPost = new HttpGet(url);

			httpResponse = new DefaultHttpClient().execute(httpPost);
			System.out.println(httpResponse.getStatusLine().getStatusCode());

			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				result = result.replaceAll("\r", "");
				System.out.println(result);

				JSONObject aObject = JSON.parseObject(result);

				R = aObject.get("Successful").toString();
				System.out.println("successful :" + R);

			}

		} catch (Exception e) {
			e.getMessage();
		}

		return R;
	}

	/*** 调用服务网 接口 **/

	/**
	 * 验证用户名是否存在
	 * 
	 * @param username
	 *            用户名
	 * @return
	 */
	public BaseModel checkIsExist(String username) {
		String url = path + "Content/CheckUserLogin?ulogin=" + username;
		BaseModel bm = null;
		Request request = new Request.Builder().get().url(url).build();
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				bm = gson.fromJson(response.body().string(), BaseModel.class);
			} else {
				bm = new BaseModel();
				bm.Successful = false;
				bm.Error = "服务器繁忙";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bm = new BaseModel();
			bm.Successful = false;
			bm.Error = "服务器繁忙";
		}
		return bm;
	}

	/**
	 * 会员注册
	 * 
	 * @param userLogin
	 *            帐号
	 * @param passWord
	 *            密码
	 * @param passWordConfirm
	 *            去人密码
	 * @param zy
	 *            服务专员
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public BaseModelJson<String> register(String userLogin, String passWord, String passWordConfirm, String zy) {
		String url = path + "Content/RegisterNew";
		BaseModelJson<String> bmj = null;
		JSONObject param = new JSONObject();
		param.put("userLogin", userLogin);
		param.put("passWord", passWord);
		param.put("passWordConfirm", passWordConfirm);
		param.put("zy", zy);
		param.put("kbn", "0");
		param.put("cardNo", "");
		com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(JSONTPYE, gson.toJson(param));
		Request request = new Request.Builder().url(url).post(body).build();
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				bmj = gson.fromJson(response.body().string(), BaseModelJson.class);
			} else {
				bmj = new BaseModelJson<>();
				bmj.Successful = false;
				bmj.Error = "服务器繁忙";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bmj = new BaseModelJson<>();
			bmj.Successful = false;
			bmj.Error = "服务器繁忙";
		}
		return bmj;
	}

	/**
	 * 登录
	 * 
	 * @param userLogin
	 *            帐号
	 * @param passWord
	 *            密码
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public BaseModelJson<String> sigIn(String userLogin, String passWord) {
		String url = path + "Content/SignIn?userLogin=" + userLogin + "&userPass=" + passWord;
		JSONObject param = new JSONObject();
		com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(JSONTPYE, gson.toJson(param));
		Request request = new Request.Builder().url(url).post(body).build();
		BaseModelJson<String> bmj = null;
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				bmj = gson.fromJson(response.body().string(), BaseModelJson.class);
			} else {
				bmj = new BaseModelJson<>();
				bmj.Successful = false;
				bmj.Error = "服务器繁忙";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bmj = new BaseModelJson<>();
			bmj.Successful = false;
			bmj.Error = "服务器繁忙";
		}
		return bmj;
	}

	/**
	 * 根据token获取 会员信息
	 * 
	 * @param token
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public BaseModelJson<FwwUser> getFwwUserInfo(String token) {
		String url = path + "Member/GetZcUserById";
		Request request = new Request.Builder().url(url).addHeader("Token", token).get().build();
		BaseModelJson<FwwUser> bmj = null;
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				bmj = gson.fromJson(response.body().string(), BaseModelJson.class);
			} else {
				bmj = new BaseModelJson<>();
				bmj.Successful = false;
				bmj.Error = "服务器繁忙";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bmj = new BaseModelJson<>();
			bmj.Successful = false;
			bmj.Error = "服务器繁忙";
		}
		return bmj;
	}

}
