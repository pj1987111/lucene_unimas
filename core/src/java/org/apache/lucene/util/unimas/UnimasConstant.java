package org.apache.lucene.util.unimas;

import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhy on 2018/4/18.
 */
public class UnimasConstant {
    //indexname---segmentinfos
    private static Map<String, ESInfo> esInfosMap = new HashMap<>();

    public static class ESInfo {
        public String indexName;
        public String shardName;
        public String dvPath = null;
        //for delete
        public String dvIndices;
        public int indexOrder = 20;
        public Map<String, String> fieldTypeMap = new HashMap<>();
        public ESInfo(String dvPath, String indexs, String indexOrder, Map<String, String> fieldTypeMap, String indexName, String shardName) {
            this.dvPath = dvPath;
            this.dvIndices = indexs;
            this.indexOrder = Integer.parseInt(indexOrder);
            this.indexName = indexName;
            this.shardName = shardName;
            this.fieldTypeMap = fieldTypeMap;
        }

        public static ESInfo readSt(Directory segmentDir) throws IOException {
            UnimasIndexReader.MetaConfig metaConfig = UnimasIndexReader.getMetaConfig(PathUtil.stPath(segmentDir.toString()));
            Map.Entry<String, String> indexShard = PathUtil.getIndexNameAndShard(segmentDir.toString());
            if(indexShard == null)
                throw new IOException("path:"+segmentDir.toString()+" do now have index and shard parts!");
            return new ESInfo(metaConfig.dvPath, metaConfig.indexs, metaConfig.indexOrder, metaConfig.fieldTypeMap, indexShard.getKey(), indexShard.getValue());
        }
    }

    public static ESInfo getEsInfo(String indexName, Directory segmentDir) {
        ESInfo esInfo = esInfosMap.get(indexName);
        if(esInfo == null) {
            try {
                esInfo = ESInfo.readSt(segmentDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            esInfosMap.put(indexName, esInfo);
        }
        return esInfo;
    }

    public static void putEsInfos(ESInfo esInfo) {
        esInfosMap.put(esInfo.indexName, esInfo);
    }

}
