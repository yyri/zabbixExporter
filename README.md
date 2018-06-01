# zabbixExporter
A project need all cpu utilization and memory usage data in zabbix, manually export will cost days for hundreds of hosts, so I wrote these codes to export Zabbix data to csv in seconds.

Zabbix connector with  com.github.0312birdzhang.Zabbix4j.

		<dependency>
			<groupId>com.github.0312birdzhang</groupId>
			<artifactId>Zabbix4j</artifactId>
			<version>0.1.9</version>
		</dependency>

Unfortunatelly this lib doesn't support zabbix3.
		
		