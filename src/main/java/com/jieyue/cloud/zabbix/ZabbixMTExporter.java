/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.zabbix4j.ZabbixApi;
import com.zabbix4j.ZabbixApiException;
import com.zabbix4j.history.HistoryGetRequest;
import com.zabbix4j.host.HostGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetResponse;
import com.zabbix4j.item.ItemGetRequest;

import javafx.concurrent.Task;

/**
 * @author Yueyang yancyyue@gmail.com
 *
 */
public class ZabbixMTExporter {

	// Runtime Configuration
	private static int hostsLimit = 5000;
	// How many values will be fetched of a monitor item.1 item every minute.1440
	// item for one day.
	private static int itemValueCountLimit = 2000;

	private static ZabbixApi zabbixApi = null;
	private static long start = 0;
	private static Logger log = LogManager.getLogger(ZabbixMTExporter.class);
	private static String dataFileName = "c:/downloads/ZabbixData";

	private static String user = "admin";

	// 鹏博生产
	private static String zabbixName = "-Prod-";
	private static String zabbixUrl = "http://172.16.101.17/zabbix/api_jsonrpc.php";
	private static String password = "fVRFjcNkbDVs";
	// 鹏博士准生产
//	private static String zabbixName = "-Staging-";
//	private static String zabbixUrl = "http://172.18.100.227/zabbix/api_jsonrpc.php";
//	private static String password = "123456";

	/**
	 * @param args
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws ZabbixApiException
	 * @throws IOException
	 */
	public static void main(String[] args)
			throws InterruptedException, ExecutionException, ZabbixApiException, IOException {

		HistoryGetRequest historyReqofCPU = new HistoryGetRequest();
		HistoryGetRequest historyReqofMEM = new HistoryGetRequest();

		ZabbixUtils.setHistoryReqParams(historyReqofCPU, 0, itemValueCountLimit, "clock", ZabbixUtils.getSortInDesc(),
				ZabbixUtils.QueryPeroid.TODAY);
		ZabbixUtils.setHistoryReqParams(historyReqofMEM, 3, itemValueCountLimit, "clock", ZabbixUtils.getSortInDesc(),
				ZabbixUtils.QueryPeroid.TODAY);

		start = System.currentTimeMillis();
		log.info("Exporting start.");

		// login to zabbix
		zabbixApi = new ZabbixApi(zabbixUrl);
		zabbixApi.login(user, password);

		// global data objects
		Map<Integer, ZabbixObject> zabbixStore = new HashMap();
		List hostIdList = new ArrayList();
		List<Integer> cpuloadItemids = new ArrayList<>();
		List<Integer> memItemids = new ArrayList<>();

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

		ExecutorService executor = Executors.newFixedThreadPool(5);
		List<Future<String>> res = new LinkedList<>();

		// get cpuLoad item values
		for (Integer cpuPerfItemid : cpuloadItemids) {
			i++;
			historyReqofCPU = new HistoryGetRequest();
			ZabbixUtils.setHistoryReqParams(historyReqofCPU, 0, itemValueCountLimit, "clock",
					ZabbixUtils.getSortInDesc(), ZabbixUtils.QueryPeroid.TODAY);
			Future future = executor.submit(
					new ZabbixHistoryGetThread(zabbixApi, historyReqofCPU, zabbixStore, i, cpuPerfItemid, true));
			res.add(future);
		}

		// get mem item values
		i = 0;
		for (Integer memItemId : memItemids) {
			i++;
			historyReqofMEM = new HistoryGetRequest();
			ZabbixUtils.setHistoryReqParams(historyReqofMEM, 3, itemValueCountLimit, "clock",
					ZabbixUtils.getSortInDesc(), ZabbixUtils.QueryPeroid.TODAY);
			Future future = executor
					.submit(new ZabbixHistoryGetThread(zabbixApi, historyReqofMEM, zabbixStore, i, memItemId, true));
			res.add(future);
		}

		executor.shutdown();

		while (!executor.isTerminated()) {
			Thread.sleep(1000);
		}

		ZabbixUtils.writeCsv(zabbixStore, dataFileName + zabbixName + System.currentTimeMillis() + ".csv");

		log.info("All threads exit.\r\nMain thread exting...");
		long end = System.currentTimeMillis();
		log.info("Time Cost:" + (int) ((end - start) / 1000) + " seconds.");
	}

}
