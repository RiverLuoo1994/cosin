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

package com.chatopera.cc.webim.web.handler.apps.kbs;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.chatopera.cc.util.Menu;
import com.chatopera.cc.util.extra.DataExchangeInterface;
import com.chatopera.cc.util.task.DSData;
import com.chatopera.cc.util.task.ExcelImportProecess;
import com.chatopera.cc.util.task.export.ExcelExporterProcess;
import com.chatopera.cc.webim.service.repository.SysDicRepository;
import com.chatopera.cc.webim.service.repository.TopicItemRepository;
import com.chatopera.cc.webim.util.OnlineUserUtils;
import com.chatopera.cc.webim.web.model.KnowledgeType;
import com.chatopera.cc.webim.web.model.SysDic;
import com.chatopera.cc.webim.web.model.Topic;
import com.chatopera.cc.webim.web.model.TopicItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.chatopera.cc.core.UKDataContext;
import com.chatopera.cc.util.UKTools;
import com.chatopera.cc.util.task.DSDataEvent;
import com.chatopera.cc.util.task.process.TopicProcess;
import com.chatopera.cc.webim.service.es.TopicRepository;
import com.chatopera.cc.webim.service.repository.AreaTypeRepository;
import com.chatopera.cc.webim.service.repository.KnowledgeTypeRepository;
import com.chatopera.cc.webim.service.repository.MetadataRepository;
import com.chatopera.cc.webim.service.repository.ReporterRepository;
import com.chatopera.cc.webim.web.handler.Handler;
import com.chatopera.cc.webim.web.model.MetadataTable;
import com.chatopera.cc.webim.web.model.UKeFuDic;

@Controller
@RequestMapping("/apps")
public class TopicController extends Handler{
	
	@Value("${web.upload-path}")
    private String path;
	
	@Value("${uk.im.server.port}")  
    private Integer port; 
	
	@Autowired
	private KnowledgeTypeRepository knowledgeTypeRes ;
	
	@Autowired
	private AreaTypeRepository areaRepository;
	
	@Autowired
	private SysDicRepository sysDicRepository;
	
	@Autowired
	private TopicRepository topicRes;
	
	
	@Autowired
	private MetadataRepository metadataRes ;
	
	
	@Autowired
	private TopicItemRepository topicItemRes ;
	
	@Autowired
	private ReporterRepository reporterRes ;
	
    
    @RequestMapping("/topic")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public ModelAndView knowledge(ModelMap map , HttpServletRequest request , @Valid String q , @Valid String type, @Valid String aiid) {
    	List<KnowledgeType> knowledgeTypeList = knowledgeTypeRes.findByOrgi(super.getOrgi(request))  ;
    	map.put("knowledgeTypeList", knowledgeTypeList);
    	KnowledgeType ktype = null ;
    	if(!StringUtils.isBlank(type)){
    		for(KnowledgeType knowledgeType : knowledgeTypeList){
    			if(knowledgeType.getId().equals(type)){
    				ktype = knowledgeType ;
    				break ;
    			}
    		}
    	}
    	if(!StringUtils.isBlank(q)){
    		map.put("q", q) ;
     	}
    	if(ktype!=null){
    		map.put("curtype", ktype) ;
    		map.put("topicList", topicRes.getTopicByCateAndOrgi(ktype.getId(),super.getOrgi(request), q, super.getP(request), super.getPs(request)))  ;
    	}else{
    		map.put("topicList", topicRes.getTopicByCateAndOrgi(UKDataContext.DEFAULT_TYPE,super.getOrgi(request), q, super.getP(request), super.getPs(request)))  ;
    	}
    	map.addAttribute("areaList", areaRepository.findByOrgi(super.getOrgi(request))) ;
    	return request(super.createAppsTempletResponse("/apps/business/topic/index"));
    }
    
    @RequestMapping("/topic/add")
    @Menu(type = "xiaoe" , subtype = "knowledgeadd")
    public ModelAndView knowledgeadd(ModelMap map , HttpServletRequest request , @Valid String type, @Valid String aiid) {
    	map.put("type", type);
    	map.put("aiid", aiid);
    	return request(super.createRequestPageTempletResponse("/apps/business/topic/add"));
    }
    
