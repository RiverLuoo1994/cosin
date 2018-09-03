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
package com.chatopera.cc.webim.service.repository;

import java.util.List;

import com.chatopera.cc.webim.web.model.SipTrunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SipTrunkRepository extends JpaRepository<SipTrunk, String> {
	
	public SipTrunk findByIdAndOrgi(String id , String orgi);
	public List<SipTrunk> findByHostidAndOrgi(String hostid , String orgi);
	public List<SipTrunk> findByOrgi(String orgi);
	public int countByNameAndOrgi(String name, String orgi);
	
	public List<SipTrunk> findByName(String name);
	
	public List<SipTrunk> findByDefaultsipAndOrgi(boolean def, String orgi);
	
	public List<SipTrunk> findByDefaultsip(boolean def);
}
