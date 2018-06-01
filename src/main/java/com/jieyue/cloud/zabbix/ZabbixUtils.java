/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.csvreader.CsvWriter;
import com.zabbix4j.GetRequestCommonParams;
import com.zabbix4j.ZabbixApi;
import com.zabbix4j.ZabbixApiException;
import com.zabbix4j.history.HistoryGetRequest;
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
public class ZabbixUtils {
	private static Logger log = LogManager.getLogger(com.jieyue.cloud.zabbix.ZabbixUtils.class);

	public static String memorySearchKey = "memory";
	public static String memFreeName = "Free memory";
	public static String memTotalName = "Total memory";
	public static String memAvailableName = "Available memory";
	
	public static String cpuavg15SearchKey = "cpu";
	public static String cpuavg15SearchName = "Processor load (1 min average per core)";
	public static String cpuIdleTimeSearchKey = "system.cpu.util";
	public static String cpuIdleTimeKeyofLinux = "system.cpu.util[,idle]";
	public static String cpuIdleTimeKeyofWin = "system.cpu.util[,,]";
	public static String cpuUserTimeSearchKey = "system.cpu.util";
	public static String cpuUserTimeKeyofLinux = "system.cpu.util[,user]";
	public static String cpuUserTimeKeyofWin = "system.cpu.util[,,]";

	public static String datetimeFormat = "yyyy-MM-dd HH:mm:ss";
	public static DecimalFormat fnum = new DecimalFormat("##0.0000");
	public static SimpleDateFormat sdf = new SimpleDateFormat(datetimeFormat, Locale.CHINA);

	private static List sortInDesc = null;
	private static List sortInAsc = null;
	private static long secondsOfADay = 60 * 60 * 24;

	// 2017-11-11 00:00:00 1510329600 in SECONDs
	// 2017-11-12 00:00:00 1510416000 in SECONDs
	// 2017-12-12 00:00:00 1513008000
	// 2017-12-13 00:00:00 1513094400
	// 2018-01-01 00:00:00 1514736000
	// 2018-01-01 00:00:00 1514736000
	// 2018-01-02 00:00:00 1514822400

	public static enum QueryPeroid {

		TODAY("today", 0, 0), ThreeDaySAgo("3daysago", 0, 0), SevenDaysAgo("7daysago", 0, 0), ThisWeek("ThisWeek", 0,
				0), Double11("double11", 1510329600, 1510416000), Double12("double12", 1513008000,
						1513094400), NewYear("newyear", 1514736000, 1514822400);

		private String peroidKey;
		private long from;
		private long till;

		private QueryPeroid(String peroidKey, long from, long till) {
			this.peroidKey = peroidKey;
			this.from = from;
			this.till = till;
		}

		/**
		 * @return the peroidKey
		 */
		public String getPeroidKey() {
			return peroidKey;
		}

		/**
		 * @param peroidKey
		 *            the peroidKey to set
		 */
		public void setPeroidKey(String peroidKey) {
			this.peroidKey = peroidKey;
		}

		/**
		 * @return the from
		 */
		public long getFrom() {
			return from;
		}

		/**
		 * @param from
		 *            the from to set
		 */
		public void setFrom(long from) {
			this.from = from;
		}

		/**
		 * @return the till
		 */
		public long getTill() {
			return till;
		}

		/**
		 * @param till
		 *            the till to set
		 */
		public void setTill(long till) {
			this.till = till;
		}

	}

	static {
		// init today,3daysago,7daysago timestamps
		long current = System.currentTimeMillis() / 1000;
		ZabbixUtils.QueryPeroid.TODAY.from = current - secondsOfADay;
		ZabbixUtils.QueryPeroid.TODAY.till = current;
		ZabbixUtils.QueryPeroid.ThreeDaySAgo.from = current - secondsOfADay * 4;
		ZabbixUtils.QueryPeroid.ThreeDaySAgo.till = current - secondsOfADay * 3;
		ZabbixUtils.QueryPeroid.SevenDaysAgo.from = current - secondsOfADay * 8;
		ZabbixUtils.QueryPeroid.SevenDaysAgo.till = current - secondsOfADay * 7;
		ZabbixUtils.QueryPeroid.ThisWeek.from = current - secondsOfADay * 7;
		ZabbixUtils.QueryPeroid.ThisWeek.till = current;

	}

