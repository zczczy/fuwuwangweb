package com.platform.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.platform.common.contants.Constants;
import com.platform.common.utils.Md5;
import com.platform.common.utils.UUIDUtil;
import com.platform.entity.MerchantInfo;
import com.platform.entity.Order;
import com.platform.entity.Order_time;
import com.platform.entity.User;
import com.platform.entity.User_token;
import com.platform.mapper.IOSTokenMapper;
import com.platform.mapper.OrderMapper;
import com.platform.mapper.UserMapper;
import com.platform.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserMapper mapper;

	@Autowired
	private OrderMapper ordermapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private IOSTokenMapper iostokenMapper;

	String results = null;

	/**
	 * app用户登录
	 */
	public User addapplogin(String userLogin, String userPass, String type) {

		User user = new User();
		/*** VIP ****/
		if ("4".equals(type)) {

			System.out.println("进入 vip 登录.....会员帐号：" + userLogin + "   会员密码 ： " + userPass);

			// 读者需要将本例中的IP换成自己机器的IP
			String url = "http://124.254.56.58:8007/api/Content/SignIn?userLogin=" + userLogin + "&userPass="
					+ userPass;

			HttpResponse httpResponse = null;
			User user_vip = mapper.findUser(userLogin); // 获取登陆人的所有信息;
			try {
				HttpPost httpPost = new HttpPost(url);

				httpResponse = new DefaultHttpClient().execute(httpPost);
				System.out.println(httpResponse.getStatusLine().getStatusCode());

				if (200 == httpResponse.getStatusLine().getStatusCode()) {
					String result = EntityUtils.toString(httpResponse.getEntity());
					result = result.replaceAll("\r", "");

					System.out.println("返回值：" + result);

					JSONObject aObject = JSON.parseObject(result);
					if (aObject.get("Successful").toString().equals("true")) {

						if (null == user_vip) {
							System.out.println(" 这个 会员在本地没有保存数据....... 把他的数据插入到数据库中。。。。");
							User uu = new User();
							uu.setUser_id(UUIDUtil.getRandom32PK()); // id
							uu.setUserLogin(userLogin); // 帐号
							uu.setPassWord(Md5.getVal_UTF8(userPass)); // 密码md5
							uu.setUser_type(Constants.USER_VIP); // 会员用户
							uu.setUser_state(Constants.USER_ACTIVE); // 活跃
							System.out.println("U  :" + uu.getUser_id() + "   " + uu.getUser_type());
							userMapper.userrigester_user(uu); // 注册的会员插入到
							uu.setRealName("--");
							uu.setIdCard("--");
							uu.setDq("--");
							uu.setZy("--");
							uu.setUser_email("--");
							uu.setUser_img(".jpg");
							userMapper.userrigester_userinfo(uu);

						}

						String token = aObject.get("Data").toString();
						System.out.println("会员令牌：" + token);
						user_vip = mapper.findUser(userLogin); // 获取登陆人的所有信息
						user_vip.setToken(token);
						user_vip.setResults("登录成功");

						user_vip.setDianzibi(select_dianzibi(token));
						user_vip.setLongbi(select_longbi(token));
						// user_vip.setSession_id(session.getId());
						// session.setAttribute("session_user", user_vip);

					} else {
						user_vip = new User();
						user_vip.setResults(aObject.get("Error").toString());
					}

				}

			} catch (Exception e) {
				e.getMessage();
			}
			System.out.println("User  会员登录后 ： " + user_vip);
			return user_vip;

		}

		else if ("3".equals(type)) {

			System.out.println("进入  普通用户 登录");
			User user_putong = mapper.findUser(userLogin); // 获取登陆人的所有信息
			System.out.println("--------" + userLogin);
			if ((null) != user_putong) {

				if (user_putong.getUser_type().equals("3")) { // 普通用户

					if (user_putong.getUser_state().equals("2")) { // 正常用户

						if (user_putong.getPassWord().equals(Md5.getVal_UTF8(userPass))) {

							user_putong.setUserLogin(userLogin);
							user_putong.setResults("登录成功");

							System.out.println("服务层修改用户" + user_putong.getUser_id() + "登录状态");

							System.out.println("普通用户的ID：" + user_putong.getUser_id());
							// session.setAttribute("session_user",
							// user_putong); // 密码正确
							// 登录成功
							// user_putong.setSession_id(session.getId()); //
							// 加入ID；

						} else {
							user_putong.setResults("账户或密码错误！"); // 密码错误
						}
					} else {
						user_putong.setResults("用户冻结"); // 违规用户
					}

				}

				else {
					user_putong.setResults("非法帐号"); // 商人 && 管理员 de 帐号不让登陆app
				}

			}
			if (null == user_putong) {
				user_putong = new User();
				user_putong.setResults("帐号不存在");
			}
			System.out.println("User 普通用户登录后 ： " + user_putong);
			return user_putong;

		} else {

			user.setResults("请求错误");
			return user;

		}

	}

	/**
	 * web 端 登录
	 */
	public User weblogin(String userLogin) {

		User user = mapper.selectUserlogin(userLogin);

		return user;
	}

	/**
	 * 用户注册
	 */
	public void adduser(User user) {

		mapper.userrigester_user(user);
		mapper.userrigester_userinfo(user);

	}

	/**
	 * 查看所有用户
	 */
	public List<User> userlist() throws Exception {

		List<User> list = mapper.userlist();

		return list;
	}

	/**
	 * 用户锁定,解锁，删除
	 */
	public void updateuser_state(User user) {

		mapper.updateuser_state(user);
		System.out.println("对用户进行了用户锁定,解锁，删除");
	}

	/*** 查看个人信息 ***/
	public User usermsg(String userLogin) {

		User user1 = mapper.findByUsername(userLogin);

		return user1;
	}

	/***** 修改密码 *****/
	public void updatepass(User user) throws Exception {

		mapper.updatepass(user);

	}

	/***** 根据名字查用户 *****/
	public List<User> finduserByname(String phone) {

		List<User> list = mapper.finduserByname(phone);
		return list;
	}

	public List<User> findMerchantByname(String merchant_name) {

		List<User> list = mapper.findMerchantByname(merchant_name);
		return list;
	}

	/****** 查看订单 ******/
	public List<Order> findOrderByUserId(String username, String order_time_start, String order_time_end)
			throws Exception {

		List<Order> list = new ArrayList<Order>();
		Order_time orTime = new Order_time();
		orTime.setOrder_time_start(order_time_start);
		orTime.setOrder_time_end(order_time_end);
		orTime.setUser_name(username);

		if (!("").equals(username) && !(null == username)) {
			System.out.println("有名字查订单");
			list = ordermapper.findOrderByUserId(orTime);
		} else
			list = ordermapper.findOrderBytime_1(orTime);
		System.out.println("没名字查订单");
		return list;
	}

	/******* 管理员注册 商人 ********/
	public void add_merchant(User user) {

		mapper.userrigester_user(user);
		mapper.userrigester_merchantinfo(user);
	}

	/****** 查看用户龙币 *******/
	public static Integer select_longbi(String token) {

		System.out.println("获取龙币......令牌..." + token);

		Integer Longbi_number = null;
		String url = "http://124.254.56.58:8007/api/Member/GetMemberLongBi";

		HttpResponse httpResponse = null;
		try {
			HttpGet httpget = new HttpGet(url);

			httpget.addHeader("Token", token);

			httpResponse = new DefaultHttpClient().execute(httpget);
			System.out.println(httpResponse.getStatusLine().getStatusCode());

			if (200 == httpResponse.getStatusLine().getStatusCode()) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				result = result.replaceAll("\r", "");

				JSONObject aObject = JSON.parseObject(result);
				Longbi_number = Integer.parseInt(aObject.get("Data").toString());

				System.out.println("用户龙币数量 ： " + result); //

			}
		} catch (Exception e) {
			e.getMessage();
		}

		return Longbi_number;

	}

	/****** 查看用户点子币 *******/
	public static Double select_dianzibi(String token) {

		System.out.println("获取点子币........." + token);

		Double dianzibi_number = null;
		String url = "http://124.254.56.58:8007/api/Member/GetMemberElectronicMoney";

		HttpResponse httpResponse = null;
		try {
			HttpGet httpget = new HttpGet(url);

			httpget.addHeader("Token", token);

			httpResponse = new DefaultHttpClient().execute(httpget);
			System.out.println(httpResponse.getStatusLine().getStatusCode());

			if (200 == httpResponse.getStatusLine().getStatusCode()) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				result = result.replaceAll("\r", "");

				JSONObject aObject = JSON.parseObject(result);
				dianzibi_number = Double.parseDouble(aObject.get("Data").toString());

				System.out.println("用户点子币数量 ： " + result); //

			}
		} catch (Exception e) {
			e.getMessage();
		}

		return dianzibi_number;

	}

	/**** user_id找用户 ****/
	public User finduserById(String user_id) {

		User user = userMapper.finduserById(user_id);

		return user;
	}

	/** 验证 商人是否 存在 *********/
	public User selectUserlogin(String userLogin) {
		User user = userMapper.selectUserlogin(userLogin);
		return user;
	}

	/** App 登录 验证账号是否存在 *********/
	public User findUser(String userLogin) {
		User user = userMapper.findUser(userLogin);
		return user;
	}

	/***** 用户注册t_user ******/
	public void adduser_t_user(User user) {
		userMapper.userrigester_user(user);

	}

	/***** 修改头像 *****/
	public void updateuser_img(User user) {

		userMapper.updauser_img(user);

	}

	/***
	 * 修改app普通用户邮箱
	 */
	public void updateEmail(User user) {
		userMapper.updateEmail(user);
	}

	/**** 用户 修改用户登录状态 ****/
	public void update_login_state(User user) {
		userMapper.updatelogin_state(user);

	}

	/**** 插入token *******/
	public void add_token(User user) {

		iostokenMapper.add_token(user);
	}

	/****** 取出token ****/
	public User select_token(String token) {

		return iostokenMapper.select_token(token);
	}

	/**** 取出所有token ******/
	public User_token select_UsertokenByUserid(String user_id) {

		return iostokenMapper.select_UsertokenByUserid(user_id);
	}

	/***** 根据user_id 修改 token ******/
	public void update_token(User_token user_token) {

		iostokenMapper.update_token(user_token);
	}

	/*** 添加验证码 ****/
	public void add_YZM(User_token user) {

		iostokenMapper.add_YZM(user);
	}

	/***** 获取单个验证码 ******/
	public User_token select_YZM(String user_id) {

		return iostokenMapper.select_YZM(user_id);
	}

	/**** 修改验证码 ****/
	public void update_YZM(User_token user_token) {
		iostokenMapper.update_YZM(user_token);

	}

	// leo

	/**
	 * 根据验证帐号是否存在
	 * 
	 * @param user_email
	 * @return
	 */
	public Integer chechUserIsExist(String userLogin) {

		return mapper.chechUserIsExist(userLogin);
	}

	/**
	 * 根据验证邮箱是否存在
	 * 
	 * @param user_email
	 * @return
	 */
	public Integer chechUserEmailIsExist(String user_email) {
		return mapper.chechUserEmailIsExist(user_email);
	}

	/**
	 * 用户登录验证
	 * 
	 * @param map
	 * @return
	 */
	public User login(Map<String, String> map) {
		return mapper.login(map);
	}

	/**
	 * 根据 token查询用户
	 * 
	 * @param token
	 * @return
	 */
	public User getUserInforByToken(String token) {
		return mapper.getUserInforByToken(token);
	}

	/**
	 * 根据 用tokenId查询用户
	 * 
	 * @param map
	 * @return
	 */
	public User getUserInforByToken(User user) {
		return mapper.getUserInforByToken(user);
	}

	/**
	 * 根据 用户名和邮箱查询用户
	 * 
	 * @param map
	 * @return
	 */
	public User getUserInforByUserNameAndEmail(User user) {
		return mapper.getUserInforByUserNameAndEmail(user);
	}

	/**
	 * 根据 用户名和邮箱查询用户
	 * 
	 * @param map
	 * @return
	 */
	public User getUserInforByUserNameAndCode(Map<String, String> map) {
		return mapper.getUserInforByUserNameAndCode(map);
	}

	public List<User> merchantlist() {
		// TODO Auto-generated method stub
		return mapper.merchantlist();
	}

	/**
	 * 根据用户名获取商人信息
	 */
	public User findmerchantByuserlogin(String userLogin) {

		return mapper.findmerchantByuserlogin(userLogin);
	}

	/**
	 * 用户登录
	 * 
	 * @param map
	 * @return
	 */
	@Override
	public MerchantInfo getUserLogin(Map<String, String> map) {
		return mapper.getUserLogin(map);
	}

	/**
	 * 添加商人
	 * 
	 * @param merchantInfo
	 */
	public int addMerchant(MerchantInfo merchantInfo) {
		return mapper.addMerchant(merchantInfo);
	}
	/**
	 * 商人申请
	 * 
	 * @param merchantInfo
	 */
	public int addMerchantExtra(MerchantInfo merchantInfo) {
		return mapper.addMerchantExtra(merchantInfo);
	}
	/**
	 * 商人申请
	 * 
	 * @param merchantInfo
	 */
	public int addMerchantInfo(MerchantInfo merchantInfo) {
		return mapper.addMerchantInfo(merchantInfo);
	}

	/**
	 * 更新登录时间
	 * 
	 * @param merchantInfo
	 */
	public int updateLoginState(MerchantInfo merchantInfo) {
		return mapper.updateLoginState(merchantInfo);
	}

	/**
	 * 修改密码
	 * 
	 * @param map
	 */
	public int updatePassword(Map<String, String> map) {
		return mapper.updatePassword(map);
	}

	/**
	 * 判断用户是否存在
	 * @param userLogin
	 * @return
	 */
	public int checkUserLoginIsExist(String userLogin){
		return mapper.checkUserLoginIsExist(userLogin);
	}
	
	/**
	 * 获取商人列表
	 * @return
	 */
	@Override
	public List<MerchantInfo> getMerchantList(){
		return mapper.getMerchantList();
	}
	
	/**
	 * 根据帐号模糊查找商人
	 * @param merchant_name
	 * @return
	 */
	public List<MerchantInfo> findMerchantByUserLogin(String userLogin){
		return mapper.findMerchantByUserLogin(userLogin);
	}
	
	/**
	 * 用户锁定，解锁，删除 
	 * @param merchantInfo
	 * @return
	 */
	public int updateUserState(MerchantInfo merchantInfo){
		return mapper.updateUserState(merchantInfo);
	}
	
	/**
	 * 查询 商家 信息和 申请信息
	 * @param user_id
	 * @return
	 */
	public MerchantInfo getMerchantInfo(String user_id){
		return mapper.getMerchantInfo(user_id);
	}
}
