package org.apache.lucene.util.unimas;

import org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by zhy on 2018/4/13.
 */
public class PathUtil {

    public static Directory reCreatePath(String name, Directory dir) {
        Directory res = dir;
        Map.Entry<String, String> indexShardEntry = PathUtil.getIndexNameAndShard(res.toString());
        UnimasConstant.ESInfo esInfo = null;
        if (indexShardEntry != null)
            esInfo = UnimasConstant.getEsInfo(indexShardEntry.getKey(), dir);
        if (esInfo == null)
            return res;
        if (isDocValuesOrIndexFiles(esInfo, name))
            res = reCreatePath(esInfo, dir);
        return res;
    }

    public static Directory reCreatePath(UnimasConstant.ESInfo esInfo, Directory dir) {
        Directory res = dir;
        try {
            res = SimpleFSDirectory.open(Paths.get(esInfo.dvPath + "/" + esInfo.indexName + "/" + esInfo.shardName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Path stPath(String fullPath) {
        String dataPath = getTrueDirPath(fullPath);
        StringBuilder sb = new StringBuilder();
        String[] datas = dataPath.split("/");
        if (datas.length > 5) {
            for (int i = 0; i < datas.length - 2; i++) {
                sb.append("/");
                sb.append(datas[i]);
            }
            sb.append("/");
            sb.append("_state");
        }
        String stPath = sb.toString();
        Path res = null;
        try {
            if (stPath.length() > 0) {
                Directory stDir = SimpleFSDirectory.open(Paths.get(stPath));
                String[] stFiles = stDir.listAll();
                if (stFiles != null && stFiles.length > 0) {
                    res = Paths.get(stPath + "/" + stFiles[0]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static boolean isDocValuesOrIndexFiles(UnimasConstant.ESInfo esInfo, String name) {
        return esInfo.dvPath != null && esInfo.dvPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION));
    }

    public static Map.Entry<String, String> getIndexNameAndShard(String dirName) {
        Map.Entry<String, String> res = null;
        String[] datas = getTrueDirPath(dirName).split("/");
        if (datas!=null && datas.length > 5) {
            String indexName = datas[datas.length - 3];
            String shardName = datas[datas.length - 2];
            res = new AbstractMap.SimpleEntry<>(indexName, shardName);
        } else {
            System.out.println("error");
        }
        return res;
    }

    /**
     * 规则：模糊匹配
     * 从第一个/开始作为路径开始的标志，直到出现),空格为止
     * 样例
     * LockValidatingDirectoryWrapper(store(mmapfs(/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index)))
     * mmapfs(/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index)
     * MMapDirectory@/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index lockFactory=org.apache.lucene.store.NativeFSLockFactory@5ebcd7e4
     * @param dirName
     * @return
     */
    private static String getTrueDirPath(String dirName) {
//        String[] res;
        StringBuilder sb = new StringBuilder();
        boolean pipeiStart = false;
        for(char ch : dirName.toCharArray()) {
            if(!pipeiStart && ch=='/') {
                pipeiStart = true;
                sb.append(ch);
                continue;
            }
            if(pipeiStart) {
                if(ch!=' ' && ch!=')')
                    sb.append(ch);
                else
                    break;
            }
        }
//        res = sb.toString().split("/");
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(getTrueDirPath("LockValidatingDirectoryWrapper(store(mmapfs(/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index)))"));
        System.out.println(getTrueDirPath("mmapfs(/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index)"));
        System.out.println(getTrueDirPath("MMapDirectory@/data-b/udb-56/nodes/0/indices/_KTJV0ZYRs-VTPbybXIxHw/0/index lockFactory=org.apache.lucene.store.NativeFSLockFactory@5ebcd7e4"));
    }
}
