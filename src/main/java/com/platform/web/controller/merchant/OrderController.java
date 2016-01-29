package com.platform.web.controller.merchant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.common.contants.Constants;
import com.platform.common.utils.DateUtil;
import com.platform.entity.Order;
import com.platform.entity.Order_time;
import com.platform.entity.Return_ticket;
import com.platform.entity.User;
import com.platform.service.OrderService;
import com.platform.web.controller.app.BaseModelJson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@Controller
@RequestMapping(value = "/merchant/order/")
public class OrderController {

	@Autowired
	private OrderService orderService;

	OkHttpClient client = new OkHttpClient();

	public static final MediaType JSONTPYE = MediaType.parse("application/json; charset=utf-8");

	Gson gson = new Gson();
	
	/******* 当天订单 查看 ******/
	@RequestMapping(value = "{type}", method = RequestMethod.GET)
	public String order(Model model, @PathVariable String type, Integer pageNum, Integer pageSize,
			HttpSession session) {

		User u = (User) session.getAttribute("bean");
		System.out.println("商人的ID ：" + u.getUser_id());
		// 默认显示待审核状态
		if (pageNum == null)
			pageNum = 1;
		if (pageSize == null)
			pageSize = Constants.PAGE_SIZE;
		PageHelper.startPage(pageNum, pageSize, true);
		Order order = new Order();
		List<Order> lOrders = new ArrayList<Order>();

		if (type.equals("treated")) { // 当天已经处理的订单
			System.out.println("当天已处理订单");

			order.setOrder_state(Constants.ORDER_STATE_01);
			order.setUser_id(u.getUser_id());
			System.out.println("  时间 ： " + order.getOrder_time() + "  state :" + order.getOrder_state());

			lOrders = orderService.findOrderBystate(order);

			model.addAttribute("page", new PageInfo<Order>(lOrders));
			return "merchant/order/treatedorderlist";
		} else if (type.equals("untreated")) { // 当天未处理的订单
			System.out.println("当天wei处理订单");
			order.setUser_id(u.getUser_id());
			order.setOrder_state(Constants.ORDER_STATE_02);
			System.out.println("  时间 ： " + order.getOrder_time() + "  state :" + order.getOrder_state());
			lOrders = orderService.findOrderBystate(order);
		}
		System.out.println(lOrders);
		model.addAttribute("page", new PageInfo<Order>(lOrders));

		return "merchant/order/untreatedorderlist";
	}

	/**** --消费码 查订单 *****/
	@RequestMapping(value = "selectorder", method = RequestMethod.GET)
	public String selectorder(Model model, String trading_number,HttpSession session) {
		System.out.println("订单——order_id");
		User user=(User) session.getAttribute("bean");
		Order lorders = orderService.findOrderBytrading_number(trading_number,user.getUser_id());
		if(lorders!=null){
			
			model.addAttribute("order", lorders);
			
		}else{
			model.addAttribute("info", "消费码有误，请重新输入！");
		}

		return "merchant/order/tradingorderlist";
	}

	/*** 订单交易 ***/
	@RequestMapping(value = "trading", method = RequestMethod.POST)
	public String trading(Model model,String order_id,HttpServletRequest request) {

		Order order = new Order();
		order.setOrder_id(order_id);
		System.out.println("点击了 交易按钮" + " order_id :" + order_id);

		order.setOrder_state(Constants.ORDER_STATE_01);
		orderService.updateOrderBystate(order);
		model.addAttribute("info", "使用成功");
		return "merchant/order/tradingorderlist";

	}

	/**** 历史订单 查看 *****/
	@RequestMapping(value = "oldorder", method = RequestMethod.GET)
	public String oldorder(Model model, Integer pageNum, Integer pageSize, String order_time_start,
			String order_time_end, HttpSession session) {

		User u = (User) session.getAttribute("bean");
		System.out.println("商人的ID ：" + u.getUser_id());
		// 默认显示待审核状态
		if (pageNum == null)
			pageNum = 1;
		if (pageSize == null)
			pageSize = Constants.PAGE_SIZE;
		PageHelper.startPage(pageNum, pageSize, true);
		if(null!=order_time_start&&null!=order_time_end){
			
		
		if(order_time_start.length()>0&&order_time_end.length()==0){
			order_time_end=DateUtil.getDay();
		}else if(order_time_start.length()==0&&order_time_end.length()==0){
			Calendar cal = Calendar.getInstance();//获取一个Claender实例
		    cal.set(1900,01,01);
		    order_time_start=DateUtil.getyy_mm_dd(cal.getTime());
		    order_time_end=DateUtil.getDay();
		}
		}
		System.out.println("起始时间" + order_time_start + "截至时间" + order_time_start);

		Order_time order_time = new Order_time(order_time_start, order_time_end);
		order_time.setUser_id(u.getUser_id());
		order_time.setOrder_state(Constants.ORDER_STATE_02);
		List<Order> lorder = orderService.findOrderBytime(order_time);

		List<String> params = new ArrayList<String>();
		if (order_time_start != null && order_time_start.length() != 0 && order_time_end != null
				&& order_time_end.length() != 0) {
			String param1 = "order_time_start=" + order_time_start + "&" + "order_time_end=" + order_time_end + "&";
			params.add(param1);
		}

		model.addAttribute("params", params);
		model.addAttribute("page", new PageInfo<Order>(lorder));

		System.out.println(lorder);

		return "merchant/order/Oldorderlist";
	}

