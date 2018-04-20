package org.apache.lucene.util.unimas.index;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.unimas.PathUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat.INDEX_EXTENSION;
import static org.apache.lucene.util.unimas.UnimasConstant.newPath;

/**
 * Created by zhy on 2018/4/19.
 */
public class BplusTreeProducer {
    private Map<String, FieldBplusTree> bplusTreeWapperMap = new HashMap<>();
    final static List<String> ignoreField = new ArrayList<>();
    static {

        ignoreField.add("_source");
        ignoreField.add("_type");
        ignoreField.add("_uid");
        ignoreField.add("_version");
        ignoreField.add("_all");
        ignoreField.add("_field_names");
    }

    public BplusTreeProducer(SegmentReadState state) {
        //只有读了segment才会有值
        if(SegmentInfos.indexName!=null && SegmentInfos.shardName!=null) {
            for(FieldInfo fieldInfo : state.fieldInfos) {
                if(!ignoreField.contains(fieldInfo.name)) {
                    try {
                        String indexName = IndexFileNames.segmentFileName(state.segmentInfo.name,
                                "Lucene54_0_"+fieldInfo.name, INDEX_EXTENSION);
                        FSDirectory fsDirectory = SimpleFSDirectory.open(Paths.get(newPath+"/"+
                                SegmentInfos.indexName+"/"+SegmentInfos.shardName));
                        IndexInput indexInput = fsDirectory.openInput(indexName, state.context);
//                    IndexInput indexInput = PathUtil.reCreatePath(indexName, state.directory)
//                            .openInput(indexName, state.context);
                        bplusTreeWapperMap.put(fieldInfo.name, new FieldBplusTree().initData(indexInput));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Map<String, FieldBplusTree> getFieldIndex() {
        return bplusTreeWapperMap;
    }
}
