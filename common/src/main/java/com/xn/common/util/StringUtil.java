package com.xn.common.util;


import cn.xn.user.utils.RequestSignUtils;
import com.xn.common.Exception.CaseErrorEqualException;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

/**
 * Created by zhouxi.zhou on 2016/3/9.
 */
public class StringUtil {
    private final static Logger logger = LoggerFactory.getLogger(StringUtil.class);
    private static String SYSTEM_TYPE = "systemType";
    private static final String CHARSET = "UTF-8";

    public static String firstToLow(String s) {

        String str;
        if (s.length() > 2) {
            str = s.substring(0, 1).toLowerCase() + s.substring(1);
        } else {
            str = s;
        }
        return str;
    }

    public static String firstToUp(String s) {

        String str;
        if (s.length() > 2) {
            str = s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            str = s;
        }
        return str;
    }

    public static String getPro(String file, String properName) {
        Properties prop = new Properties();
        String value = null;
        InputStream in = Object.class.getResourceAsStream("/" + file);
        try {
            prop.load(in);
            value = prop.getProperty(properName).trim();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("properties file is not exist");
        } finally {
            return value;
        }
    }


    public static Boolean isEmpty(Object value) {
        if (value == null) return true;
        if (org.apache.commons.lang.StringUtils.isBlank(value.toString())) return true;
        return false;
    }

    public static boolean isJson(Object value) {
        if (!(value instanceof String)) return false;
        String json = value.toString();
        return json.startsWith("{") && json.endsWith("}");
    }

    public static String getConfig(File file, String properName, String defaultValue) {

        List<String> lines = FileUtil.fileReadeForList(file);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                if (line.contains("=")) {
                    String split[] = line.split("=", 2);
                    if (split[0].equals(properName)) {
                        return split[1];

                    }

                }
            }
        }
        return defaultValue;
    }

    public static String md5(String str) {
        return encrypt(str, CHARSET);
    }

    private static String encrypt(String str, String charset) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (org.apache.commons.lang.StringUtils.isNotBlank(charset)) {
                md.update(str.getBytes(charset));
            } else {
                md.update(str.getBytes());
            }

            byte b[] = md.digest();

            int i;

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            str = buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return str;
    }

    public static String httpAddSign(TreeMap<String, String> map, boolean useSign) throws CaseErrorEqualException {
        String key = "";
        if (useSign) {
            if (!map.containsKey("systemType")) {
                throw new CaseErrorEqualException("no systemType");
            }
            if (map.containsKey("sign_type")) {
                map.remove("sign_type");
            }
            key = getPro("test.properties", "key." + map.get("systemType"));
        }
        if (key.equals("") && useSign) {
            throw new CaseErrorEqualException("no key");
        }
        return addSign(map, key, useSign);
    }

    public static List<String> dubboAddSign(List<String> lines) throws CaseErrorEqualException {
        Collections.sort(lines);
        String param = "";
        String key = "";
        for (String line : lines) {
            if (!line.startsWith("#") && line.split("=").length == 2) {

                param += line + "&";
                if (line.startsWith("systemType")) {
                    String value = line.split("=")[1];
                    key = getPro("test.properties", "key." + value);
                }
            }
        }


        if (!key.equals("")) {
            param += "key=" + key;
        }
        if (param.equals("")) {
            throw new CaseErrorEqualException("all parameters are null");
        } else if (!param.contains("systemType")) {
            logger.error("systemType is not exist,cannot add sign");
            throw new CaseErrorEqualException("systemType is not exist,cannot add sign");
        }

        String sign = md5(param);

        lines.add("sign=" + sign);
        return lines;

    }


    //生成随机手机号
    public static int getNum(int start, int end) {
        return (int) (Math.random() * (end - start + 1) + start);
    }

    public static String addLoginName() {
        String[] telFirst = "134,135,136,137,138,139,150,151,152,157,158,159,130,131,132,155,156,133,153".split(",");
        int index = getNum(0, telFirst.length - 1);
        String first = telFirst[index];
        String second = String.valueOf(getNum(1, 888) + 10000).substring(1);
        String thrid = String.valueOf(getNum(1, 9100) + 10000).substring(1);
        return first + second + thrid;

    }


