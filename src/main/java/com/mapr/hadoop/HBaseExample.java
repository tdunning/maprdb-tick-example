package com.mapr.hadoop;

import com.google.common.collect.Lists;
import org.apache.hadoop.hbase.util.Bytes;
import org.hbase.async.KeyValue;
import org.joda.time.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.*;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
// Input file is in CSV format:
// Symbol,Date,Open,High,Low,Close,Volume
// AAIT,18-May-2015 11:29,36.58,36.58,36.58,36.58,375

public class HBaseExample {	
	private static String generateKeyString(String symbol, DateTime dateTime) {
        return String.format("%s_%tY-%<tm-%<td-%<tH", symbol ,dateTime.toDate());
	}

    public static class TickWriterCallable implements Callable<Double> {
        private TickDataClient tdc;
        private Map<String, DataReader.TransactionList> mp;
        private String tableName;
        private String cfName;
        private String key;
        private Double elapsed;
        Set<String> keySet;

        public TickWriterCallable(TickDataClient _tdc, Map<String,DataReader.TransactionList> _m, String _tableName, String _cfName, String _key) {
            tdc = _tdc;
            mp = _m;
            tableName = _tableName;
            cfName = _cfName;
            key = _key;
            elapsed = 0.0;
            keySet = new HashSet<String>();

            keySet.add(key);
        }

        public void persistMapAsync() throws java.io.IOException {
            byte[] cfNameBytes = Bytes.toBytes(cfName);
            byte[] columnNameBytes = Bytes.toBytes("data");

            double pt0 = System.nanoTime() * 1e-9;
            for (String s : keySet) {
                KeyValue kv = new KeyValue(Bytes.toBytes(s), cfNameBytes, columnNameBytes, Bytes.toBytes(mp.get(s).asJsonMaps()));
                tdc.performPut(kv);
            }
            double pt1 = System.nanoTime() * 1e-9;
            // System.out.printf("Wrote %d equities in %.3f seconds\n", mp.size(), pt1 - pt0);
            elapsed = pt1-pt0;
        }

        @Override
        public Double call() {
            try {
                persistMapAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return elapsed;
        }
    }

	public static void main(String[] args) throws IOException {
        String cfName = args[0];
        String tableName = args[1];
        String inputFilePath = args[2];
        int nThreads = 5;

        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        TickDataClient tdc = new TickDataClient("", cfName, tableName);
        tdc.init();

        DataReader rd = new DataReader();
        double t0 = System.nanoTime() * 1e-9;
        Map<String, DataReader.TransactionList> m = rd.read(Files.newReaderSupplier(Paths.get(inputFilePath).toFile(), Charsets.UTF_8));
        double t1 = System.nanoTime() * 1e-9;
        System.out.printf("Read %d equities in %.3f seconds\n", m.size(), t1 - t0);

        Set<String> keys = m.keySet();
        final List<TickWriterCallable> tasks = Lists.newArrayList();

        Double totalElapsed = 0.0;
        for (String k: keys) {
            TickWriterCallable t = new TickWriterCallable(tdc, m, tableName, cfName, k);
            tasks.add(t);
        }

        double t2 = System.nanoTime() * 1e-9;
        try {
            List<Future<Double>> results = es.invokeAll(tasks);
            for (Future<Double> f: results) {
                try {
                    totalElapsed += f.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        double t3 = System.nanoTime() * 1e-9;
        System.out.printf("Wrote %d equities in %.3f seconds\n", m.size(), t3-t2);

        es.shutdown();
        tdc.term();
	}
}
