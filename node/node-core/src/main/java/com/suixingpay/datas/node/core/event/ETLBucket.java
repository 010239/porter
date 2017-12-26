package com.suixingpay.datas.node.core.event;/**
 * All rights Reserved, Designed By Suixingpay.
 *
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2017年12月26日 11:04
 * @Copyright ©2017 Suixingpay. All rights reserved.
 * 注意：本内容仅限于随行付支付有限公司内部传阅，禁止外泄以及用于其他的商业用途。
 */

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2017年12月26日 11:04
 * @version: V1.0
 * @review: zhangkewei[zhang_kw@suixingpay.com]/2017年12月26日 11:04
 */
public class ETLBucket {
    private final long sequence;
    private final List<ETLRow> rows;

    public ETLBucket(long sequence, List<ETLRow> rows) {
        this.sequence = sequence;
        this.rows = rows;
    }

    public long getSequence() {
        return sequence;
    }

    public List<ETLRow> getRows() {
        return rows;
    }

    /**
     * 转换数据模型
     * @param events
     * @return
     */
    public static ETLBucket from(Pair<Long, List<MessageEvent>> events) {
        List<ETLRow> rows = new ArrayList<>();
        for (MessageEvent event : events.getRight()) {
            List<ETLColumn> columns = new ArrayList<>();
            Boolean loopAfter = ! event.getAfter().isEmpty();
            for (Map.Entry<String,Object> entity : loopAfter ? event.getAfter().entrySet() : event.getBefore().entrySet()) {
                Object newValue = "";
                Object oldValue = "";
                Class type = String.class;

                if (loopAfter) {
                    newValue = entity.getValue();
                    oldValue = event.getBefore().getOrDefault(entity.getKey(),null);
                } else {
                    newValue = event.getBefore().getOrDefault(entity.getKey(),null);
                    oldValue = entity.getValue();
                }

                type =  null != newValue ? newValue.getClass() : (null != oldValue ? oldValue.getClass() : type);
                ETLColumn column = new ETLColumn(entity.getKey(), newValue, oldValue, type, event.getPrimaryKeys().contains(entity.getKey()));
                columns.add(column);
            }

            ETLRow row = new ETLRow(event.getSchema(), event.getTable(), event.getOpType(), columns, event.getOpTs());
            rows.add(row);
        }
        return new ETLBucket(events.getKey(), rows);
    }
}