	/*** 定位 **/
	@RequestMapping(value = "execute", method = RequestMethod.GET)
	public String execute(HttpServletRequest request) {
		String info=(String) request.getAttribute("info");
		if(null!=info){
			request.setAttribute("info", info);
		}
		return "merchant/order/tradingorderlist";

	}

	/*** 查所有会员 没有返券的订单 **/
	@RequestMapping(value = "fanquan", method = RequestMethod.GET)
	public String fanquan(Model model,HttpSession session) {
		User user=(User) session.getAttribute("bean");
		List<Order> lorder = orderService.selectUseVip_fanquan(user.getUser_id(),Constants.ORDER_RETURN_NUMBER_STATE_02);
		model.addAttribute("order", lorder);

		model.addAttribute("return_number_state", Constants.ORDER_RETURN_NUMBER_STATE_02);
		return "merchant/order/fanquan";

	}
	/*** 查所有会员返券的订单根据返券状态 **/
	@RequestMapping(value = "zhaofanquan", method = RequestMethod.GET)
	public String zhaofanquan(Model model,HttpSession session,Integer return_number_state) {
		User user=(User) session.getAttribute("bean");
		List<Order> lorder = orderService.selectUseVip_fanquan(user.getUser_id(),return_number_state);
		System.out.println("输出所有的订单 ：" + lorder);
		model.addAttribute("order", lorder);
		model.addAttribute("return_number_state", return_number_state);
		
		return "merchant/order/fanquan";
		
	}

	/***** 给 会员返券 ******/
	@RequestMapping(value = "Return_ticket", method = RequestMethod.POST)
	public String Return_ticket(HttpServletRequest request, String order_id, String pay_password, HttpSession session) {

		//System.out.println("返券：  订单的ID ：" + order_id + "  商人的支付密码 ： " + pay_password);

		User user = (User) session.getAttribute("bean");
		System.out.println("session : 商人 agui ：" + user);
		Return_ticket ort = orderService.selectfanquan_info(order_id);
		//String successful = send_ticketInfo(ort); // 返券信息 发给公司
		BaseModelJson<String> result=toMemberElectronicVoucher(ort.getReturn_mz(),ort.getReturn_number().toString(),ort.getUserLogin(),user.getMerchant_account(),pay_password);
		//System.out.println(result.Error);
		if (result.Successful) {

			Order order = new Order();
			order.setOrder_id(order_id);
			order.setReturn_number_state(Constants.ORDER_RETURN_NUMBER_STATE_03);
			orderService.updateorder_return_number_state(order);
			request.setAttribute("info", "返券成功");
			return "redirect:/merchant/order/fanquan";
		} else {
			request.setAttribute("info","返券错误，"+result.Error);
			return "/merchant/order/fanquan";
		}

	}

	/***** 给公司发送发送返券信息 *****/
	public static String send_ticketInfo(Return_ticket orTicket) {

		String successful = null;
		String url = "http://124.254.56.58:8007/api/Content/ToMemberElectronicVoucher?mz=" + orTicket.getReturn_mz()
				+ "&num=" + orTicket.getReturn_number() + "&ulogin=" + orTicket.getUserLogin() + "&comName="
				+ orTicket.getMerchant_name() + "&compw=" + orTicket.getPay_password();

		HttpResponse httpResponse = null;
		try {
			HttpPost httpPost = new HttpPost(url);

			// httpPost.addHeader("Token", token );

			httpResponse = new DefaultHttpClient().execute(httpPost);
			System.out.println(httpResponse.getStatusLine().getStatusCode());

			if (200 == httpResponse.getStatusLine().getStatusCode()) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				result = result.replaceAll("\r", "");

				JSONObject aObject = JSON.parseObject(result);
				successful = aObject.get("Successful").toString();
				System.out.println("给公司发送返券信息        返回 结果是：" + result);

			}
		} catch (Exception e) {
			e.getMessage();
		}

		return successful;
	}

	
	/**
	 * 
	 * @param mz 面值 7：100面值兑现券 8：200面值兑现券 9：500面值兑现券 10：400面值兑现券
	 * @param num 数量
	 * @param ulogin 会员登陆用户名
	 * @param comName 服务网帐号
	 * @param compw 商家支付密码
	 * @return
	 */
	public  BaseModelJson<String> toMemberElectronicVoucher(String mz,String num,String ulogin,String comName,String compw){
		String url = Constants.PATH + "Content/ToMemberElectronicVoucher?mz=" + mz + "&num=" + num + "&ulogin="
				+ ulogin + "&comName=" + comName+ "&compw=" + compw;
		JSONObject param = new JSONObject();
		com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(JSONTPYE, gson.toJson(param));
		Request request = new Request.Builder().url(url).post(body).build();
		BaseModelJson<String> bmj = null;
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				bmj = gson.fromJson(response.body().string(), new TypeToken<BaseModelJson<String>>(){}.getType());
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
