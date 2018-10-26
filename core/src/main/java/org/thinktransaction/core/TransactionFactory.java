package org.thinktransaction.core;

import org.thinktransaction.core.idempotent.IdempotentExecutor;
import org.thinktransaction.core.schema.bedt.BedtExecutor;
import org.thinktransaction.core.schema.ctp.CtpExecutor;
import org.thinktransaction.core.schema.tcc.TccExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 事务工厂
 *
 * @author darren.ouyang
 * @version 2018/9/26 15:11
 */
public final class TransactionFactory {

    /**
     * 是否已初始化
     */
    private static boolean initFlag = false;

    /**
     * 数据存储器
     */
    private static TransactionDataProvider dataProvider;

    /**
     * 线程池服务
     * 场景1 异步执行事件.
     * 场景2 事件间隔扫描.
     */
    private static ScheduledExecutorService executorService;

    /**
     * 扫描器初始化延迟时间, 单位秒
     */
    private static int SCANNER_INITIAL_DELAY= 60;

    /**
     * 扫描器每次执行间隔时间, 单位秒
     */
    private static int SCANNER_DELAY = 15;

    /**
     * 重试最大次数
     */
    private static int MAX_RETRY_COUNT = 10;

    /**
     * 初始化事务工厂
     *
     * @param dataProvider 数据存储器
     */
    public static void init (
        TransactionDataProvider dataProvider,
        ScheduledExecutorService executorService
    ){
        TransactionFactory.dataProvider = dataProvider;
        TransactionFactory.executorService = executorService;

        // scannerInitialDelay秒后开始, 每间隔scannerDelay秒执行一次事务扫描器
        executorService.scheduleWithFixedDelay(
            new TransactionScanner(dataProvider),
            TransactionFactory.SCANNER_INITIAL_DELAY,
            TransactionFactory.SCANNER_DELAY,
            TimeUnit.SECONDS
        );

        // 标记初始化完成
        TransactionFactory.initFlag = true;
    }


    /**
     * 创建最大努力型分布式事务执行器
     *
     * @return 执行器
     */
    public static BedtExecutor createBedtExecutor(){
        // 检测是否已初始化
        assertInit();

        return new BedtExecutor()
            .setDataProvider(dataProvider)
            .setExecutorService(executorService);
    }

    /**
     * 创建补偿型事务执行器
     *
     * @return 执行器
     */
    public static CtpExecutor createCtpExecutor (){
        // 检测是否已初始化
        assertInit();

        return new CtpExecutor()
            .setDataProvider(dataProvider)
            .setExecutorService(executorService);
    }

    /**
     * 创建tcc事务执行器
     *
     * @return 执行器
     */
    public static TccExecutor createTccExecutor (){
        // 检测是否已初始化
        assertInit();

        return new TccExecutor()
            .setDataProvider(dataProvider)
            .setExecutorService(executorService);
    }

    /**
     * 创建幂等处理执行器
     *
     * @return 执行器
     */
    public static IdempotentExecutor createIdempotentExecutor (){
        // 检测是否已初始化
        assertInit();

        return new IdempotentExecutor()
            .setDataProvider(dataProvider);
    }

    /**
     * 断言是否初始化
     */
    private static void assertInit (){
        if (!initFlag){
            throw new RuntimeException("TransactionFactory error, please init first!!!");
        }
    }
}
