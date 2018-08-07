package com.zl.lock;

/**
 * @program: test
 * @description: 测试
 * @author: 张乐
 * @create: 2018-08-06 17:36
 **/
public class Test {
    public static void main(String[] args) throws InterruptedException {
        ZKLock zkLock = new ZKLock("192.168.188.131:2181");
        zkLock.lock();
        System.out.println("获得锁");
        Thread.sleep(10000);
        zkLock.unlock();
        System.out.println("释放锁");
    }
}
