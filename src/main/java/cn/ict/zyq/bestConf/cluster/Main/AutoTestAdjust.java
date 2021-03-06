/**
 * Copyright (c) 2017 Institute of Computing Technology, Chinese Academy of Sciences, 2017
 * Institute of Computing Technology, Chinese Academy of Sciences contributors. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package cn.ict.zyq.bestConf.cluster.Main;

import cn.ict.zyq.bestConf.bestConf.sysmanipulator.ClusterManager;
import cn.ict.zyq.bestConf.cluster.Interface.*;
import cn.ict.zyq.bestConf.cluster.InterfaceImpl.SUTPerformance;
import cn.ict.zyq.bestConf.cluster.InterfaceImpl.SUTSystemOperation;
import cn.ict.zyq.bestConf.cluster.InterfaceImpl.SUTTest;
import cn.ict.zyq.bestConf.cluster.Utils.PropertiesUtil;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class AutoTestAdjust implements ClusterManager {
    private Properties pps;
    private String systemName;
    private String shellsPath;
    private String configFilePath;
    private String[] servers;
    private int numServers;
    private String[] users;
    private String[] passwords;
    private String serverAll;
    private String configFileStyle;
    private String interfacePath;
    private String localDataPath;
    private List<SystemOperation> cluster;
    private Test sutTest;
    private List<ConfigReadin> ConfR_cluster;
    private List<ConfigWrite> ConfW_cluster;
    private Performance systemPerformance;
    private boolean isInterrupt = false;
    private int maxRoundConnection;
    private int sutStartTimeout;
    private long testDurationTimeOutInSec;

    private int performanceType;
    private String targetTestServer;
    private String targetTestUsername;
    private String targetTestPassword;
    private String targetTestPath;
    private int targetTestErrorNum;
    private String perfsfilepath;
    private String configfilename;
    private String remoteConfigFilePath;
    private double performance;
    private int sshReconnectWatingtime;

    public AutoTestAdjust(String configFilePath) {
        this.configFilePath = configFilePath;
        cluster = new ArrayList<SystemOperation>();
        ConfR_cluster = new ArrayList<ConfigReadin>();
        ConfW_cluster = new ArrayList<ConfigWrite>();
        this.getProperties();

        perfsfilepath = localDataPath + "/perfsfile";

        for (int i = 0; i < numServers; i++) {
            try {
                // 调用自己实现的配置读写类，这里是为了扩展性和兼容性
                ConfigReadin readin = (ConfigReadin) Class.forName(interfacePath + "." + systemName + "ConfigReadin").newInstance();
                readin.initial(servers[i], users[i], passwords[i], localDataPath, remoteConfigFilePath);
                ConfR_cluster.add(readin);

                ConfigWrite write = (ConfigWrite) Class.forName(interfacePath + "." + systemName + "ConfigWrite").newInstance();
                write.initial(servers[i], users[i], passwords[i], localDataPath, remoteConfigFilePath);
                ConfW_cluster.add(write);

                //初始化操作目标主机的句柄
                SystemOperation so = new SUTSystemOperation();
                so.initial(servers[i], users[i], passwords[i], shellsPath, sutStartTimeout, maxRoundConnection, sshReconnectWatingtime);

                // 将主机加入到集群操作列表中
                cluster.add(so);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        sutTest = new SUTTest(testDurationTimeOutInSec);
        //初始化测试类，这个类主要用来：
		//启动测试
        //判断测试是否完成
        //终止测试
        //获取测试结果
        sutTest.initial(targetTestServer, targetTestUsername, targetTestPassword, targetTestPath, maxRoundConnection, sshReconnectWatingtime);

        //初始化性能类，这个类主要用来：
		//获取吞吐量
		//获取延迟
        systemPerformance = new SUTPerformance();

    }

    ;

    // 初始化一些参数，也就是从配置文件 SUTconfig.properties 中读取参数。如系统的 ip ，用户名，密码，系统名称，shell path等
    private void getProperties() {
        try {
            pps = PropertiesUtil.GetAllProperties(configFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // todo 这里被我注释了
        //sshReconnectWatingtime = Integer.parseInt(pps.getProperty("sshReconnectWatingtimeInSec"));
        //maxRoundConnection = Integer.parseInt(pps.getProperty("maxConnectionRetries"));
        //targetTestPath = pps.getProperty("targetTestPath");
        //remoteConfigFilePath = pps.getProperty("remoteConfigFilePath");
        //
        //performanceType = Integer.parseInt(pps.getProperty("performanceType"));
        //performanceType = performanceType > 2 ? 2 : performanceType;
        //
        //targetTestErrorNum = Integer.valueOf(pps.getProperty("maxConsecutiveFailedSysStarts"));
        //targetTestServer = pps.getProperty("targetTestServer");
        //targetTestUsername = pps.getProperty("targetTestUsername");
        //targetTestPassword = pps.getProperty("targetTestPassword");
        //systemName = pps.getProperty("systemName");
        //numServers = Integer.valueOf(pps.getProperty("numServers"));
        //servers = new String[numServers];
        //users = new String[numServers];
        //passwords = new String[numServers];
        //for (int i = 0; i < numServers; i++) {
        //    servers[i] = pps.getProperty("server" + i);
        //    users[i] = pps.getProperty("username" + i);
        //    passwords[i] = pps.getProperty("password" + i);
        //}
        //shellsPath = pps.getProperty("shellsPath");
        //serverAll = "";
        //interfacePath = pps.getProperty("interfacePath");
        //for (int i = 0; i < numServers; i++) {
        //    if (i != numServers - 1) {
        //        serverAll += this.servers[i];
        //        serverAll += ",";
        //    } else
        //        serverAll += servers[i];
        //}
        //localDataPath = pps.getProperty("localDataPath");
        //sutStartTimeout = Integer.parseInt(pps.getProperty("sutStartTimeoutInSec"));
        //testDurationTimeOutInSec = Long.parseLong(pps.getProperty("testDurationTimeoutInSec"));
    }

    private static int maxTry = 3;

    public boolean startTest(HashMap hmTarget, int num, boolean isInterrupt) {

        //将配置文件批量写入目标系统的配置文件
        for (int i = 0; i < numServers; i++) {
            HashMap hm = ConfR_cluster.get(i).modifyConfigFile(hmTarget);
            ConfW_cluster.get(i).writetoConfigfile(hm);
            ConfW_cluster.get(i).uploadConfigFile();
        }
        //批量启动目标系统
        for (int i = 0; i < numServers; i++) {
            cluster.get(i).start();
        }
        Boolean flag = true;
        boolean[] flags = new boolean[numServers];
        for (int i = 0; i < numServers; i++) {
            flags[i] = false;
        }

        //检查目标系统是否启动
        for (int i = 0; i < numServers; i++) {
            flags[i] = cluster.get(i).isStarted();
        }
        for (int i = 0; i < numServers; i++) {
            if (flags[i])
                continue;
            else
                flag = false;
        }

        //启动测试（每次测试之前，都先终止测试
        if (flag) {
            sutTest.terminateTest();
            sutTest.startTest();

            //获取性能数据
            performance = sutTest.getResultofTest(num, isInterrupt);
            System.out.println("performance is : " + performance);
            for (int i = 0; i < numServers; i++)
                cluster.get(i).stopSystem();
            systemPerformance.initial(performance, 1.0 / (Math.abs(performance) + 1));
            return true;
        }
        return false;
    }

    private double[] getPerf(String filePath) {
        double[] result = new double[2];
        File res = new File(filePath);
        try {
            int tot = 0;
            BufferedReader reader = new BufferedReader(new FileReader(res));
            String readline = null;
            while ((readline = reader.readLine()) != null) {
                result[tot++] = Double.parseDouble(readline);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    public static String getMD5(Instance ins) {
        StringBuffer name = new StringBuffer("");
        for (int i = 0; i < ins.numAttributes() - 2; i++) {
            name.append(Math.round(ins.value(ins.attribute(i))) + ",");
        }
        return getMD5(name.toString());
    }

    public static String getMD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writePerfstoFile(Instance ins) {
        File perfFolder = new File(perfsfilepath);
        if (!perfFolder.exists())
            perfFolder.mkdirs();

        File file = new File(perfsfilepath + "/" + getMD5(ins));
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(ins.value(ins.attribute(ins.numAttributes() - 1)) + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //运行测试用例的真正函数
    public Instances runExp(Instances samplePoints, String perfAttName) {
        Instances retVal = null;
        if (samplePoints.attribute(perfAttName) == null) {
            Attribute performance = new Attribute(perfAttName);
            samplePoints.insertAttributeAt(performance, samplePoints.numAttributes());
        }
        int pos = samplePoints.numInstances();
        int count = 0;
        for (int i = 0; i < pos; i++) {
            Instance ins = samplePoints.get(i);
            HashMap hm = new HashMap();
            int tot = 0;
            //把上面的值放到这里
            for (int j = 0; j < ins.numAttributes(); j++) {
                hm.put(ins.attribute(j).name(), ins.value(ins.attribute(j)));
            }

            boolean testRet;
            if (Double.isNaN(ins.value(ins.attribute(ins.numAttributes() - 1)))) {

                //批量启动测试
                testRet = this.startTest(hm, i, isInterrupt);

                double y = 0;
                if (!testRet) {// the setting does not work, we skip it
                    y = -1;
                    count++;
                    if (count >= targetTestErrorNum) {
                        System.out.println("There must be somthing wrong with the system. Please check and restart.....");
                        System.exit(1);
                    }
                } else {
                    // 获取性能数据： latency throughput
                    y = getPerformanceByType(performanceType);
                    count = 0;
                }

                ins.setValue(samplePoints.numAttributes() - 1, y);

                writePerfstoFile(ins);
            } else {
                continue;
            }
        }
        retVal = samplePoints;
        retVal.setClassIndex(retVal.numAttributes() - 1);

        return retVal;
    }

    public void shutdown() {
        for (int i = 0; i < numServers; i++) {
            cluster.get(i).shutdown();
        }
    }

    public double getPerformanceByType(int type) {
        double performance = 0.0;
        switch (type) {
            case 1: {   //indicates latency
                performance = systemPerformance.getPerformanceOfLatency();
                break;
            }
            case 2: {   //indicates throughput
                performance = systemPerformance.getPerformanceOfThroughput();
                System.out.println("throughput is " + performance);
                break;
            }
            default:
                break;
        }
        return performance;
    }

    /**
     * set the bestConf to cluster and get the running performance
     * @param attributeToVal
     * @return
     */
    public double setOptimal(Map<Attribute, Double> attributeToVal) {
        HashMap hm = new HashMap();
        for (Attribute key : attributeToVal.keySet()) {
            Double value = attributeToVal.get(key);
            hm.put(key.name(), value);
        }
        this.startTest(hm, 0, false);
        double y = 0;
        y = performance;
        return y;
    }

    public void test(int timeToTest, boolean isInterrupt) {
        HashMap yamlModify = new HashMap();
        yamlModify.put("concurrent_reads", 234);
        int tot = timeToTest;
        while (tot-- > 0) {
            startTest(yamlModify, 0, false);
            System.out.println("tot = " + tot);
        }
    }

    public static void main(String[] args) {
        AutoTestAdjust cym = new AutoTestAdjust("data/SUTconfig.properties");
        HashMap yamlModify = new HashMap();
        int tot = 4;
        while (tot-- > 0) {
            cym.startTest(yamlModify, 0, false);
            System.out.println("performance is : " + cym.getPerformanceByType(3));
            System.out.println("tot = " + tot);
        }


        cym.shutdown();
    }

    // 收集性能数据，位于机器的 /perfsfile 目录下，其实就是遍历该目录下的所有文件
    @Override
    public Instances collectPerfs(Instances samplePoints, String perfAttName) {
        Instances retVal = null;

        if (samplePoints.attribute(perfAttName) == null) {
            Attribute performance = new Attribute(perfAttName);
            samplePoints.insertAttributeAt(performance, samplePoints.numAttributes());
        }


        File perfFolder = new File(perfsfilepath);
        int tot = 0;
        if (perfFolder.exists()) {
            //let's get all the name set for the sample points
            //让我们把所有的采样点的名字都设置好
            Iterator<Instance> itr = samplePoints.iterator();
            TreeSet<String> insNameSet = new TreeSet<String>();
            HashMap<String, Integer> mapping = new HashMap<String, Integer>();
            int pos = 0;
            while (itr.hasNext()) {
                String mdstr = getMD5(itr.next());
                insNameSet.add(mdstr);
                mapping.put(mdstr, new Integer(pos++));
            }

            //now we collect
            File[] perfFiles = perfFolder.listFiles(new PerfsFileFilter(insNameSet));
            tot = perfFiles.length;
            if (tot > 0) isInterrupt = true;
            for (int i = 0; i < tot; i++) {
                Instance ins = samplePoints.get(mapping.get(perfFiles[i].getName()));
                double[] results = getPerf(perfFiles[i].getAbsolutePath());
                if (results != null) {
                    ins.setValue(samplePoints.numAttributes() - 1, results[0]);
                }
            }
        }
        retVal = samplePoints;
        retVal.setClassIndex(retVal.numAttributes() - 1);
        System.out.println("Total number of collected performances is : " + tot);
        return retVal;
    }

    class PerfsFileFilter implements FileFilter {
        Set<String> nameSet = null;

        public PerfsFileFilter(Set<String> nameSet) {
            this.nameSet = nameSet;
        }

        @Override
        public boolean accept(File file) {
            if (nameSet != null && nameSet.contains(file.getName()))
                return true;
            return false;
        }
    }

    @Override
    public void test(int timeToTest) {
    }
}
