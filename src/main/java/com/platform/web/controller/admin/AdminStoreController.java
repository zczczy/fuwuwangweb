package com.platform.web.controller.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.platform.common.contants.Constants;
import com.platform.common.utils.DateUtil;
import com.platform.entity.MerchantInfo;
import com.platform.entity.Order;
import com.platform.entity.Store_state;
import com.platform.entity.vo.StoreForWeb;
import com.platform.entity.vo.StoreVo;
import com.platform.service.StoreService;

@Controller
@RequestMapping("/admin/store/")
public class AdminStoreController {

	@Autowired
	private StoreService storeService;

	Store_state sstate = new Store_state();

	/**
	 * 店铺管理的首页
	 *
	 */
	@RequestMapping(value = "{type}", method = RequestMethod.GET)
	public String liststore(Model model, @PathVariable String type, Integer pageNum, Integer pageSize, String phone,
			String searchBy ,HttpSession session) throws Exception {
		// 默认显示待审核状态
		if (pageNum == null)
			pageNum = 1;
		if (pageSize == null)
			pageSize = Constants.PAGE_SIZE;
		// pageSize=2;
		PageHelper.startPage(pageNum, pageSize, true);


		List<StoreForWeb> lStores = new ArrayList<StoreForWeb>();
		if (type.equals("list")){
			
			lStores = storeService.findStoreOrderByStatus();
		}
		else if (type.equals("search")) {
			if (("0").equals(searchBy)) {//店铺名

				lStores = storeService.findstoreByname(phone);
			}
			if(("1").equals(searchBy)){//商户用户名
				lStores = storeService.findstoreByMerchant_name(phone);
			}

		}
	
		List<String> params = new ArrayList<String>();
		if (phone != null && phone.length() != 0) {
			String param1 = "phone=" + phone + "&";
			params.add(param1);
		}
		if (searchBy != null && searchBy.length() != 0) {
			String param1 = "searchBy=" + searchBy + "&";
			params.add(param1);
			model.addAttribute("searchBy", searchBy);
		}

		model.addAttribute("params", params);
		model.addAttribute("page", new PageInfo<StoreForWeb>(lStores));
		return "admin/store/adminliststore";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	@RequestMapping(value = "auditsuccess", method = RequestMethod.POST)
	public String checkstore(Model model,String store_id, HttpSession session) throws Exception {
		MerchantInfo u=(MerchantInfo) session.getAttribute("bean");
		Store_state state = new Store_state();
		state.setStore_id(store_id);
		state.setStore_state(Constants.STORE_ACTIVE);
		state.setCheck_user_id(u.getUserLogin());
		storeService.updateStoreState(state);
		System.out.println("执行审核通过"+"  "+"状态："+state+" "+"店铺："+store_id+"  "+"用户："+u);
		return Constants.YU+"admin/store/list";
		// 默认显示待审核状态
		}
	@RequestMapping(value = "auditfail", method = RequestMethod.POST)
	public String auditfail(Model model,String store_id, HttpSession session) throws Exception {
		MerchantInfo u=(MerchantInfo) session.getAttribute("bean");
		Store_state state = new Store_state();
		state.setStore_id(store_id);
		state.setStore_state(Constants.STORE_FAILURE);
		state.setCheck_user_id(u.getUserLogin());
		storeService.updateStoreState(state);
		System.out.println("执行拒绝审核"+"  "+"状态："+state+" "+"店铺："+store_id+"  "+"用户："+u);
		return Constants.YU+"admin/store/list";
		// 默认显示待审核状态
	}
	@RequestMapping(value = "lockstore", method = RequestMethod.POST)
	public String lockstore(Model model,String store_id, HttpSession session) throws Exception {
		MerchantInfo u=(MerchantInfo) session.getAttribute("bean");
		Store_state state = new Store_state();
		state.setStore_id(store_id);
		state.setStore_state(Constants.STORE_LOCK);
		state.setCheck_user_id(u.getUserLogin());
		storeService.updateStoreState(state);
		System.out.println("执行锁定店铺"+"  "+"状态："+state+" "+"店铺："+store_id+"  "+"用户："+u);
		return Constants.YU+"admin/store/list";
		// 默认显示待审核状态
	}
	@RequestMapping(value = "activitystore", method = RequestMethod.POST)
	public String activitystore(Model model,String store_id, HttpSession session) throws Exception {
		MerchantInfo u=(MerchantInfo) session.getAttribute("bean");
		Store_state state = new Store_state();
		state.setStore_id(store_id);
		state.setCheck_user_id(u.getUserLogin());
		state.setStore_state(Constants.STORE_ACTIVE);
		storeService.updateStoreState(state);
		System.out.println("执行解锁店铺"+"  "+"状态："+state+" "+"店铺："+store_id+"  "+"用户："+u);
		return Constants.YU+"admin/store/list";
		// 默认显示待审核状态
	}

	/******店铺订单
	 * @throws Exception *****/
	@RequestMapping(value = "selectOrder_store" , method = RequestMethod.GET)
	public  String  selectOrder_store(String  storename , String  order_time_start , String order_time_end,
			Model model, Integer pageNum, Integer pageSize ) throws Exception{
		
		// 默认显示待审核状态
		if (pageNum == null)
			pageNum = 1;
		if (pageSize == null)
			 pageSize=Constants.PAGE_SIZE;
		if (null != order_time_start || null != order_time_end) {
			if (order_time_start.length() > 0 && order_time_end.length() == 0) {
				order_time_end = DateUtil.getDay();
			} else if (order_time_start.length() == 0 && order_time_end.length() == 0) {
				Calendar cal = Calendar.getInstance();// 获取一个Claender实例
				cal.set(1900, 01, 01);
				order_time_start = DateUtil.getyy_mm_dd(cal.getTime());
				order_time_end = DateUtil.getDay();
			}
		}else{
			order_time_start = DateUtil.getDay();
			order_time_end = DateUtil.getDay();
		}
		PageHelper.startPage(pageNum, pageSize, true);
		System.out.println("进入店铺订单查看 ： "  + storename );		

           List<Order>	 lOrders = new ArrayList<Order>();
		   
			 lOrders = storeService.findOrderByGoodsId(storename, order_time_start, order_time_end);
		
	   
		
	    List<String> params = new ArrayList<String>();
		/*if (order_time_start != null && order_time_start.length() != 0 
			&& order_time_end != null && order_time_end.length() != 0
			&& storename != null && storename.length() !=0 ) {*/
			
			String param1 = "order_time_start=" + order_time_start + "&"
					         + "order_time_end=" + order_time_end + "&"  + "storename=" + storename + "&";
			
			
			params.add(param1);
		//}

	    model.addAttribute("params", params);
		
		model.addAttribute("page", new PageInfo<Order>(lOrders));
		
		
		return  "admin/store/storeorder";
	}
	@RequestMapping(value = "findstroeinfobystore_id", method = RequestMethod.GET)
	@ResponseBody
	public StoreVo findstroeinfobystore_id(Model model, String store_id, String type) {
		StoreVo vo=storeService.findStoreinfoByStore_id(store_id);
		return vo;
	}

	
}