    @RequestMapping("/topic/save")
    @Menu(type = "xiaoe" , subtype = "knowledgesave")
    public ModelAndView knowledgesave(HttpServletRequest request , @Valid Topic topic , @Valid String type , @Valid String aiid) {
    	if(!StringUtils.isBlank(topic.getTitle())){
    		if(!StringUtils.isBlank(type)){
    			topic.setCate(type);
    		}else{
    			topic.setCate(UKDataContext.DEFAULT_TYPE);
    		}
    		topic.setOrgi(super.getOrgi(request));
    		topicRes.save(topic) ;
    		List<TopicItem> topicItemList = new ArrayList<TopicItem>();
    		for(String item : topic.getSilimar()) {
				TopicItem topicItem = new TopicItem();
				topicItem.setTitle(item);
				topicItem.setTopicid(topic.getId());
				topicItem.setOrgi(topic.getOrgi());
				topicItem.setCreater(topic.getCreater());
				topicItem.setCreatetime(new Date());
				topicItemList.add(topicItem) ;
			}
			if(topicItemList.size() > 0) {
				topicItemRes.save(topicItemList) ;
			}
    		/**
    		 * 重新缓存
    		 * 
    		 */
    		OnlineUserUtils.resetHotTopic((DataExchangeInterface) UKDataContext.getContext().getBean("topic") , super.getUser(request) , super.getOrgi(request) , aiid) ;
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html"+(!StringUtils.isBlank(type) ? "?type="+type : "")));
    }
    
    @RequestMapping("/topic/edit")
    @Menu(type = "xiaoe" , subtype = "knowledgeedit")
    public ModelAndView knowledgeedit(ModelMap map , HttpServletRequest request , @Valid String id , @Valid String type, @Valid String aiid) {
    	map.put("type", type);
    	if(!StringUtils.isBlank(id)){
    		map.put("topic", topicRes.findOne(id)) ;
    	}
    	List<KnowledgeType> knowledgeTypeList = knowledgeTypeRes.findByOrgi(super.getOrgi(request))  ; 
    	map.put("knowledgeTypeList", knowledgeTypeList);
    	return request(super.createRequestPageTempletResponse("/apps/business/topic/edit"));
    }
    
    @RequestMapping("/topic/update")
    @Menu(type = "xiaoe" , subtype = "knowledgeupdate")
    public ModelAndView knowledgeupdate(HttpServletRequest request ,@Valid Topic topic , @Valid String type, @Valid String aiid) {
    	Topic temp = topicRes.findOne(topic.getId()) ;
    	if(!StringUtils.isBlank(topic.getTitle())){
    		if(!StringUtils.isBlank(type)){
    			topic.setCate(type);
    		}else{
    			topic.setCate(UKDataContext.DEFAULT_TYPE);
    		}
    		topic.setCreater(temp.getCreater());
    		topic.setCreatetime(temp.getCreatetime());
    		topic.setOrgi(super.getOrgi(request));
    		
    		topicRes.save(topic) ;
    		topicItemRes.delete(topicItemRes.findByTopicid(topic.getId())) ;
    		List<TopicItem> topicItemList = new ArrayList<TopicItem>();
    		for(String item : topic.getSilimar()) {
				TopicItem topicItem = new TopicItem();
				topicItem.setTitle(item);
				topicItem.setTopicid(topic.getId());
				topicItem.setOrgi(topic.getOrgi());
				topicItem.setCreater(topic.getCreater());
				topicItem.setCreatetime(new Date());
				topicItemList.add(topicItem) ;
			}
			if(topicItemList.size() > 0) {
				topicItemRes.save(topicItemList) ;
			}
    		
    		/**
    		 * 重新缓存
    		 * 
    		 */
    		OnlineUserUtils.resetHotTopic((DataExchangeInterface) UKDataContext.getContext().getBean("topic") , super.getUser(request) , super.getOrgi(request),aiid) ;
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html"+(!StringUtils.isBlank(type) ? "?type="+type : "")));
    }
    
    @RequestMapping("/topic/delete")
    @Menu(type = "xiaoe" , subtype = "knowledgedelete")
    public ModelAndView knowledgedelete(HttpServletRequest request ,@Valid String id , @Valid String type, @Valid String aiid) {
    	if(!StringUtils.isBlank(id)){
    		topicRes.delete(topicRes.findOne(id));
    		/**
    		 * 重新缓存
    		 * 
    		 */
    		topicItemRes.delete(topicItemRes.findByTopicid(id)) ;
    		
    		OnlineUserUtils.resetHotTopic((DataExchangeInterface) UKDataContext.getContext().getBean("topic") , super.getUser(request) , super.getOrgi(request) , aiid) ;
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html"+(!StringUtils.isBlank(type) ? "?type="+type : "")));
    }
    
    @RequestMapping("/topic/type/add")
    @Menu(type = "xiaoe" , subtype = "knowledgetypeadd")
    public ModelAndView knowledgetypeadd(ModelMap map , HttpServletRequest request ,@Valid String type, @Valid String aiid) {
    	map.addAttribute("areaList", areaRepository.findByOrgi(super.getOrgi(request))) ;
    	
    	List<KnowledgeType> knowledgeTypeList = knowledgeTypeRes.findByOrgi(super.getOrgi(request))  ; 
    	map.put("knowledgeTypeList", knowledgeTypeList);
    	map.put("aiid", aiid);
    	if(!StringUtils.isBlank(type)){
    		map.put("type", type) ;
    	}
    	
    	return request(super.createRequestPageTempletResponse("/apps/business/topic/addtype"));
    }
    
    @RequestMapping("/topic/type/save")
    @Menu(type = "xiaoe" , subtype = "knowledgetypesave")
    public ModelAndView knowledgetypesave(HttpServletRequest request ,@Valid KnowledgeType type, @Valid String aiid) {
    	//int tempTypeCount = knowledgeTypeRes.countByNameAndOrgiAndParentidNot(type.getName(), super.getOrgi(request) , !StringUtils.isBlank(type.getParentid()) ? type.getParentid() : "0") ;
    	KnowledgeType knowledgeType = knowledgeTypeRes.findByNameAndOrgi(type.getName(), super.getOrgi(request)) ;
    	if(knowledgeType == null){
    		type.setOrgi(super.getOrgi(request));
    		type.setCreatetime(new Date());
    		type.setId(UKTools.getUUID());
    		type.setTypeid(type.getId());
    		type.setUpdatetime(new Date());
    		if(StringUtils.isBlank(type.getParentid())){
    			type.setParentid("0");
    		}else{
    			type.setTypeid(type.getParentid());
    		}
    		type.setCreater(super.getUser(request).getId());
    		knowledgeTypeRes.save(type) ;
    		OnlineUserUtils.resetHotTopicType((DataExchangeInterface) UKDataContext.getContext().getBean("topictype") , super.getUser(request), super.getOrgi(request) , aiid);
    	}else {
    		return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?aiid="+aiid+"&msg=k_type_exist"));
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?aiid="+aiid));
    }
    
    @RequestMapping("/topic/type/edit")
    @Menu(type = "xiaoe" , subtype = "knowledgetypeedit")
    public ModelAndView knowledgetypeedit(ModelMap map , HttpServletRequest request, @Valid String type , @Valid String aiid) {
    	map.put("knowledgeType", knowledgeTypeRes.findOne(type)) ;
    	map.addAttribute("areaList", areaRepository.findByOrgi(super.getOrgi(request))) ;
    	
    	map.put("aiid", aiid);
    	
    	List<KnowledgeType> knowledgeTypeList = knowledgeTypeRes.findByOrgi(super.getOrgi(request))  ; 
    	map.put("knowledgeTypeList", knowledgeTypeList);
    	return request(super.createRequestPageTempletResponse("/apps/business/topic/edittype"));
    }
    
    @RequestMapping("/topic/type/update")
    @Menu(type = "xiaoe" , subtype = "knowledgetypeupdate")
    public ModelAndView knowledgetypeupdate(HttpServletRequest request ,@Valid KnowledgeType type, @Valid String aiid) {
    	//int tempTypeCount = knowledgeTypeRes.countByNameAndOrgiAndIdNot(type.getName(), super.getOrgi(request) , type.getId()) ;
    	KnowledgeType knowledgeType = knowledgeTypeRes.findByNameAndOrgiAndIdNot(type.getName(), super.getOrgi(request),type.getId()) ;
    	if(knowledgeType == null){
    		KnowledgeType temp = knowledgeTypeRes.findByIdAndOrgi(type.getId(), super.getOrgi(request)) ;
    		temp.setName(type.getName());
    		temp.setParentid(type.getParentid());
    		if(StringUtils.isBlank(type.getParentid()) || type.getParentid().equals("0")){
    			temp.setParentid("0");
    			temp.setTypeid(temp.getId());
    		}else{
    			temp.setParentid(type.getParentid());
    			temp.setTypeid(type.getParentid());
    		}
    		knowledgeTypeRes.save(temp) ;
    		OnlineUserUtils.resetHotTopicType((DataExchangeInterface) UKDataContext.getContext().getBean("topictype") , super.getUser(request), super.getOrgi(request) , aiid);
    	}else {
    		return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?aiid="+aiid+"&msg=k_type_exist&type="+type.getId()));
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?aiid="+aiid+"type="+type.getId()));
    }
    
    @RequestMapping("/topic/type/delete")
    @Menu(type = "xiaoe" , subtype = "knowledgedelete")
    public ModelAndView knowledgetypedelete(HttpServletRequest request ,@Valid String id , @Valid String type, @Valid String aiid) {
    	Page<Topic> page = topicRes.getTopicByCateAndOrgi(type,super.getOrgi(request), null, super.getP(request), super.getPs(request)) ;
    	String msg = null ;
    	if(page.getTotalElements() == 0){
	    	if(!StringUtils.isBlank(id)){
	    		knowledgeTypeRes.delete(id);
	    		OnlineUserUtils.resetHotTopicType((DataExchangeInterface) UKDataContext.getContext().getBean("topictype") , super.getUser(request), super.getOrgi(request) , aiid);
	    	}
    	}else{
    		msg = "notempty" ;
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html"+(msg!=null ? "?msg=notempty" : "")));
    }
    
    @RequestMapping("/topic/area")
    @Menu(type = "admin" , subtype = "area")
    public ModelAndView area(ModelMap map ,HttpServletRequest request , @Valid String id, @Valid String aiid) {
    	
    	SysDic sysDic = sysDicRepository.findByCode(UKDataContext.UKEFU_SYSTEM_AREA_DIC) ;
    	if(sysDic!=null){
	    	map.addAttribute("sysarea", sysDic) ;
	    	map.addAttribute("areaList", sysDicRepository.findByDicid(sysDic.getId())) ;
    	}
    	map.addAttribute("cacheList", UKeFuDic.getInstance().getDic(UKDataContext.UKEFU_SYSTEM_AREA_DIC)) ;
    	
    	map.put("knowledgeType", knowledgeTypeRes.findOne(id)) ;
        return request(super.createRequestPageTempletResponse("/apps/business/topic/area"));
    }
    
    
    @RequestMapping("/topic/area/update")
    @Menu(type = "admin" , subtype = "organ")
    public ModelAndView areaupdate(HttpServletRequest request ,@Valid KnowledgeType type, @Valid String aiid) {
    	KnowledgeType temp = knowledgeTypeRes.findByIdAndOrgi(type.getId(), super.getOrgi(request)) ;
    	if(temp != null){
    		temp.setArea(type.getArea());
    		knowledgeTypeRes.save(temp) ;
    		OnlineUserUtils.resetHotTopicType((DataExchangeInterface) UKDataContext.getContext().getBean("topictype") , super.getUser(request), super.getOrgi(request) , aiid);
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?type="+type.getId()));
    }
    
    
    @RequestMapping("/topic/imp")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public ModelAndView imp(ModelMap map , HttpServletRequest request , @Valid String type, @Valid String aiid) {
    	map.addAttribute("type", type) ;
        return request(super.createRequestPageTempletResponse("/apps/business/topic/imp"));
    }
    
    @RequestMapping("/topic/impsave")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public ModelAndView impsave(ModelMap map , HttpServletRequest request , @RequestParam(value = "cusfile", required = false) MultipartFile cusfile , @Valid String type, @Valid String aiid) throws IOException {
    	DSDataEvent event = new DSDataEvent();
    	String fileName = "xiaoe/"+UKTools.getUUID()+cusfile.getOriginalFilename().substring(cusfile.getOriginalFilename().lastIndexOf(".")) ;
    	File excelFile = new File(path , fileName) ;
    	if(!excelFile.getParentFile().exists()){
    		excelFile.getParentFile().mkdirs() ;
    	}
    	MetadataTable table = metadataRes.findByTablename("uk_xiaoe_topic") ;
    	if(table!=null){
	    	FileUtils.writeByteArrayToFile(new File(path , fileName), cusfile.getBytes());
	    	event.setDSData(new DSData(table,excelFile , cusfile.getContentType(), super.getUser(request)));
	    	event.getDSData().setClazz(Topic.class);
	    	event.setOrgi(super.getOrgi(request));
	    	if(!StringUtils.isBlank(type)){
	    		event.getValues().put("cate", type) ;
	    	}else{
	    		event.getValues().put("cate", UKDataContext.DEFAULT_TYPE) ;
	    	}
	    	event.getValues().put("creater", super.getUser(request).getId()) ;
	    	event.getDSData().setProcess(new TopicProcess(topicRes));
	    	reporterRes.save(event.getDSData().getReport()) ;
	    	new ExcelImportProecess(event).process() ;		//启动导入任务
    	}
    	
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html?type="+type));
    }
    
    @RequestMapping("/topic/batdelete")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public ModelAndView batdelete(ModelMap map , HttpServletRequest request , HttpServletResponse response , @Valid String[] ids ,@Valid String type, @Valid String aiid) throws IOException {
    	if(ids!=null && ids.length > 0){
    		Iterable<Topic> topicList = topicRes.findAll(Arrays.asList(ids)) ;
    		topicRes.delete(topicList);
    		for(Topic topic : topicList) {
    			topicItemRes.delete(topicItemRes.findByTopicid(topic.getId())) ;
    		}
    	}
    	
    	return request(super.createRequestPageTempletResponse("redirect:/apps/topic.html"+(!StringUtils.isBlank(type) ? "?type="+type:"")));
    }
    
    @RequestMapping("/topic/expids")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public void expids(ModelMap map , HttpServletRequest request , HttpServletResponse response , @Valid String[] ids, @Valid String aiid) throws IOException {
    	if(ids!=null && ids.length > 0){
    		Iterable<Topic> topicList = topicRes.findAll(Arrays.asList(ids)) ;
    		MetadataTable table = metadataRes.findByTablename("uk_xiaoe_topic") ;
    		List<Map<String,Object>> values = new ArrayList<Map<String,Object>>();
    		for(Topic topic : topicList){
    			values.add(UKTools.transBean2Map(topic)) ;
    		}
    		
    		response.setHeader("content-disposition", "attachment;filename=UCKeFu-Contacts-"+new SimpleDateFormat("yyyy-MM-dd").format(new Date())+".xls");  
    		if(table!=null){
    			ExcelExporterProcess excelProcess = new ExcelExporterProcess( values, table, response.getOutputStream()) ;
    			excelProcess.process();
    		}
    	}
    	
        return ;
    }
    
    @RequestMapping("/topic/expall")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public void expall(ModelMap map , HttpServletRequest request , HttpServletResponse response,@Valid String type, @Valid String aiid) throws IOException {
    	Iterable<Topic> topicList = topicRes.getTopicByOrgi(super.getOrgi(request) ,type , null) ;
    	
    	MetadataTable table = metadataRes.findByTablename("uk_xiaoe_topic") ;
		List<Map<String,Object>> values = new ArrayList<Map<String,Object>>();
		for(Topic topic : topicList){
			values.add(UKTools.transBean2Map(topic)) ;
		}
		
		response.setHeader("content-disposition", "attachment;filename=UCKeFu-XiaoE-"+new SimpleDateFormat("yyyy-MM-dd").format(new Date())+".xls");  
		
		if(table!=null){
			ExcelExporterProcess excelProcess = new ExcelExporterProcess( values, table, response.getOutputStream()) ;
			excelProcess.process();
		}
        return ;
    }
    
    @RequestMapping("/topic/expsearch")
    @Menu(type = "xiaoe" , subtype = "knowledge")
    public void expall(ModelMap map , HttpServletRequest request , HttpServletResponse response , @Valid String q , @Valid String type, @Valid String aiid) throws IOException {
    	
    	Iterable<Topic> topicList = topicRes.getTopicByOrgi(super.getOrgi(request) , type , q) ;
    	
    	MetadataTable table = metadataRes.findByTablename("uk_xiaoe_topic") ;
		List<Map<String,Object>> values = new ArrayList<Map<String,Object>>();
		for(Topic topic : topicList){
			values.add(UKTools.transBean2Map(topic)) ;
		}
		
		response.setHeader("content-disposition", "attachment;filename=UCKeFu-XiaoE-"+new SimpleDateFormat("yyyy-MM-dd").format(new Date())+".xls");  
		
		if(table!=null){
			ExcelExporterProcess excelProcess = new ExcelExporterProcess( values, table, response.getOutputStream()) ;
			excelProcess.process();
		}
        return ;
    }
}