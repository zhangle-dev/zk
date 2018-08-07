package com.zl.util;

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