//    public static String setSign(Object obj) {
//
//        TreeMap<String, String> map = beanToSortMap(obj);
//
//        String systemType = map.get(SYSTEM_TYPE);
//
//        String key = getPro("test.properties", "key." + systemType);
//
//        String addSign = addSign(map, key);
//        if (org.apache.commons.lang.StringUtils.isNotEmpty(addSign)) {
//            return addSign;
//        }
//        return null;
//    }

    public static TreeMap<String, String> beanToSortMap(Object obj) {
        TreeMap<String, String> params = new TreeMap<String, String>();
        try {
            PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
            PropertyDescriptor[] descriptors = propertyUtilsBean.getPropertyDescriptors(obj);
            for (int i = 0; i < descriptors.length; i++) {
                String name = descriptors[i].getName();
                if (!org.apache.commons.lang.StringUtils.equals(name, "class")) {
                    Object o = propertyUtilsBean.getNestedProperty(obj, name);
                    if (o != null)
                        params.put(name, o.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    public static String addSign(TreeMap<String, String> treeMap, String key, boolean useSign) {
        //遍历签名参数
        StringBuilder sign_sb = new StringBuilder();

        Iterator<String> it = treeMap.keySet().iterator();
        while (it.hasNext()) {
            String mapKey = it.next();
            if (org.apache.commons.lang.StringUtils.isEmpty(treeMap.get(mapKey)))
                continue;
            if (org.apache.commons.lang.StringUtils.isEmpty(sign_sb.toString())) {
                sign_sb.append(mapKey + "=" + treeMap.get(mapKey));
            } else {
                sign_sb.append("&" + mapKey + "=" + treeMap.get(mapKey));
            }
        }
        if (useSign) {
            String signPara = sign_sb.toString() + "&key=" + key;
            String sign = md5(signPara);
            sign_sb.append("&sign=" + sign);
            return sign_sb.toString();
        } else {
            return sign_sb.toString();
        }
    }

    public static boolean validateSign(Object obj) {

        TreeMap<String, String> map = RequestSignUtils.beanToSortMap(obj);
        String sign = map.remove("sign");


        String key = getPro("test.properties", "key.QGZ");

        String addSign = RequestSignUtils.addSign(map, key);
        if (StringUtils.isNotEmpty(addSign) && addSign.equals(sign)) {
            return true;
        } else {
            logger.info("isPassSign@, 签名错误，传入签名:{},计算出的签名:{}", sign, addSign);
            return false;
        }
    }

    public static void main(String[] args) {
//        System.out.println(addLoginName());
//        System.out.println(md5("appVersion=2.4.0&friendJson=[{\"friendMobile\":\"111111111114\",\"friendName\":\"测试05\"}]&memberNo=8e299dbf-fd2a-4282-966a-d1f946683133&mobile=1232545&sourceType=android&systemType=QGZ&key=J1IGqSYgjv0pPF6TIgXu4G8KAp6rkd3T"));
        System.out.println(md5("appVersion=2.4.0&loginName=13480979901&loginPwd=123456&sourceType=android&systemType=QGZ&key=J1IGqSYgjv0pPF6TIgXu4G8KAp6rkd3T"));
//        appVersion=2.4.0&day=false&days=3&hour=72&memberNo=b4f3904f-cfe7-4572-8a07-f0e48bb80a74&refereeNo=1256097325667&sourceType=android&systemType=QGZ&key=J1IGqSYgjv0pPF6TIgXu4G8KAp6rkd3T


//        String s = getSign("checkLoginReq.setAppVersion(\"2.4.0\");\n" +
//                "\t\tcheckLoginReq.setMemberNo(\"05135e9b-7fdb-478a-ba40-569bdf8b7331\");\n" +
//                "\t\tcheckLoginReq.setSign(\"%\");\n" +
//                "\t\tcheckLoginReq.setSourceType(\"android\");\n" +
//                "\t\tcheckLoginReq.setSystemType(\"QGZ\");\n" +
//                "\t\tcheckLoginReq.setTokenId(\"ef8bd754-5124-47c1-b763-57b29010ea45\");");
//
//        System.out.println(s);

//        String str = (String) JOptionPane.showInputDialog(null, "参数：\n", "获得sign", JOptionPane.PLAIN_MESSAGE, null, null,
//                null);
//        JOptionPane.showMessageDialog(null, getSign(str), "显示ID",JOptionPane.PLAIN_MESSAGE);
//
//        UpdateRefereeReq updateRefereeReq=new UpdateRefereeReq();
//        updateRefereeReq.setAppVersion("2.4.0");
//        updateRefereeReq.setMemberNo("b4f3904f-cfe7-4572-8a07-f0e48bb80a74");
//        updateRefereeReq.setDay(false);
//        updateRefereeReq.setRefereeNo("1256097325667");
//        updateRefereeReq.setDays(3);
//        updateRefereeReq.setSourceType("android");
//        updateRefereeReq.setSystemType("QGZ");
//        validateSign(updateRefereeReq);

    }

}