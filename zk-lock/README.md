# 使用zookeeper实现分布式锁

前几天使用zookeeper实现了一个简单的分布式锁，今天有空上传一下，方便下次用的时候找

 - 首先导入zk的maven依赖，或者导入相应jar包

```
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.6</version>
</dependency>
```
- 然后写了一个zookeeper的工具类

```
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;

/**
 * @program: test
 * @description: zookeeper工具类
 * @author: 张乐
 * @create: 2018-08-06 09:26
 **/
public class ZKUtils {

    private ZooKeeper zookeeper;

    /**
     * 创建zk工具类
     * @param zkConnectStr zk的主机名和端口，也可以是ip和端口，多个用半角逗号分隔
     * @throws IOException
     */
    public ZKUtils(String zkConnectStr) throws IOException {
        zookeeper = new ZooKeeper(zkConnectStr, 2000, null);
    }

    /**
     * 创建临时有序节点
     * @param path
     * @param node
     * @param watch
     */
    public String createZNodeES(String path, String node, Watcher watch) {
        try {
            watch(path, watch);
            return zookeeper.create(path + node,new byte[1], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 对某个节点添加监控
     * @param path
     * @param watcher
     */
    private void watch(String path,Watcher watcher) {
        try {
            zookeeper.getChildren(path, e -> {
                watcher.process(e);
                watch(path,watcher);
            });
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            zookeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取子节点列表
     * @return
     */
    public List<String> getChildNode(String parentPath) {
        try {
            return zookeeper.getChildren(parentPath, true);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除节点
     * @param znodePath 要删除的节点路径
     */
    public void deleteNode(String znodePath) {
        try {
            zookeeper.delete(znodePath,-1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
```

- 最后继承ReentrantLock 并重写lock和unlock方法

```
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
 * @create: 2018-08-06 09:33
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

```

- 下面写一个使用的例子，可以在多台机器上同时运行查看效果

```
/**
 * @program: test
 * @description: 测试
 * @author: 张乐
 * @create: 2018-08-06 10:16
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
```
