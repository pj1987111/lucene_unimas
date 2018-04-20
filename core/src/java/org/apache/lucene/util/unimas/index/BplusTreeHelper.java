package org.apache.lucene.util.unimas.index;

import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhy on 2018/4/18.
 */
public class BplusTreeHelper {
    public static void addValues(Map<String, FieldBplusTree> fieldIndex, String fieldName, Iterable<Number> values) throws IOException {
        int docid = 0;
        for (Number nv : values) {
            final Long v;
            if (nv == null) {
                v = 0L;
            } else {
                v = nv.longValue();
            }
//            if (index != null) {
//                String writeVal = fieldName + "+val:" + v + "+docid:" + docid + "\n";
//                index.write(writeVal.getBytes(), 0, writeVal.length());
//            }
            FieldBplusTree fieldBplusTree = fieldIndex.get(fieldName);
            if (fieldBplusTree != null)
                fieldBplusTree.add(v, docid);
            ++docid;
        }
    }

    public static void addValues(Map<String, FieldBplusTree> fieldIndex, String fieldName, Iterable<BytesRef> values,
                                 Iterable<Number> docToOrd) throws IOException {
        List<String> valuesTemp = new ArrayList<>();
        for (BytesRef value : values)
            valuesTemp.add(value.utf8ToString());
        int docid = 0;
        for (Number nv : docToOrd) {
            final int v;
            if (nv == null) {
                v = 0;
            } else {
                v = nv.intValue();
            }
            String valueTemp = valuesTemp.get(v);
            if (valueTemp != null) {
//                if (index != null) {
//                    String writeVal = fieldName + "+val:" + valueTemp + "+docid:" + docid + "\n";
//                    index.write(writeVal.getBytes(), 0, writeVal.length());
//                }
                FieldBplusTree fieldBplusTree = fieldIndex.get(fieldName);
                if (fieldBplusTree != null)
                    fieldBplusTree.add(valueTemp, docid);
            }
            ++docid;
        }
    }
}
