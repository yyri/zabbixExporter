/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.zabbix4j.host.HostGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetResponse;
import com.zabbix4j.item.ItemGetRequest;

/**
 * @author Yueyang yancyyue@gmail.com
 *
 */
public class ZabbixExporter {

	// Runtime Configuration
	private int hostsLimit = 5;
	// How many values will be fetched of a monitor item
	private int itemValueCountLimit = 100;

	private ZabbixApi zabbixApi = null;
	private long start = 0;
	private Logger log = LogManager.getLogger(this.getClass());
	private String dataFileName = "c:/downloads/ZabbixData.csv";

	private String user = "admin";
	// 鹏博生产
	// private String password = "fVRFjcNkbDVs";
	// 鹏博士准生产
	private String password = "123456";
	// 鹏博士准生产
	private String zabbixUrl = "http://172.18.100.227/zabbix/api_jsonrpc.php";
	// 鹏博士生产
	// private String zabbixUrl = "http://172.16.101.17/zabbix/api_jsonrpc.php";

	HistoryGetRequest historyReqofCPU = new HistoryGetRequest();
	HistoryGetRequest historyReqofMEM = new HistoryGetRequest();

	@Before
	public void initTests() throws ZabbixApiException {

		setHistoryReqParams(historyReqofCPU, 0, itemValueCountLimit, "clock", ZabbixUtils.getSortInDesc(),
				ZabbixUtils.QueryPeroid.TODAY);
		setHistoryReqParams(historyReqofMEM, 3, itemValueCountLimit, "clock", ZabbixUtils.getSortInDesc(),
				ZabbixUtils.QueryPeroid.TODAY);

		start = System.currentTimeMillis();
		log.info("Exporting start.");

		// login to zabbix
		zabbixApi = new ZabbixApi(zabbixUrl);
		zabbixApi.login(user, password);
	}

	@Test
	public void exportMonData() {
		Map<Integer, ZabbixObject> zabbixStore = new HashMap();
		List hostIdList = new ArrayList();
		List<Integer> cpuloadItemids = new ArrayList<>();
		List<Integer> memItemids = new ArrayList<>();

		try {
			// get hostlist
			int i = 0;
			HostGetRequest hostReq = new HostGetRequest();
			ZabbixUtils.getHostList(zabbixApi, zabbixStore, hostIdList, hostsLimit, hostReq);

			// get host interfaces
			HostInterfaceGetRequest hostifReq = new HostInterfaceGetRequest();
			HostInterfaceGetResponse hostifResp = null;
			com.zabbix4j.hostinteface.HostInterfaceGetRequest.Params ifparams = hostifReq.getParams();
			ZabbixUtils.getHostInterfaces(zabbixApi, zabbixStore, hostIdList, hostifReq, ifparams);

			// get itemid of cpu utilization
			ItemGetRequest itemReq = new ItemGetRequest();
			Map<String, String> filter = new HashMap<String, String>();
			// filter.put("key_", ZabbixUtils.cpuavg15SearchKey);
			filter.put("key_", ZabbixUtils.cpuIdleTimeSearchKey);
			// filter.put("name", ZabbixUtils.cpuavg15SearchName);
			ZabbixUtils.getCpuPerfItemid(zabbixApi, zabbixStore, hostIdList, cpuloadItemids, filter, itemReq);

			// get itemid of memTotal and memAva
			filter = new HashMap<String, String>();
			filter.put("key_", ZabbixUtils.memorySearchKey);
			ZabbixUtils.getMemItemids(zabbixApi, zabbixStore, hostIdList, memItemids, filter, itemReq);

			// get cpuLoad item values
			for (Integer cpuPerfItemid : cpuloadItemids) {
				i++;
				ZabbixUtils.getValueByItemId(zabbixApi, i, zabbixStore, cpuPerfItemid, historyReqofCPU, true);
				for (Integer zsKey : zabbixStore.keySet()) {
					ZabbixObject zo = (ZabbixObject) zabbixStore.get(zsKey);
					log.info(zo.toArray().toString());
				}
			}

			// get mem item values
			i = 0;
			for (Integer memItemId : memItemids) {
				i++;
				ZabbixUtils.getValueByItemId(zabbixApi, i, zabbixStore, memItemId, historyReqofMEM, false);
			}

			ZabbixUtils.writeCsv(zabbixStore, dataFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@After
	public void afterTests() {
		long end = System.currentTimeMillis();
		log.info("Time Cost:" + (int) ((end - start) / 1000) + " seconds.");
	}

	/**
	 * 
	 * @param historyReq
	 * @param history
	 * @param limit
	 * @param orderField
	 * @param order
	 * @param peroid
	 */
	private void setHistoryReqParams(HistoryGetRequest historyReq, int history, int limit, String orderField,
			List order, ZabbixUtils.QueryPeroid peroid) {
		historyReq.getParams().setOutput("extend");
		historyReq.getParams().setHistory(history);
		historyReq.getParams().setLimit(limit);
		historyReq.getParams().setSortField(orderField);
		historyReq.getParams().setSortorder(order);
		ZabbixUtils.setTimePeriod(historyReq.getParams(), peroid);
	}
}