	/**
	 * 
	 * @param dateInStr
	 *            "yyyy-MM-dd HH:mm:ss"
	 * @return
	 * @throws ParseException
	 */
	public static Long date2Timestamp(String dateInStr) throws ParseException {
		return ZabbixUtils.sdf.parse(dateInStr).getTime();
	}

	public static Long getCurrentUnixTimeStamp() {
		return System.currentTimeMillis() / 1000;
	}

	public static String unixTimeStamp2Date(String timestampString) {
		Long timestamp = Long.parseLong(timestampString) * 1000;
		String date = ZabbixUtils.sdf.format(new Date(timestamp));
		return date;
	}

	public static String unixTimeStamp2Date(Long timestamp) {

		String date = ZabbixUtils.sdf
				.format(new Date(timestamp.toString().length() > 10 ? timestamp : timestamp * 1000));
		return date;
	}

	/**
	 * @return the sortInDesc
	 */
	public static List getSortInDesc() {
		if (sortInDesc == null) {
			sortInDesc = new ArrayList();
			sortInDesc.add("DESC");
		}
		return sortInDesc;
	}

	/**
	 * @param sortInDesc
	 *            the sortInDesc to set
	 */
	public static void setSortInDesc(List sortInDesc) {
		ZabbixUtils.sortInDesc = sortInDesc;
	}

	/**
	 * @return the sortInAsc
	 */
	public static List getSortInAsc() {
		if (sortInAsc == null) {
			sortInAsc = new ArrayList();
			sortInAsc.add("ASC");
		}
		return sortInAsc;
	}

	/**
	 * @param sortInAsc
	 *            the sortInAsc to set
	 */
	public static void setSortInAsc(List sortInAsc) {
		ZabbixUtils.sortInAsc = sortInAsc;
	}

	/**
	 * @throws IOException
	 */
	public static void exportToCsv(Map<Integer, ZabbixObject> zabbixStore, String fileName) throws IOException {
		// 创建CSV写对象
		CsvWriter csvWriter = new CsvWriter(fileName, ',', Charset.forName("GBK"));

		// 写表头
		String[] headers = { "HostName", "HostID", "HostIP", "CPU UserTime Avg", "CPU UserTime Max", "CPU UserTime Min",
				"CPU UserTime Samples Count", "MemAvailable(GB)", "MemTotal(GB)", "UsedMem%" };
		// , "15avg-2hours", "15avgMax-2hours", "15avgMin-2hours"
		csvWriter.writeRecord(headers);
		for (Integer itr : zabbixStore.keySet()) {
			ZabbixObject zo = (ZabbixObject) zabbixStore.get(itr);
			csvWriter.writeRecord(zo.toArray());
		}
		csvWriter.close();
	}

	/**
	 * @param zabbixStore
	 * @param hostIdList
	 * @param itemids
	 * @param filter
	 * @throws ZabbixApiException
	 */
	public static void getCpuPerfItemid(ZabbixApi zabbixApi, Map<Integer, ZabbixObject> zabbixStore, List hostIdList,
			List<Integer> itemids, Map<String, String> filter, ItemGetRequest itemReq) throws ZabbixApiException {
		int i = 0;
		com.zabbix4j.item.ItemGetRequest.Params itemParams = itemReq.getParams();
		itemParams.setHostids(hostIdList);
		itemParams.setSearch(filter);
		itemReq.setParams(itemParams);
		ItemGetResponse itemResp = zabbixApi.item().get(itemReq);
		log.info("CpuPerf Itemids Result Size:" + itemResp.getResult().size());
		for (ItemObject item : itemResp.getResult()) {
			i++;
			if (item.getKey_().equals(ZabbixUtils.cpuUserTimeKeyofLinux)
					|| item.getKey_().equals(ZabbixUtils.cpuUserTimeKeyofWin)) {
				log.debug(i + ":" + "HostID/Name/ItemID:" + item.getHostid() + "/" + item.getName() + "/"
						+ item.getItemid());
				ZabbixObject zo = (ZabbixObject) zabbixStore.get(item.getHostid());
				zo.setCpuPerf_cur_itemid(item.getItemid());
				itemids.add(item.getItemid());
				zabbixStore.put(item.getHostid(), zo);
			}
		}
	}

