/*
 * Copyright (C) 2017 优客服-多渠道客服系统
 * Modifications copyright (C) 2018 Chatopera Inc, <https://www.chatopera.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chatopera.cc.webim.web.handler;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.text.ParseException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.chatopera.cc.core.UKDataContext;
import com.chatopera.cc.util.UKView;
import com.chatopera.cc.webim.service.cache.CacheHelper;
import com.chatopera.cc.webim.service.repository.TenantRepository;
import com.chatopera.cc.webim.web.handler.api.rest.QueryParams;
import com.chatopera.cc.webim.web.model.Tenant;
import com.chatopera.cc.webim.web.model.User;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.chatopera.cc.util.UKTools;
import com.chatopera.cc.webim.web.model.SystemConfig;


@Controller
@SessionAttributes
public class Handler {
	@Autowired
	private TenantRepository tenantRes;
	
	public final static int PAGE_SIZE_BG = 1 ;
	public final static int PAGE_SIZE_TW = 20 ;
	public final static int PAGE_SIZE_FV = 50 ;
	public final static int PAGE_SIZE_HA = 100 ;
	
	private long starttime = System.currentTimeMillis();
	
	public User getUser(HttpServletRequest request){
		User user = (User) request.getSession(true).getAttribute(UKDataContext.USER_SESSION_NAME)  ;
		if(user==null){
			String authorization = request.getHeader("authorization") ;
			if(StringUtils.isBlank(authorization) && request.getCookies()!=null){
				for(Cookie cookie : request.getCookies()){
					if(cookie.getName().equals("authorization")){
						authorization = cookie.getValue() ; break ;
					}
				}
			}
			if(!StringUtils.isBlank(authorization)){
				user = (User) CacheHelper.getApiUserCacheBean().getCacheObject(authorization, UKDataContext.SYSTEM_ORGI) ;
			}
			if(user==null){
				user = new User();
				user.setId(UKTools.getContextID(request.getSession().getId())) ;
				user.setUsername(UKDataContext.GUEST_USER+"_"+UKTools.genIDByKey(user.getId())) ;
				user.setOrgi(UKDataContext.SYSTEM_ORGI);
				user.setSessionid(user.getId()) ;
			}
		}else{
			user.setSessionid(user.getId()) ;
		}
		return user ;
	}
	
	/**
	 * 
	 * @param queryBuilder
	 * @param request
	 */
	public BoolQueryBuilder search(BoolQueryBuilder queryBuilder , ModelMap map, HttpServletRequest request){
		queryBuilder.must(termQuery("orgi", this.getOrgi(request))) ;
		
		//搜索框
		if(!StringUtils.isBlank(request.getParameter("q"))) {
			String q = request.getParameter("q") ;
			q = q.replaceAll("(OR|AND|NOT|:|\\(|\\))", "") ;
			if(!StringUtils.isBlank(q)){
				queryBuilder.must(QueryBuilders.boolQuery().must(new QueryStringQueryBuilder(q).defaultOperator(Operator.AND))) ;
				map.put("q", q) ;
			}
		}
		
		//筛选表单
		if(!StringUtils.isBlank(request.getParameter("filterid"))) {
			queryBuilder.must(termQuery("filterid", request.getParameter("filterid"))) ;
			map.put("filterid", request.getParameter("filterid")) ;
		}
		
		//批次
		if(!StringUtils.isBlank(request.getParameter("batid"))) {
			queryBuilder.must(termQuery("batid", request.getParameter("batid"))) ;
			map.put("batid", request.getParameter("batid")) ;
		}
		
		//活动
		if(!StringUtils.isBlank(request.getParameter("actid"))) {
			queryBuilder.must(termQuery("actid", request.getParameter("actid"))) ;
			map.put("actid", request.getParameter("actid")) ;
		}
		
		//业务状态
		if(!StringUtils.isBlank(request.getParameter("workstatus"))) {
			queryBuilder.must(termQuery("workstatus", request.getParameter("workstatus"))) ;
			map.put("workstatus", request.getParameter("workstatus")) ;
		}
		
		//拨打状态
		if(!StringUtils.isBlank(request.getParameter("callstatus"))) {
			queryBuilder.must(termQuery("callstatus", request.getParameter("callstatus"))) ;
			map.put("callstatus", request.getParameter("callstatus")) ;
		}
		
		//预约状态
		if(!StringUtils.isBlank(request.getParameter("apstatus"))) {
			queryBuilder.must(termQuery("apstatus", request.getParameter("apstatus"))) ;
			map.put("apstatus", request.getParameter("apstatus")) ;
		}
		
		RangeQueryBuilder rangeQuery = null ;
		//拨打时间区间查询
		if(!StringUtils.isBlank(request.getParameter("callbegin")) || !StringUtils.isBlank(request.getParameter("callend"))){
			
			if(!StringUtils.isBlank(request.getParameter("callbegin"))) {
				try {
					
					rangeQuery = QueryBuilders.rangeQuery("calltime").from(UKTools.dateFormate.parse(request.getParameter("callbegin")).getTime()) ;
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
			}
			if(!StringUtils.isBlank(request.getParameter("callend")) ) {
				
				try {
					
					if(rangeQuery == null) {
						rangeQuery = QueryBuilders.rangeQuery("calltime").to(UKTools.dateFormate.parse(request.getParameter("callend")).getTime()) ;
					}else {
						rangeQuery.to(UKTools.dateFormate.parse(request.getParameter("callend")).getTime()) ;
					}
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
				
			}
			map.put("callbegin", request.getParameter("callbegin")) ;
			map.put("callend", request.getParameter("callend")) ;
		}
		//预约时间区间查询
		if(!StringUtils.isBlank(request.getParameter("apbegin")) || !StringUtils.isBlank(request.getParameter("apend"))){
			
			if(!StringUtils.isBlank(request.getParameter("apbegin"))) {
				try {
					
					rangeQuery = QueryBuilders.rangeQuery("aptime").from(UKTools.dateFormate.parse(request.getParameter("apbegin")).getTime()) ;
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
			}
			if(!StringUtils.isBlank(request.getParameter("apend")) ) {
				
				try {
					
					if(rangeQuery == null) {
						rangeQuery = QueryBuilders.rangeQuery("aptime").to(UKTools.dateFormate.parse(request.getParameter("apend")).getTime()) ;
					}else {
						rangeQuery.to(UKTools.dateFormate.parse(request.getParameter("apend")).getTime()) ;
					}
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
				
				
				
			}
			map.put("apbegin", request.getParameter("apbegin")) ;
			map.put("apend", request.getParameter("apend")) ;
		}
		
		if(rangeQuery!=null) {
			queryBuilder.must(rangeQuery) ;
		}
		
		//外呼任务id
		if(!StringUtils.isBlank(request.getParameter("taskid"))) {
			queryBuilder.must(termQuery("taskid", request.getParameter("taskid"))) ;
			map.put("taskid", request.getParameter("taskid")) ;
		}
		//坐席
		if(!StringUtils.isBlank(request.getParameter("owneruser"))) {
			queryBuilder.must(termQuery("owneruser", request.getParameter("owneruser"))) ;
			map.put("owneruser", request.getParameter("owneruser")) ;
		}
		//部门
		if(!StringUtils.isBlank(request.getParameter("ownerdept"))) {
			queryBuilder.must(termQuery("ownerdept", request.getParameter("ownerdept"))) ;
			map.put("ownerdept", request.getParameter("ownerdept")) ;
		}
		//分配状态
		if(!StringUtils.isBlank(request.getParameter("status"))) {
			queryBuilder.must(termQuery("status", request.getParameter("status"))) ;
			map.put("status", request.getParameter("status")) ;
		}

		return queryBuilder ;
	}
	
	public User getIMUser(HttpServletRequest request , String userid , String nickname){
		User user = (User) request.getSession(true).getAttribute(UKDataContext.IM_USER_SESSION_NAME)  ;
		if(user==null){
			user = new User();
			if(!StringUtils.isBlank(userid)){
				user.setId(userid) ;
			}else{
				user.setId(UKTools.getContextID(request.getSession().getId())) ;
			}
			if(!StringUtils.isBlank(nickname)){
				user.setUsername(nickname);
			}else{
				user.setUsername(UKDataContext.GUEST_USER+"_"+UKTools.genIDByKey(user.getId())) ;
			}
			user.setSessionid(user.getId()) ;
		}else{
			user.setSessionid(UKTools.getContextID(request.getSession().getId())) ;
		}
		return user ;
	}
	
	
	
	public void setUser(HttpServletRequest request , User user){
		request.getSession(true).removeAttribute(UKDataContext.USER_SESSION_NAME) ;
		request.getSession(true).setAttribute(UKDataContext.USER_SESSION_NAME , user) ;
	}
	

	/**
	 * 创建系统监控的 模板页面
	 * @param page
	 * @return
	 */
	public UKView createAdminTempletResponse(String page) {
		return new UKView("/admin/include/tpl" , page);
	}
	/**
	 * 创建系统监控的 模板页面
	 * @param page
	 * @return
	 */
	public UKView createAppsTempletResponse(String page) {
		return new UKView("/apps/include/tpl" , page);
	}
	
	/**
	 * 创建系统监控的 模板页面
	 * @param page
	 * @return
	 */
	public UKView createEntIMTempletResponse(String page) {
		return new UKView("/apps/entim/include/tpl" , page);
	}
	
	public UKView createRequestPageTempletResponse(String page) {
		return new UKView(page);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public ModelAndView request(UKView data) {
    	return new ModelAndView(data.getTemplet()!=null ? data.getTemplet(): data.getPage() , "data", data) ;
    }

	public int getP(HttpServletRequest request) {
		int page = 0;
		String p = request.getParameter("p") ;
		if(!StringUtils.isBlank(p) && p.matches("[\\d]*")){
			page = Integer.parseInt(p) ;
			if(page > 0){
				page = page - 1 ;
			}
		}
		return page;
	}
	
	public int getPs(HttpServletRequest request) {
		int pagesize = PAGE_SIZE_TW;
		String ps = request.getParameter("ps") ;
		if(!StringUtils.isBlank(ps) && ps.matches("[\\d]*")){
			pagesize = Integer.parseInt(ps) ;
		}
		return pagesize;
	}
	
	public int getP(QueryParams params) {
		int page = 0;
		if(params!=null && !StringUtils.isBlank(params.getP()) && params.getP().matches("[\\d]*")){
			page = Integer.parseInt(params.getP()) ;
			if(page > 0){
				page = page - 1 ;
			}
		}
		return page;
	}
	
	public int getPs(QueryParams params) {
		int pagesize = PAGE_SIZE_TW;
		if(params != null && !StringUtils.isBlank(params.getPs()) && params.getPs().matches("[\\d]*")){
			pagesize = Integer.parseInt(params.getPs()) ;
		}
		return pagesize;
	}
	
	
	public int get50Ps(HttpServletRequest request) {
		int pagesize = PAGE_SIZE_FV;
		String ps = request.getParameter("ps") ;
		if(!StringUtils.isBlank(ps) && ps.matches("[\\d]*")){
			pagesize = Integer.parseInt(ps) ;
		}
		return pagesize;
	}
	
	public String getOrgi(HttpServletRequest request){	
		return getUser(request).getOrgi();
	}
	/**
	 * 机构id
	 * @param request
	 * @return
	 */
	public String getOrgid(HttpServletRequest request){	
		User u = getUser(request);
		return u.getOrgid();
	}
	
	public Tenant getTenant(HttpServletRequest request){
		return tenantRes.findById(getOrgi(request));
	}
	/**
	 * 根据是否租户共享获取orgi
	 * @param request
	 * @return
	 */
	public String getOrgiByTenantshare(HttpServletRequest request){	
		SystemConfig systemConfig = UKTools.getSystemConfig();
		if(systemConfig!=null&&systemConfig.isEnabletneant()&&systemConfig.isTenantshare()) {
			User user = this.getUser(request) ;
			return user.getOrgid();
    	}
		return getOrgi(request);
	}
	
	/**
	 * 判断是否租户共享
	 * @return
	 */
	public boolean isTenantshare(){	
		SystemConfig systemConfig = UKTools.getSystemConfig();
		if(systemConfig!=null&&systemConfig.isEnabletneant()&&systemConfig.isTenantshare()) {
			return true;
    	}
		return false;
	}
	
	/**
	 * 判断是否多租户
	 * @return
	 */
	public boolean isEnabletneant(){	
		SystemConfig systemConfig = UKTools.getSystemConfig();
		if(systemConfig!=null&&systemConfig.isEnabletneant()) {
			return true;
    	}
		return false;
	}
	/**
	 * 判断是否多租户
	 * @return
	 */
	public boolean isTenantconsole(){	
		SystemConfig systemConfig = UKTools.getSystemConfig();
		if(systemConfig!=null&&systemConfig.isEnabletneant()&&systemConfig.isTenantconsole()) {
			return true;
    	}
		return false;
	}

	public long getStarttime() {
		return starttime;
	}

	public void setStarttime(long starttime) {
		this.starttime = starttime;
	}
}
