/**
 * 
 */
package com.jieyue.cloud.zabbix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.zabbix4j.ZabbixApi;
import com.zabbix4j.history.HistoryGetRequest;
import com.zabbix4j.host.HostGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetRequest;
import com.zabbix4j.hostinteface.HostInterfaceGetResponse;
import com.zabbix4j.item.ItemGetRequest;

/**
 * @author Yueyang yancyyue@gmail.com
 *
 */
public class ZabbixHistoryGetThread implements Runnable {
	private Logger log = LogManager.getLogger(this.getClass());

	ZabbixApi zabbixApi = null;
	HistoryGetRequest hgr = null;
	Map<Integer, ZabbixObject> zabbixStore = null;
	int itemid = 0;
	int sn = 0;
	boolean doAverage = false;

	ZabbixHistoryGetThread(ZabbixApi inZabbixApi, HistoryGetRequest inHgr, Map<Integer, ZabbixObject> inZabbixStore,
			int inSn, int inItemid, boolean inDoAverage) {
		zabbixApi = inZabbixApi;
		hgr = inHgr;
		zabbixStore = inZabbixStore;
		sn = inSn;
		itemid = inItemid;
		doAverage = inDoAverage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		log.debug(Thread.currentThread().getName() + " is running." + System.currentTimeMillis());
		try {
			// get itemid values
			new ZabbixUtils().getValueByItemId(zabbixApi, sn, zabbixStore, itemid, hgr, doAverage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	// @Override
	// public Object call() throws Exception {
	// System.out.println(Thread.currentThread().getName() + " is running." +
	// System.currentTimeMillis());
	//
	// return Thread.currentThread().getName();
	// }

}
