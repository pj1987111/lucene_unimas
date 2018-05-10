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
    public static void addValues(Map<String, FieldBplusTree> fieldIndex, String fieldName, Iterable<Number> values, Map<String, String> fieldTypeMap) throws IOException {
        if(fieldIndex==null)
            return;
        int docid = 0;
        for (Number nv : values) {
            final Long v;
            if (nv == null) {
                v = 0L;
            } else {
                v = nv.longValue();
            }
            FieldBplusTree fieldBplusTree = fieldIndex.get(fieldName);
            if (fieldBplusTree != null) {
                String type = fieldTypeMap.get(fieldName);
                if(type!=null && type.equals("double"))
                    fieldBplusTree.add(Double.longBitsToDouble(v), docid);
                else
                    fieldBplusTree.add(v, docid);
            }
            ++docid;
        }
    }

    public static void addValues(Map<String, FieldBplusTree> fieldIndex, String fieldName, Iterable<BytesRef> values,
                                 Iterable<Number> docToOrd) throws IOException {
        if(fieldIndex==null)
            return;
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
                FieldBplusTree fieldBplusTree = fieldIndex.get(fieldName);
                if (fieldBplusTree != null)
                    fieldBplusTree.add(valueTemp, docid);
            }
            ++docid;
        }
    }

    public static List<Integer> and(List<Integer>... conditionLists) {
        if(conditionLists.length==0)
            return null;
        else if(conditionLists.length==1)
            return conditionLists[0];
        List<Integer> res = conditionLists[0];
        for(int i=1; i<conditionLists.length; i++)
            res.retainAll(conditionLists[i]);
        return res;
    }
}
