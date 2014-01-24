package com.addthis.hydra.job.spawn;

import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;

import com.addthis.basis.util.Parameter;
import com.addthis.basis.util.Strings;

import com.addthis.hydra.data.util.DateUtil;
import com.addthis.hydra.data.util.JSONFetcher;
import com.addthis.hydra.task.stream.StreamSourceMeshy;
import com.addthis.meshy.MeshyClient;
import com.addthis.meshy.service.file.FileReference;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobAlertUtil {
    private final static Logger log = LoggerFactory.getLogger(JobAlertUtil.class);
    private final static String queryURLBase = "http://" + Parameter.value("spawn.queryhost") + ":2222/query/call";
    private final static String defaultOps = "gather=s";
    private final static int alertQueryTimeout = Parameter.intValue("alert.query.timeout", 20_000);
    private final static int alertQueryRetries = Parameter.intValue("alert.query.retries", 4);
    private final static DateTimeFormatter ymdFormatter = new DateTimeFormatterBuilder().appendTwoDigitYear(2000).appendMonthOfYear(2).appendDayOfMonth(2).toFormatter();
    private final static String meshHost = Parameter.value("mesh.host", "localhost");
    private final static int meshPort = Parameter.intValue("mesh.port", 5000);
    private final static int pathTokenOffset = Parameter.intValue("source.mesh.path.token.offset", 2);

    /* Blank StreamSourceMeshy for performing the canonical FileReference key generation */
    private static final StreamSourceMeshy streamSourceMeshy = new StreamSourceMeshy();

    private static MeshyClient meshyClient;
    static {
        streamSourceMeshy.setPathTokenOffset(pathTokenOffset);
        try {
            meshyClient = new MeshyClient(meshHost, meshPort);
        } catch (IOException e) {
            log.warn("Failed to initialize job alert meshy", e);
        }
    }

    /**
     * Count the total byte sizes of files along a certain path via mesh
     * @param jobId The job to check
     * @param dirPath The path to check within the jobId, e.g. split/{{now-1}}/importantfiles/*.gz
     * @return A long representing the total size in bytes of files along the specified path
     */
    public static long getTotalBytesFromMesh(String jobId, String dirPath) {
        String meshLookupString = "/job*/" + jobId + "/*/gold/" + expandDateMacro(dirPath);
        if (meshyClient != null) {
            try {
                Collection<FileReference> fileRefs = meshyClient.listFiles(new String[]{meshLookupString});
                HashSet<String> fileRefKeysUsed = new HashSet<>();
                long totalBytes = 0;
                for (FileReference fileRef : fileRefs) {
                    // Use StreamSourceMeshy to generate a canonical path key. In particular, strip off any multi-minion prefixes if appropriate.
                    String meshFileKey = streamSourceMeshy.getPathOffset(fileRef.name);
                    if (!fileRefKeysUsed.contains(meshFileKey)) {
                        totalBytes += fileRef.size;
                        fileRefKeysUsed.add(meshFileKey);
                    }
                }
                return totalBytes;
            } catch (IOException e) {
                log.warn("Job alert mesh look up failed", e);
            }
        }
        else {
            log.warn("Received mesh lookup request jobId=" + jobId + " dirPath=" + dirPath + " while meshy client was not instantiated; returning zero");
        }
        return 0;
    }

    /**
     * Count the total number of hits along a certain path in a tree object
     * @param jobId The job to query
     * @param checkPath The path to check, e.g.
     * @return The number of hits along the specified path
     */
    public static long getQueryCount(String jobId, String checkPath) {

        HashSet<String> result = JSONFetcher.staticLoadSet(getQueryURL(jobId, checkPath, defaultOps, defaultOps), alertQueryTimeout, alertQueryRetries, null);
        if (result == null || result.isEmpty()) {
            log.warn("Found no data for job " + jobId + " checkPath=" + checkPath + "; returning zero");
            return 0;
        } else if (result.size() > 1) {
            log.warn("Found multiple results for job " + jobId + "checkPath=" + checkPath + "; using first row");
        }
        String raw = result.iterator().next();
        return Long.parseLong(raw.replaceAll("[\\[\\]]", "")); // Trim [] characters and parse as long

    }

    private static String getQueryURL(String jobId, String path, String ops, String rops) {
        return queryURLBase + "?job=" + jobId + "&path=" + Strings.urlEncode(expandDateMacro(path)) + "&ops=" + Strings.urlEncode(ops) + "&rops=" + Strings.urlDecode(rops);
    }

    /**
     * Split a path up and replace any {{now-1}}-style elements with the YYMMDD equivalent
     * @param path The input path to process
     * @return The path with the relevant tokens replaced
     */
    private static String expandDateMacro(String path) {
        for (String entry : path.split("[/:]")) {
            if (entry.startsWith(DateUtil.NOW_PREFIX) && entry.endsWith(DateUtil.NOW_POSTFIX)) {
                path = path.replace(entry, DateUtil.getDateTime(ymdFormatter, entry).toString(ymdFormatter));
            }
        }
        return path;
    }
}
