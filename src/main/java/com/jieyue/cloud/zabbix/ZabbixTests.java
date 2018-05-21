/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zabbix4j.ZabbixApi;
import com.zabbix4j.ZabbixApiException;
import com.zabbix4j.history.HistoryGetRequest;
import com.zabbix4j.history.HistoryGetRequest.Params;
import com.zabbix4j.history.HistoryGetResponse;
import com.zabbix4j.history.HistoryObject;
import com.zabbix4j.host.HostGetRequest;
import com.zabbix4j.host.HostGetResponse;
import com.zabbix4j.host.HostObject;
import com.zabbix4j.hostinteface.HostInterfaceGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetResponse;
import com.zabbix4j.hostinteface.HostInterfaceObject;
import com.zabbix4j.item.ItemGetRequest;
import com.zabbix4j.item.ItemGetResponse;
import com.zabbix4j.item.ItemObject;

/**
 * @author Yueyang yancyyue@gmail.com
 *
 */
public class ZabbixTests {

	private ZabbixApi zabbixApi = null;
	private long start = 0;
	private Logger log = LogManager.getLogger(this.getClass());
	HistoryGetRequest historyReq = null;
	Params historyParamsGetLastOneOfCPULoad = null;
	Params historyParamsGetLastOneOfMEM = null;
	Params historyParamsGetAvgInaWeekOfCPULoad = null;

	@Before
	public void initTests() throws ZabbixApiException {

		String user = "admin";
		String password = "fVRFjcNkbDVs";
		// String password = "123456";

		start = System.currentTimeMillis();
		// login to zabbix
		zabbixApi = new ZabbixApi("http://172.16.101.17/zabbix/api_jsonrpc.php");
		// zabbixApi = new ZabbixApi("http://172.18.100.227/zabbix/api_jsonrpc.php");
		zabbixApi.login(user, password);
		log.info("Test Start.");

	}

	@After
	public void afterTests() {
		long end = System.currentTimeMillis();
		log.info("Time Cost:" + (int) ((end - start) / 1000) + " seconds");
	}

	@Test
	public void testGetHostidSByHostnames() {
		HostGetRequest hostReq = new HostGetRequest();
		HostGetRequest.Params params = hostReq.getParams();
		params.setOutput("extend");
		Map<String, String> filter = new HashMap<String, String>();
//		filter.put("host", "Zabbix");
		filter.put("host", "VMserver13");
		
		 params.setSearch(filter);
		 params.setLimit(5);
		log.debug(hostReq.getParams());
		HostGetResponse hostResp = null;
		try {
			hostResp = zabbixApi.host().get(hostReq);
		} catch (ZabbixApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("HostRequest result size:" + hostResp.getResult().size());
		int i = 0;
		for (HostObject host : hostResp.getResult()) {
			i++;
			log.info("Hostlist[" + i + "]:" + host.getName() + "/" + host.getHostid());
		}
	}

	@Test
	// get interfaces of hosts
	public void testGetHostifsByHostnames() throws ZabbixApiException {
		HostGetRequest hostReq = new HostGetRequest();
		com.zabbix4j.host.HostGetRequest.Params params = hostReq.getParams();
		params.setLimit(10);
		HostGetResponse hostResp = zabbixApi.host().get(hostReq);
		HostInterfaceGetRequest hostifReq = new HostInterfaceGetRequest();

		HostInterfaceGetResponse hostifResp = null;
		List hostIdList = new ArrayList();
		int i = 0;
		for (Iterator itr = hostResp.getResult().iterator(); itr.hasNext();) {
			HostObject host = (HostObject) itr.next();
			i++;
			log.info(i + ":" + host.getName() + "/" + host.getHostid());
			hostIdList.add(host.getHostid());
		}
		com.zabbix4j.hostinteface.HostInterfaceGetRequest.Params ifparams = hostifReq.getParams();
		ifparams.setHostids(hostIdList);
		hostifReq.setParams(ifparams);
		hostifResp = zabbixApi.hostInterface().get(hostifReq);
		i = 0;
		for (HostInterfaceObject hostif : hostifResp.getResult()) {
			i++;
			log.info(i + ":" + hostif.getHostid() + "/" + hostif.getIp());
		}
	}

	@Test
	public void testGetItemidSByHostid() throws ZabbixApiException {
		ItemGetRequest itemReq = new ItemGetRequest();
		com.zabbix4j.item.ItemGetRequest.Params params = itemReq.getParams();
		// params.setHost("VMserver13");
		List hostIdList = new ArrayList();
		hostIdList.add(10112);
		// hostIdList.add(10653);
		// hostIdList.add(10635);
		params.setHostids(hostIdList);
		Map<String, String> filter = new HashMap<String, String>();
		// filter.put("key_", "idle");
		// filter.put("key_", "cpu");
		filter.put("key_", "system.cpu.util[,idle]");
		// filter.put("name", "CPU idle time");
		params.setSearch(filter);
		ItemGetResponse itemResp = zabbixApi.item().get(itemReq);
		log.info("Item List Size:" + itemResp.getResult().size());
		for (ItemObject item : itemResp.getResult()) {
			log.info("key/name/itemid: " + item.getKey_() + " / " + item.getName() + " / " + item.getItemid());
		}
	}

	@Test
	public void testGetValueByItem() throws ZabbixApiException {

		// set search
		// ItemGetRequest req = new ItemGetRequest();
		HistoryGetRequest historyReq = new HistoryGetRequest();

		Params params = historyReq.getParams();
		// String key = "web.test.in[f95b885a65,f95b885a65,bps]";
		// List<Integer> hostids = new ArrayList<>();
		// hostids.add(11785);
		// params.setHostids(hostids);
		// params.setWebitems(true);
		List<Integer> itemids = new ArrayList<>();
		itemids.add(30652);
		params.setItemids(itemids);
		params.setLimit(1);
		params.setHistory(0);
		params.setSortField("clock");
		params.setSortorder(ZabbixUtils.getSortInDesc());
		params.setTime_from(ZabbixUtils.getCurrentUnixTimeStamp() - 60 * 60 * 24 * 14);
		params.setTime_till(ZabbixUtils.getCurrentUnixTimeStamp());

		// Map<String, String> search = new HashMap<String,String>();
		// search.put("key_", key);
		// params.setSearch(search);
		HistoryGetResponse resp = zabbixApi.history().get(historyReq);
		log.info(resp.getId());
		log.info("History result size:" + resp.getResult().size());
		int i = 0;
		for (HistoryObject history : resp.getResult()) {
			i++;
			if (history == null) {
				continue;
			}
			log.info(i + "/Clock/Value:" + ZabbixUtils.unixTimeStamp2Date(history.getClock()) + "/"
					+ history.getValue());
		}
	}

}
