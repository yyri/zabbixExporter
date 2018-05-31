/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * @author Yueyang yancyyue@gmail.com
 *
 */
public class ZabbixObject {

	private Logger log = LogManager.getLogger(this.getClass());

	private Integer hostid = -1;
	private String hostname = "";
	private List<String> hostips = new ArrayList();
	private String cpuPerf_cur = "";
	private Integer cpuPerf_cur_itemid = 0;
	private Integer cpuPerf_samplesCount = 0;
	private Float cpuPerf_max = (float) 0;
	private Float cpuPerf_min = (float) 0;

	private Integer memTotal = 0;
	private Integer memAvailable = 0;
	private Integer memTotalItemID = 0;
	private Integer memAvailableItemID = 0;

	DecimalFormat fnum = new DecimalFormat("#.##%");

	private static int RatesOfBytesToGiB = 1024 * 1024 * 1024;

	/**
	 * @return the memTotalItemID
	 */
	public Integer getMemTotalItemID() {
		return memTotalItemID;
	}

	/**
	 * @param memTotalItemID
	 *            the memTotalItemID to set
	 */
	public void setMemTotalItemID(Integer memTotalItemID) {
		this.memTotalItemID = memTotalItemID;
	}

	/**
	 * @return the memAvailableItemID
	 */
	public Integer getMemAvailableItemID() {
		return memAvailableItemID;
	}

	/**
	 * @param memAvailableItemID
	 *            the memAvailableItemID to set
	 */
	public void setMemAvailableItemID(Integer memAvailableItemID) {
		this.memAvailableItemID = memAvailableItemID;
	}

	/**
	 * @return the memTotal
	 */
	public Integer getMemTotal() {
		return memTotal;
	}

	/**
	 * @param memTotal
	 *            the memTotal to set
	 */
	public void setMemTotal(String memTotal) {
		try {
			this.memTotal = (int) ((new Long(memTotal)) / RatesOfBytesToGiB);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			this.memTotal = 0;
		}
	}

	/**
	 * @return the memAvailable
	 */
	public Integer getMemAvailable() {
		return memAvailable;
	}

	/**
	 * @param memAvailable
	 *            the memAvailable to set
	 */
	public void setMemAvailable(Integer memAvailable) {
		this.memAvailable = memAvailable;
	}

	public void setMemAvailable(String memAvailable) {

		try {
			Long tmp = (new Long(memAvailable)) / RatesOfBytesToGiB;
			this.memAvailable = tmp.intValue();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			this.memAvailable = 0;
		}
	}

	/**
	 * @return the cpuPerf_cur_itemid
	 */
	public Integer getCpuPerf_cur_itemid() {
		return cpuPerf_cur_itemid;
	}

	/**
	 * @param cpuPerf_cur_itemid
	 *            the cpuPerf_cur_itemid to set
	 */
	public void setCpuPerf_cur_itemid(Integer avg15_cur_itemid) {
		this.cpuPerf_cur_itemid = avg15_cur_itemid;
	}

	public String[] toArray() {
		// log.debug("hostname:" + hostname);
		// log.debug("hostid:" + hostid.toString());
		// log.debug("hostips:" + hostips.toString());
		// log.debug("cpuPerf_cur:" + cpuPerf_cur);
		// log.debug("avg15_2hours:" + avg15_2hours);

		return new String[] { hostname, hostid.toString(), hostips.toString(), cpuPerf_cur, cpuPerf_max.toString(),
				cpuPerf_min.toString(), cpuPerf_samplesCount.toString(), memAvailable.toString(), memTotal.toString(),
				getUsedMemPercent() };
	}

	private String getUsedMemPercent() {
		if (memAvailable == 0 || memTotal == 0) {
			return "";
		}
		return fnum.format((float) (memTotal - memAvailable) / memTotal);

	}

	@Test
	public void testDecimalFormat() {
		this.memTotal = 7;
		this.memAvailable = 6;
		log.debug(fnum.format((float) (memTotal - memAvailable) / memTotal));
	}

	/**
	 * @return the hostid
	 */
	public Integer getHostid() {
		return hostid;
	}

	/**
	 * @param hostid
	 *            the hostid to set
	 */
	public void setHostid(Integer hostid) {
		this.hostid = hostid;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @param hostname
	 *            the hostname to set
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * @return the hostip
	 */
	public List<String> getHostip() {
		return hostips;
	}

	/**
	 * @param hostip
	 *            the hostip to set
	 */
	public void setHostips(List<String> hostips) {
		this.hostips = hostips;
	}

	public void addHostip(String hostip) {
		if (this.hostips == null) {
			this.hostips = new ArrayList<String>();
		}
		this.hostips.add(hostip);
	}

	/**
	 * @return the cpuPerf_cur
	 */
	public String getCpuPerf_cur() {
		return cpuPerf_cur;
	}

	/**
	 * @param cpuPerf_cur
	 *            the cpuPerf_cur to set
	 */
	public void setCpuPerf_cur(String avg15_cur) {
		this.cpuPerf_cur = avg15_cur;
	}

	/**
	 * @return the cpuPerf_samplesCount
	 */
	public Integer getCpuPerf_samplesCount() {
		return cpuPerf_samplesCount;
	}

	/**
	 * @param cpuPerf_samplesCount
	 *            the cpuPerf_samplesCount to set
	 */
	public void setCpuPerf_samplesCount(Integer avg15_samplesCount) {
		this.cpuPerf_samplesCount = avg15_samplesCount;
	}

	/**
	 * find ZabbixObject contains the itemid in given zabbixStore
	 * 
	 * @param zabbixStore
	 * @param itemid
	 * @return
	 */
	public static ZabbixObject findZabbixObjectByItemId(Map zabbixStore, Integer itemid) {
		for (Object zsKey : zabbixStore.keySet()) {
			ZabbixObject zo = (ZabbixObject) zabbixStore.get(zsKey);
			if (zo.getCpuPerf_cur_itemid().equals(itemid) || zo.getMemAvailableItemID().equals(itemid)
					|| zo.getMemTotalItemID().equals(itemid)) {
				return zo;
			}
		}
		return null;
	}

	/**
	 * @return the cpuPerf_max
	 */
	public Float getCpuPerf_max() {
		return cpuPerf_max;
	}

	/**
	 * @param cpuPerf_max
	 *            the cpuPerf_max to set
	 */
	public void setCpuPerf_max(Float cpuPerf_max) {
		this.cpuPerf_max = cpuPerf_max;
	}

	/**
	 * @return the cpuPerf_min
	 */
	public Float getCpuPerf_min() {
		return cpuPerf_min;
	}

	/**
	 * @param cpuPerf_min
	 *            the cpuPerf_min to set
	 */
	public void setCpuPerf_min(Float cpuPerf_min) {
		this.cpuPerf_min = cpuPerf_min;
	}
}