	public static void getMemItemids(ZabbixApi zabbixApi, Map<Integer, ZabbixObject> zabbixStore, List hostIdList,
			List<Integer> itemids, Map<String, String> filter, ItemGetRequest itemReq) throws ZabbixApiException {
		int i = 0;
		com.zabbix4j.item.ItemGetRequest.Params itemParams = itemReq.getParams();
		itemParams.setHostids(hostIdList);
		itemParams.setSearch(filter);
		itemReq.setParams(itemParams);
		ItemGetResponse itemResp = zabbixApi.item().get(itemReq);
		log.info("Mem Itemids Result Size:" + itemResp.getResult().size());

		for (ItemObject item : itemResp.getResult()) {
			i++;
			log.info(
					i + ":" + "HostID/Name/ItemID:" + item.getHostid() + "/" + item.getName() + "/" + item.getItemid());
			ZabbixObject zo = (ZabbixObject) zabbixStore.get(item.getHostid());
			if (item.getName().equals(ZabbixUtils.memAvailableName)) {
				zo.setMemAvailableItemID(item.getItemid());
			} else if (item.getName().equals(ZabbixUtils.memTotalName)) {
				zo.setMemTotalItemID(item.getItemid());
			} else if (item.getName().equals(ZabbixUtils.memFreeName)) {
				zo.setMemFreeItemID(item.getItemid());
			}
			itemids.add(item.getItemid());
			zabbixStore.put(item.getHostid(), zo);
		}
	}

	/**
	 * @param zabbixStore
	 * @param hostIdList
	 * @param hostifReq
	 * @param ifparams
	 * @throws ZabbixApiException
	 */
	public static void getHostInterfaces(ZabbixApi zabbixApi, Map<Integer, ZabbixObject> zabbixStore, List hostIdList,
			HostInterfaceGetRequest hostifReq, com.zabbix4j.hostinteface.HostInterfaceGetRequest.Params ifparams)
			throws ZabbixApiException {
		HostInterfaceGetResponse hostifResp;
		int i;
		ifparams.setHostids(hostIdList);
		hostifReq.setParams(ifparams);
		hostifResp = zabbixApi.hostInterface().get(hostifReq);
		i = 0;
		for (HostInterfaceObject hostif : hostifResp.getResult()) {
			i++;
			log.info(i + ":" + hostif.getHostid() + "/" + hostif.getIp());
			ZabbixObject zo = (ZabbixObject) zabbixStore.get(hostif.getHostid());
			zo.addHostip(hostif.getIp());
			zabbixStore.put(hostif.getHostid(), zo);
		}
	}

	/**
	 * @param zabbixStore
	 * @param hostIdList
	 * @throws ZabbixApiException
	 */
	public static void getHostList(ZabbixApi zabbixApi, Map<Integer, ZabbixObject> zabbixStore, List hostIdList,
			int limit, HostGetRequest hostReq) throws ZabbixApiException {
		HostGetRequest.Params params = hostReq.getParams();
		// Map<String, String> filter = new HashMap<String, String>();
		params.setLimit(limit);
		hostReq.setParams(params);
		HostGetResponse hostResp = zabbixApi.host().get(hostReq);

		int i = 0;
		for (HostObject host : hostResp.getResult()) {
			i++;
			log.info(i + ":" + host.getName() + "/" + host.getHostid());
			hostIdList.add(host.getHostid());

			ZabbixObject zo = new ZabbixObject();
			zo.setHostid(host.getHostid());
			zo.setHostname(host.getName());
			zabbixStore.put(host.getHostid(), zo);
		}
	}

