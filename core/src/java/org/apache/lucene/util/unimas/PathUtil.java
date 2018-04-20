package org.apache.lucene.util.unimas;

import org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;

import static org.apache.lucene.util.unimas.UnimasConstant.newPath;

/**
 * Created by zhy on 2018/4/13.
 */
public class PathUtil {

    public static Directory reCreatePath(String name, Directory dir) {
        Directory res = dir;
        if (isDocValuesOrIndexFiles(name))
            res = reCreatePath(dir);
        return res;
    }

    public static Map.Entry<String, String> getIndexNameAndShard(String dirName) {
        Map.Entry<String, String> res = null;
        String[] datas = dirName.substring(dirName.lastIndexOf("(")+1, dirName.indexOf(")")).split("/");
        if(datas.length>5) {
            String indexName = datas[datas.length-3];
            String shardName = datas[datas.length-2];
            res = new AbstractMap.SimpleEntry<>(indexName, shardName);
        }
        return res;
    }

    public static Directory reCreatePath(Directory dir) {
        String dirName = dir.toString();
        Directory res = dir;
        try{
            Map.Entry<String, String> indexAndShard = getIndexNameAndShard(dirName);
            if(indexAndShard!=null) {
                res = SimpleFSDirectory.open(Paths.get(newPath+"/"+indexAndShard.getKey()+"/"+indexAndShard.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static boolean isDocValuesOrIndexFiles(String name) {
        return newPath != null && newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION) || name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        || name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION));
    }
}
