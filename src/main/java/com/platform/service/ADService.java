package com.platform.service;

import java.util.List;

import com.platform.entity.AD;
import com.platform.entity.vo.ADForWeb;
import com.platform.entity.vo.ADVo;



public interface ADService {

	
	/**
	 * app and web  条件： ad_position，ad_weight根据 获得 三种
	 */
	public  List<ADVo> select_ad( AD ad);
	
	/***安卓端查广告***/
	public List<ADVo>  select_adByCity_id(AD  ad);
	
	/**
	 * 管理员--添加广告
	 */
	public void addAD(AD ad) ;
	
	
	/**
	 * 管理员--默认查找活跃 或者 未发布 的广告
	 */
	public List<AD> selectAD_state(Integer ad_state) ;
	
	
	/**
	 * 管理员--默认查找 删除 的广告
	 */
	public List<AD> selectAD_Delete(Integer ad_state) ;
	
	
	
	
	
	/**
	 *  admin 删除广告
	 */
	public void  updateAD(AD ad);
	
	

	
	
	/**
	 * admin  根据时间端查看历史广告
	 */
	public List<AD>  selectAD_time(String  ad_create_start , String ad_create_end);
	
	
	
	/**
	 *  广告点击次数
	 */
	public void  addAD_click(String  ad_id);
	
	
	
	/**
	 * 根据广告id查询广告
	 * @param ad_id
	 * @return
	 */
	public ADForWeb selectADByad_id(String ad_id);

	
	
	
	public List<AD> findADlistByCity_id(Integer city_id);
	
	
	/**
	 * 修改广告
	 * @param ad
	 */
	public void updateADByID(AD ad);
	
}