	/**
	 * 
	 * @param zabbixApi
	 * @param sn
	 * @param zabbixStore
	 * @param itemid
	 * @param historyReq
	 * @param average
	 * @throws ZabbixApiException
	 */
	public void getValueByItemId(ZabbixApi zabbixApi, int sn, Map<Integer, ZabbixObject> zabbixStore, Integer itemid,
			HistoryGetRequest historyReq, boolean average) throws ZabbixApiException {
		List itemids = new ArrayList();
		itemids.add(itemid);
		historyReq.getParams().setItemids(itemids);
		// historyReq.setParams(historyParams);

		HistoryGetResponse historyResp = zabbixApi.history().get(historyReq);
		// log.info(historyResp.getId());
		log.info("Quering Itemids:" + itemids.toString() + " History Result Size:" + historyResp.getResult().size());
		List<Float> itemValueList = new ArrayList<Float>();
		for (HistoryObject history : historyResp.getResult()) {
			if (history == null) {
				continue;
			}
			log.debug(sn + ":/Clock/ItemID/Value:" + history.getClock() + "/" + history.getItemid() + "/"
					+ history.getValue());
			ZabbixObject zo = ZabbixObject.findZabbixObjectByItemId(zabbixStore, history.getItemid());
			if (zo == null) {
				log.debug("ItemID " + history.getItemid() + " matched No ZabbixObject.");
				continue;
			}
			if (zo.getCpuPerf_cur_itemid() != null && history.getItemid().equals(zo.getCpuPerf_cur_itemid())) {
				float curSum = 0;
				float maxCpuPerf = 0;
				float minCpuPerf = 9999;
				if (average) {
					// iterator CPU performance history and calculate AVG
					itemValueList.add(Float.parseFloat(history.getValue()));
					int count = 0;
					for (Float itemValue : itemValueList) {
						count++;
						curSum += itemValue;
						maxCpuPerf = itemValue > maxCpuPerf ? itemValue : maxCpuPerf;
						minCpuPerf = itemValue > minCpuPerf ? minCpuPerf : itemValue;
					}
					zo.setCpuPerf_cur(ZabbixUtils.fnum.format(curSum / count));
					zo.setCpuPerf_samplesCount(count);
					zo.setCpuPerf_max(maxCpuPerf);
					zo.setCpuPerf_min(minCpuPerf);
				} else {
					zo.setCpuPerf_cur(history.getValue());
				}
			} else if (zo.getMemAvailableItemID() != null && history.getItemid().equals(zo.getMemAvailableItemID())) {
				zo.setMemAvailable(history.getValue());
			} else if (zo.getMemTotalItemID() != null && history.getItemid().equals(zo.getMemTotalItemID())) {
				zo.setMemTotal(history.getValue());
			} else if (zo.getMemFreeItemID() != null && history.getItemid().equals(zo.getMemFreeItemID())) {
				zo.setMemFree(history.getValue());
			}
			zabbixStore.put(zo.getHostid(), zo);
		}
	}

	/**
	 * set time period of query params
	 * 
	 * @param params
	 * @param peroid
	 */

	public static void setTimePeriod(HistoryGetRequest.Params params, QueryPeroid peroid) {
		params.setTime_from(peroid.from);
		params.setTime_till(peroid.till);
	}

	@Test
	public void getTodayPeroid() {
		System.out.println(ZabbixUtils.QueryPeroid.TODAY.from);
	}

	/**
	 * generate time series by given interval. like interval by an hour or half an
	 * hour or 10 minutes to reduce load on the zabbix server and the request
	 * duration.
	 * 
	 * @param zabbixApi
	 * @param historyReq
	 */
	public static void getTimeSeries(ZabbixApi zabbixApi, HistoryGetRequest historyReq, int interval) {

		// get the last time point by request the last one item order by clock.

		// PROBLEM the sample item clock maybe not accurate.
	}

	/**
	 * 
	 * @param historyReq
	 * @param history
	 *            0 - numeric float; 1 - character; 2 - log; 3 - numeric unsigned; 4
	 *            - text. Default: 3.
	 * @param limit
	 * @param orderField
	 * @param order
	 * @param peroid
	 */
	public static void setHistoryReqParams(HistoryGetRequest historyReq, int history, int limit, String orderField,
			List order, ZabbixUtils.QueryPeroid peroid) {
		historyReq.getParams().setOutput("extend");
		historyReq.getParams().setHistory(history);
		historyReq.getParams().setLimit(limit);
		historyReq.getParams().setSortField(orderField);
		historyReq.getParams().setSortorder(order);
		ZabbixUtils.setTimePeriod(historyReq.getParams(), peroid);
	}

	@Test
	public void testDateStrToTS() throws ParseException {
		log.debug(ZabbixUtils.date2Timestamp("2018-06-01 08:39:00"));
		log.debug(System.currentTimeMillis());
		log.debug(ZabbixUtils.unixTimeStamp2Date(System.currentTimeMillis()));
	}
}
