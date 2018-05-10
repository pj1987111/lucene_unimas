package org.apache.lucene.util.unimas;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.unimas.index.FieldBplusTree;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.lucene.store.InputStreamIndexInput;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.gateway.CorruptStateException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat.INDEX_EXTENSION;

/**
 * Created by zhy on 2018/4/23.
 */
public class UnimasIndexReader {
    static Logger logger = Logger.getLogger("UnimasIndexReader");
    private Path stFilePath;
    private SegmentWriteState state;


    public UnimasIndexReader(Path stFilePath, SegmentWriteState state) {
        this.stFilePath = stFilePath;
        this.state = state;
    }

    public static class MetaConfig {
        String indexs;
        String dvPath;
        String indexOrder;
        Map<String, String> fieldTypeMap;
    }

    /**
     *
     * @param stFilePath
     * @return <indexnames,dvdpath>
     * @throws IOException
     */
    public static MetaConfig getMetaConfig(Path stFilePath) throws IOException {
        if(stFilePath==null) {
            logger.info("index init error,no st file...");
            return null;
        }
        MetaConfig res = new MetaConfig();
        StringBuilder indexSb = new StringBuilder();
        String dvPath = null;
        String indexOrder = "20";
        IndexMetaData indexMetaData = readSt(stFilePath);
        Map<String, String> fieldTypeMap = new HashMap<>();
        for (ObjectObjectCursor<String, MappingMetaData> aa : indexMetaData.getMappings()) {
            String key = aa.key;
            MappingMetaData value = aa.value;
            Map<String, Object> sourceMap = value.getSourceAsMap();
            Object metaObj = sourceMap.get("_meta");
            if (metaObj != null && metaObj instanceof Map) {
                Object metaIndexObj = ((Map<String, Object>) metaObj).get("_index");
                if (metaIndexObj != null && metaIndexObj instanceof Map) {
                    Map<String, Object> metaIndexMap = (Map<String, Object>) metaIndexObj;
                    for (Map.Entry<String, Object> metaIndexEntry : metaIndexMap.entrySet()) {
                        indexSb.append(metaIndexEntry.getKey());
                        indexSb.append(";");
                    }
                }
                Object dvPathObj = ((Map<String, Object>) metaObj).get("_dvpath");
                if (dvPathObj != null && dvPathObj instanceof String) {
                    dvPath = (String) dvPathObj;
                }
                Object indexOrderObj = ((Map<String, Object>) metaObj).get("_indexorder");
                if (indexOrderObj != null && indexOrderObj instanceof String) {
                    indexOrder = (String) indexOrderObj;
                }
            }
            Object propertiesObj = sourceMap.get("properties");
            if (propertiesObj != null && propertiesObj instanceof Map) {
                for(Map.Entry<String, Object> proEntry : ((Map<String, Object>) propertiesObj).entrySet()) {
                    String fieldName = proEntry.getKey();
                    Object fieldValObj = proEntry.getValue();
                    if (fieldValObj != null && fieldValObj instanceof Map) {
                        String type = ((Map<String, String>) fieldValObj).get("type");
                        fieldTypeMap.put(fieldName, type);
                    }
                }
            }
        }
        res.fieldTypeMap = fieldTypeMap;
        if(indexSb.length()>0)
            res.indexs = indexSb.substring(0, indexSb.length()-1);
        if(dvPath!=null)
            res.dvPath = dvPath;
        res.indexOrder = indexOrder;
        return res;
    }

    private static IndexMetaData readSt(Path stFilePath) throws IOException {
        try (Directory dir = new SimpleFSDirectory(stFilePath.getParent())) {
            try (IndexInput indexInput = dir.openInput(stFilePath.getFileName().toString(), IOContext.DEFAULT)) {
                // We checksum the entire file before we even go and parse it. If it's corrupted we barf right here.
                CodecUtil.checksumEntireFile(indexInput);
                final int fileVersion = CodecUtil.checkHeader(indexInput, "state", 0, 1);
                final XContentType xContentType = XContentType.values()[indexInput.readInt()];
                if (fileVersion == 0) {
                    // format version 0, wrote a version that always came from the content state file and was never used
                    indexInput.readLong(); // version currently unused
                }
                long filePointer = indexInput.getFilePointer();
                long contentSize = indexInput.length() - CodecUtil.footerLength() - filePointer;
                try (IndexInput slice = indexInput.slice("state_xcontent", filePointer, contentSize)) {
                    try (XContentParser parser = XContentFactory.xContent(xContentType).createParser(NamedXContentRegistry.EMPTY,
                            new InputStreamIndexInput(slice, contentSize))) {
                        return IndexMetaData.fromXContent(parser);
                    }
                }
            } catch (CorruptIndexException | IndexFormatTooOldException | IndexFormatTooNewException ex) {
                // we trick this into a dedicated exception with the original stacktrace
                throw new CorruptStateException(ex);
            }
        }
    }

    public Map<String, FieldBplusTree> initFieldIndex(Map<String, String> fieldTypeMap) throws IOException {
        if(stFilePath==null) {
            logger.info("index init error,no st file...");
            return null;
        }
        Map<String, FieldBplusTree> res = new HashMap<>();
        IndexMetaData indexMetaData = readSt(stFilePath);
        for (ObjectObjectCursor<String, MappingMetaData> aa : indexMetaData.getMappings()) {
            String key = aa.key;
            MappingMetaData value = aa.value;
            Map<String, Object> sourceMap = value.getSourceAsMap();
            Object metaObj = sourceMap.get("_meta");
            if (metaObj != null && metaObj instanceof Map) {
                Object metaIndexObj = ((Map<String, Object>) metaObj).get("_index");
                if (metaIndexObj != null && metaIndexObj instanceof Map) {
                    Map<String, Object> metaIndexMap = (Map<String, Object>) metaIndexObj;
                    for (Map.Entry<String, Object> metaIndexEntry : metaIndexMap.entrySet()) {
                        FieldBplusTree.FieldType fieldType = getBTreeFieldType("" + metaIndexEntry.getValue());
                        res.put(metaIndexEntry.getKey(), initSingleBplusTree(metaIndexEntry.getKey(), fieldType, state));
                    }
                }
            }
            Object propertiesObj = sourceMap.get("properties");
            if (propertiesObj != null && propertiesObj instanceof Map) {
                for(Map.Entry<String, Object> proEntry : ((Map<String, Object>) propertiesObj).entrySet()) {
                    String fieldName = proEntry.getKey();
                    Object fieldValObj = proEntry.getValue();
                    if (fieldValObj != null && fieldValObj instanceof Map) {
                        String type = ((Map<String, String>) fieldValObj).get("type");
                        fieldTypeMap.put(fieldName, type);
                    }
                }
            }
        }
        return res;
    }

    private FieldBplusTree.FieldType getBTreeFieldType(String fieldType) {
        if(fieldType.equals("double"))
            return FieldBplusTree.FieldType.DOUBLE;
        else if(fieldType.equals("integer") || fieldType.equals("date"))
            return FieldBplusTree.FieldType.INTEGER;
        else
            return FieldBplusTree.FieldType.STRING;
    }

    private FieldBplusTree initSingleBplusTree(String fieldName, FieldBplusTree.FieldType fieldType,
                                                      SegmentWriteState state) throws IOException {
        String indexName = IndexFileNames.segmentFileName(state.segmentInfo.name,
                state.segmentSuffix + "_" + fieldName, INDEX_EXTENSION);
        IndexOutput indexOutput = state.directory.createOutput(indexName, state.context);
        return new FieldBplusTree(fieldType, indexOutput);
    }
}
