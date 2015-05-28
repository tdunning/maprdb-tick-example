package com.mapr.hadoop;

import com.google.common.base.Splitter;
import org.joda.time.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.*;
import java.nio.file.Paths;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;


import com.mapr.hadoop.Tick;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.hbase.async.KeyValue;
// Input file is in CSV format:
// Symbol,Date,Open,High,Low,Close,Volume
// AAIT,18-May-2015 11:29,36.58,36.58,36.58,36.58,375

public class HBaseExample {	
	private static BufferedReader reader;

	private static String generateKeyString(String symbol, DateTime dateTime) {
		// TODO suggest String.format("%s_%tY-%<tm-%<td-%<tH", symbol ,dateTime.toDate()) here
		return symbol + "_" + dateTime.getYear() + dateTime.getMonthOfYear() + dateTime.getDayOfMonth() + dateTime.getHourOfDay();
	}

    public static void persistMapAsync(Map<String, DataReader.TransactionList> mp, String tableName, String cfName) throws java.io.IOException {
        TickDataClient tdc = new TickDataClient("", cfName, tableName);
        tdc.init();
        double pt0 = System.nanoTime() * 1e-9;
        for (String s : mp.keySet()) {
            KeyValue kv = new KeyValue(Bytes.toBytes(s), Bytes.toBytes(cfName), Bytes.toBytes("data"), Bytes.toBytes(mp.get(s).asJsonMaps()));
            tdc.performPut(kv);
        }
        double pt1 = System.nanoTime() * 1e-9;
        System.out.printf("Wrote %d equities in %.3f seconds\n", mp.size(), pt1 - pt0);
        tdc.term();
    }

	public static void persistMap(Map<String, DataReader.TransactionList> mp, String tableName, String cfName) throws java.io.IOException {
		Configuration config = HBaseConfiguration.create();
		HTable table = new HTable(config, tableName);

        double pt0 = System.nanoTime() * 1e-9;
        for (String s : mp.keySet()) {
			Put p = new Put(Bytes.toBytes(s));
			p.add(Bytes.toBytes(cfName), Bytes.toBytes("data"), Bytes.toBytes(mp.get(s).asJsonMaps()));
			table.put(p);
        }
        double pt1 = System.nanoTime() * 1e-9;
        System.out.printf("Wrote %d equities in %.3f seconds\n", mp.size(), pt1 - pt0);
	}

	public static void main(String[] args) throws IOException {
        Boolean async = args[2].equals("async");
        String inputFilePath = args[1];
        String tableName = args[0];

        DataReader rd = new DataReader();
        double t0 = System.nanoTime() * 1e-9;
        Map<String, DataReader.TransactionList> m = rd.read(Files.newReaderSupplier(Paths.get(inputFilePath).toFile(), Charsets.UTF_8));
        double t1 = System.nanoTime() * 1e-9;
        System.out.printf("Read %d equities in %.3f seconds\n", m.size(), t1 - t0);

        if (async) {
            System.err.println("persisting to DB asynchronously.");
            persistMapAsync(m, tableName, "cf1");
        } else {
            System.err.println("persisting to DB synchronously.");
            persistMap(m, tableName, "cf1");
        }
	}
}
