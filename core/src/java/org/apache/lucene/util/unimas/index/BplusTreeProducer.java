package org.apache.lucene.util.unimas.index;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.unimas.PathUtil;
import org.apache.lucene.util.unimas.UnimasConstant;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat.INDEX_EXTENSION;

/**
 * Created by zhy on 2018/4/19.
 */
public class BplusTreeProducer {
    private FieldBplusTree bplusTree;
    final static List<String> ignoreField = new ArrayList<>();

    static {

        ignoreField.add("_source");
        ignoreField.add("_type");
        ignoreField.add("_uid");
        ignoreField.add("_version");
        ignoreField.add("_all");
        ignoreField.add("_field_names");
    }

    public BplusTreeProducer(String segmentName, Directory cfsDir, String fieldName) {
        Map.Entry<String, String> indexShardEntry = PathUtil.getIndexNameAndShard(cfsDir.toString());
        UnimasConstant.ESInfo esInfo = null;
        if (indexShardEntry != null)
            esInfo = UnimasConstant.getEsInfo(indexShardEntry.getKey(), cfsDir);
        if (esInfo == null)
            return;
        //只有读了segment才会有值
        if (esInfo.indexName != null && esInfo.shardName != null && !ignoreField.contains(fieldName)) {
            try {
                String indexName = IndexFileNames.segmentFileName(segmentName,
                        "Lucene54_0_" + fieldName, INDEX_EXTENSION);
                Directory fsDirectory;
                if (esInfo.dvPath == null)
                    fsDirectory = cfsDir;
                else
                    fsDirectory = SimpleFSDirectory.open(Paths.get(esInfo.dvPath + "/" +
                            esInfo.indexName + "/" + esInfo.shardName));
                IndexInput indexInput = fsDirectory.openInput(indexName, null);
                bplusTree = new FieldBplusTree().initData(indexInput, esInfo.indexOrder);
            } catch (NoSuchFileException e) {
                //ignore
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FieldBplusTree getBPlusTree() {
        return bplusTree;
    }
}
