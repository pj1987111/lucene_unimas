package org.apache.lucene.util.unimas;

import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.nio.file.Paths;

/**
 * Created by zhy on 2018/4/13.
 */
public class PathUtil {

    public static final String newPath = "/tmp/zhy";

    public static Directory reCreatePath(Directory dir) {
        String dirName = dir.toString();
        Directory res = dir;
        try{
            String[] datas = dirName.substring(dirName.lastIndexOf("(")+1, dirName.indexOf(")")).split("/");
            if(datas.length>6) {
                String indexName = datas[datas.length-3];
                String shardName = datas[datas.length-2];
                res = SimpleFSDirectory.open(Paths.get(newPath+"/"+indexName+"/"+shardName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}
