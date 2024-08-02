/*
Copyright (c) 2017-2024 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    private static final long MAXIMUM_FILE_SIZE = (1024L * 1024L * 1024L * 10L); //10GB
    private static final int MAX_THREAD_COUNT = 8;
    private static ThreadPoolExecutor threadPoolExecutorFind = null;
    private static ThreadPoolExecutor threadPoolExecutorWork = null;
    private static long originalMountTotalSize = 0;
    private static final ConcurrentLinkedQueue<Future<?>> futures = new ConcurrentLinkedQueue<>();
    private static File databases;
    private static BloomFilter<String> signaturesMD5 = null;
    private static BloomFilter<String> signaturesMD5Extended = null;
    private static BloomFilter<String> signaturesSHA1 = null;
    private static BloomFilter<String> signaturesSHA256 = null;
    private static final AtomicInteger FILES_READ = new AtomicInteger();
    private static final AtomicLong DATA_READ = new AtomicLong();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please provide a directory for databases, all additional paths will be recursed for scanning.");
            System.exit(1);
        }
        if (args[0] != null) {
            if (!args[0].startsWith("/") && !args[0].startsWith(".")) {
                args[0] = "./" + args[0];
            }
            databases = new File(args[0]);
            if (databases.getParentFile().exists() && databases.isDirectory()) {
                System.out.println("Databases will be loaded from " + databases);
                try {
                   signaturesMD5 = BloomFilter.readFrom(Files.newInputStream(new File(databases + "/hypatia-md5-bloom.bin").toPath()), Funnels.stringFunnel(Charsets.US_ASCII));
                   signaturesMD5Extended = BloomFilter.readFrom(Files.newInputStream(new File(databases + "/hypatia-md5-extended-bloom.bin").toPath()), Funnels.stringFunnel(Charsets.US_ASCII));
                   signaturesSHA1 = BloomFilter.readFrom(Files.newInputStream(new File(databases + "/hypatia-sha1-bloom.bin").toPath()), Funnels.stringFunnel(Charsets.US_ASCII));
                   signaturesSHA256 = BloomFilter.readFrom(Files.newInputStream(new File(databases + "/hypatia-sha256-bloom.bin").toPath()), Funnels.stringFunnel(Charsets.US_ASCII));
                   System.out.println("Loaded database:");
                   System.out.println("\tMD5: " + signaturesMD5.approximateElementCount());
                   System.out.println("\tMD5E: " + signaturesMD5Extended.approximateElementCount());
                   System.out.println("\tSHA1: " + signaturesSHA1.approximateElementCount());
                   System.out.println("\tSHA256: " + signaturesSHA256.approximateElementCount());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Invalid databases directory");
                System.exit(1);
            }
        }

        //Start the hashing
        final long startTime = System.currentTimeMillis();
        threadPoolExecutorFind = new ThreadPoolExecutor(getMaxThreads(true), getMaxThreads(true), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(8), new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutorWork = new ThreadPoolExecutor(getMaxThreads(true), getMaxThreads(true), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(4), new ThreadPoolExecutor.CallerRunsPolicy());
        for (int c = 1; c < args.length; c++) {
            if (args[c] != null) {
                processDirectory(new File(args[c]));
                waitForThreadsComplete();
            }
        }
        long mbRead = DATA_READ.longValue() / 1000L / 1000L;
        long msSpent = System.currentTimeMillis() - startTime;
        long mbPerSecond = mbRead;
        if (msSpent > 1000) {
            mbPerSecond = (long) (((double) mbRead) / ((double) msSpent / 1000D));
        }
        System.out.println("Hashed " + FILES_READ + " files, totalling " + mbRead + "MB, " + msSpent + "ms at " + mbPerSecond + "MBps");

        System.exit(0); //Exit
    }


    private static void waitForThreadsComplete() {
        try {
            for (Future<?> future : futures) {
                future.get();
                futures.remove(future);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        futures.clear();
    }

    private static void processDirectory(File dir) {
        if (!dir.exists()) {
            System.out.println("Path doesn't exist: " + dir);
        } else {
            originalMountTotalSize = dir.getTotalSpace();
            findFilesRecursive(dir);
        }
    }

    public static void findFilesRecursive(File root) {
        File[] files = root.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                if (f.canRead() && !Files.isSymbolicLink(f.toPath())) {
                    if (f.isDirectory() && (f.getTotalSpace() == originalMountTotalSize)) {
                        futures.add(threadPoolExecutorFind.submit(() -> findFilesRecursive(f)));
                    } else {
                        if (Files.isRegularFile(f.toPath()) && f.length() > 0 && f.length() <= MAXIMUM_FILE_SIZE) {
                            futures.add(threadPoolExecutorWork.submit(() -> checkFile(f)));
                        }
                    }
                }
            }
        }
    }

    private static void checkFile(File file) {
        try {
            InputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[4096];
            int numRead;

            MessageDigest digestMD5 = MessageDigest.getInstance("MD5");
            MessageDigest digestSHA1 = MessageDigest.getInstance("SHA-1");
            MessageDigest digestSHA256 = MessageDigest.getInstance("SHA-256");

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    digestMD5.update(buffer, 0, numRead);
                    digestSHA1.update(buffer, 0, numRead);
                    digestSHA256.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            fis.close();
            DATA_READ.getAndAdd(file.length());

            if(signaturesMD5.mightContain(String.format("%032x", new BigInteger(1, digestMD5.digest())).toLowerCase())
            || signaturesMD5Extended.mightContain(String.format("%032x", new BigInteger(1, digestMD5.digest())).toLowerCase())
            || signaturesSHA1.mightContain(String.format("%040x", new BigInteger(1, digestSHA1.digest())).toLowerCase())
            || signaturesSHA256.mightContain(String.format("%064x", new BigInteger(1, digestSHA256.digest())).toLowerCase())) {
                System.out.println("MATCH FOUND: " + file);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            FILES_READ.getAndIncrement();
        }
    }

    public static int getMaxThreads(boolean cap) {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        if (cap && maxThreads > MAX_THREAD_COUNT) {
            maxThreads = MAX_THREAD_COUNT;
        }
        return maxThreads;
    }
}
