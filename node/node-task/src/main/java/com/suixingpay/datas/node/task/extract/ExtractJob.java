package com.suixingpay.datas.node.task.extract;/**
 * All rights Reserved, Designed By Suixingpay.
 *
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2017年12月24日 11:20
 * @Copyright ©2017 Suixingpay. All rights reserved.
 * 注意：本内容仅限于随行付支付有限公司内部传阅，禁止外泄以及用于其他的商业用途。
 */

import com.suixingpay.datas.common.util.ApplicationContextUtils;
import com.suixingpay.datas.node.core.event.ETLBucket;
import com.suixingpay.datas.node.core.event.MessageEvent;
import com.suixingpay.datas.node.core.task.AbstractStageJob;
import com.suixingpay.datas.node.core.task.StageType;
import com.suixingpay.datas.node.datacarrier.DataCarrier;
import com.suixingpay.datas.node.datacarrier.DataCarrierFactory;
import com.suixingpay.datas.node.datacarrier.simple.SimpleDataCarrier;
import com.suixingpay.datas.node.task.extract.extractor.ExtractorFactory;
import com.suixingpay.datas.node.task.worker.TaskWork;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.*;

/**
 * 完成事件的进一步转换、过滤。多线程执行
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2017年12月24日 11:20
 * @version: V1.0
 * @review: zhangkewei[zhang_kw@suixingpay.com]/2017年12月24日 11:20
 */
public class ExtractJob extends AbstractStageJob {
    private static final int BUFFER_SIZE = LOGIC_THREAD_SIZE * LOGIC_THREAD_SIZE * LOGIC_THREAD_SIZE;
    private final TaskWork work;
    private final ExecutorService executorService;
    private final DataCarrier<ETLBucket> carrier;
    private final ExtractorFactory extractorFactory;
    public ExtractJob(TaskWork work) {
        super(work.getBasicThreadName());
        extractorFactory = ApplicationContextUtils.INSTANCE.getBean(ExtractorFactory.class);
        this.work = work;
        //线程阻塞时，在调用者线程中执行
        executorService = new ThreadPoolExecutor(LOGIC_THREAD_SIZE, LOGIC_THREAD_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        carrier = ApplicationContextUtils.INSTANCE.getBean(DataCarrierFactory.class).newDataCarrier(BUFFER_SIZE, 1);
    }

    @Override
    protected void doStop() {
        executorService.shutdown();
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void loopLogic() {
        //只要队列有消息，持续读取
        Pair<Long, List<MessageEvent>> events = null;
        do {
            try {
                events = work.waitEvent(StageType.SELECT);
                if (null != events) {
                    final Pair<Long, List<MessageEvent>> inThreadEvents = events;
                    Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            try {
                                //将MessageEvent转换为ETLBucket
                                ETLBucket bucket = ETLBucket.from(inThreadEvents);
                                extractorFactory.extract(bucket);
                                return true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("extract MessageEvent error!", e);
            }
        } while (null != events && null != events.getRight() &&! events.getRight().isEmpty());
    }

    @Override
    public ETLBucket output() {
        return null;
    }
}