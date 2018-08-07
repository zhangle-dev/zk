package com.zl.lock;

import com.zl.util.ZKUtils;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @program: test
 * @description: 利用zookeeper实现的分布式锁
 * @author: 张乐
 * @create: 2018-08-06 10:04
 **/
public class ZKLock extends ReentrantLock {

    private ZKUtils zkUtils;
    private String lockNodeName;
    private String lockPath = "/lock";
    private String lockStr = "lock0";
    private CountDownLatch countDownLatch;

    public ZKLock(String zkConnectStr,String lockStr) {
        if (lockStr != null) {
            this.lockStr = lockStr;
        }
        try {
            zkUtils = new ZKUtils(zkConnectStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ZKLock(String zkConnectStr) {
        this(zkConnectStr, null);
    }

    @Override
    public void lock() {
        super.lock();
        countDownLatch = new CountDownLatch(1);
        //在zk中创建临时有序节点
        lockNodeName = zkUtils.createZNodeES(lockPath, "/" + lockStr, this::tryGetLock);

        //阻塞等待获取资源
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //当zk节点变动时尝试获取锁
    private void tryGetLock(WatchedEvent watchedEvent) {

        System.out.println(watchedEvent.getPath() + "--" + watchedEvent.getType() + "--" + watchedEvent.getState());
        if (tryGetLock()) {
            countDownLatch.countDown();
        }
    }

    private boolean tryGetLock() {

        List<String> list = zkUtils.getChildNode(lockPath);
        System.out.println(list);
        list.sort(String::compareTo);
        list = list.stream().filter(s -> s.startsWith(lockStr)).collect(Collectors.toList());
        //如果获取到锁
        if (lockNodeName.equals(lockPath + "/" + list.get(0))) {
            return true;
        }
        return false;
    }

    @Override
    public void unlock() {
        //释放锁并删除zk节点
        zkUtils.deleteNode(lockNodeName);
        zkUtils.close();
        super.unlock();
    }
}
