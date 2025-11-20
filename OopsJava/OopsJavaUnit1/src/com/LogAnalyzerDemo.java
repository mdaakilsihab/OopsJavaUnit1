package com.LogAnalyzer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class LogAnalyzerDemo {

    // Keywords to search
    private static final List<String> KEYWORDS =
            Arrays.asList("error", "warning", "failed", "success");

    // Thread-safe total counts
    private static final ConcurrentHashMap<String, Integer> totalCounts =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        // ----------- INPUT FOLDER -----------
        String folderPath;

        if (args.length == 0) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Enter log folder path: ");
            folderPath = sc.nextLine().trim();
        } else {
            folderPath = args[0];
        }

        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("❌ Invalid folder path!");
            return;
        }

        File[] logFiles = folder.listFiles((dir, name) ->
                name.endsWith(".txt") || name.endsWith(".log"));

        if (logFiles == null || logFiles.length == 0) {
            System.out.println("⚠ No log files found (.txt / .log)");
            return;
        }

        System.out.println("\n📂 Found " + logFiles.length + " log files.");
        System.out.println("Processing...\n");

        // ----------- THREAD POOL -----------
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor =
                Executors.newFixedThreadPool(Math.min(cpuCores, logFiles.length));

        long startConcurrent = System.currentTimeMillis();
        List<Future<Map<String, Integer>>> futures = new ArrayList<>();

        for (File file : logFiles) {
            futures.add(executor.submit(new LogWorker(file, KEYWORDS)));
        }

        // Collect thread results
        for (Future<Map<String, Integer>> future : futures) {
            try {
                mergeCounts(future.get());
            } catch (Exception e) {
                System.out.println("Error in worker thread: " + e);
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        long endConcurrent = System.currentTimeMillis();

        // ----------- SEQUENTIAL PROCESS ----------
        long startSequential = System.currentTimeMillis();

        Map<String, Integer> sequentialMap = new HashMap<>();
        for (File file : logFiles) {
            Map<String, Integer> r = processSequential(file);
            r.forEach((k, v) -> sequentialMap.merge(k, v, Integer::sum));
        }

        long endSequential = System.currentTimeMillis();

        // ----------- OUTPUT RESULTS -----------
        System.out.println("\n===== FINAL SUMMARY =====");
        totalCounts.forEach((k, v) ->
                System.out.println(k.toUpperCase() + " = " + v));

        String mostCommon = totalCounts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        System.out.println("\n🔥 MOST FREQUENT KEYWORD: " + mostCommon.toUpperCase());

        writeOutputToFile(totalCounts);

        System.out.println("\n⚡ Concurrent Time: " + (endConcurrent - startConcurrent) + " ms");
        System.out.println("🐢 Sequential Time: " + (endSequential - startSequential) + " ms");

        System.out.println("\n✔ Processing Complete.");
    }

    // Merge results from workers
    private static void mergeCounts(Map<String, Integer> workerResult) {
        workerResult.forEach((k, v) ->
                totalCounts.merge(k, v, Integer::sum));
    }

    // Sequential processing for comparison
    private static Map<String, Integer> processSequential(File file) {
        Map<String, Integer> map = new HashMap<>();
        KEYWORDS.forEach(k -> map.put(k, 0));

        try (Stream<String> lines = Files.lines(file.toPath())) {
            lines.map(String::toLowerCase).forEach(line -> {
                for (String keyword : KEYWORDS) {
                    if (line.contains(keyword)) {
                        map.put(keyword, map.get(keyword) + 1);
                    }
                }
            });
        } catch (IOException e) {
            System.out.println("Sequential read error for file: " + file.getName());
        }

        return map;
    }

    // Save results to output file
    private static void writeOutputToFile(Map<String, Integer> result) {
        try {
            Files.createDirectories(Paths.get("output"));
            FileWriter writer = new FileWriter("output/log_result.txt");

            writer.write("=== Log Keyword Summary ===\n");
            for (String k : KEYWORDS) {
                writer.write(k + " = " + result.getOrDefault(k, 0) + "\n");
            }
            writer.close();

            System.out.println("\n📄 Results saved to output/log_result.txt");

        } catch (Exception e) {
            System.out.println("❌ Error writing output file.");
        }
    }

    // ==================================================
    // =============== THREAD WORKER CLASS ==============
    // ==================================================
    static class LogWorker implements Callable<Map<String, Integer>> {

        private final File file;
        private final List<String> keywords;

        public LogWorker(File file, List<String> keywords) {
            this.file = file;
            this.keywords = keywords;
        }

        @Override
        public Map<String, Integer> call() {
            Map<String, Integer> result = new HashMap<>();
            keywords.forEach(k -> result.put(k, 0));

            long lineCount = 0;

            try (Stream<String> lines = Files.lines(file.toPath())) {
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    String line = it.next().toLowerCase();
                    lineCount++;
                    for (String keyword : keywords) {
                        if (line.contains(keyword)) {
                            result.put(keyword, result.get(keyword) + 1);
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("❌ Error reading file: " + file.getName());
            }

            System.out.println(Thread.currentThread().getName()
                    + " → Processed " + file.getName()
                    + " | Lines: " + lineCount);

            return result;
        }
    }
}
