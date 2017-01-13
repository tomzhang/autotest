package com.xn.common.sign;/**
 * Created by xn056839 on 2016/11/22.
 */

import com.xn.common.Exception.CaseErrorEqualException;
import com.xn.common.service.GetPara;
import com.xn.common.util.StringUtil;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.TreeMap;

import static com.xn.common.util.SignUtil.mapToString;
import static com.xn.common.util.SignUtil.md5;

public class PayAddSign {
    private static final Logger logger = LoggerFactory.getLogger(PayAddSign.class);

    public String PayHttpAddSing(TreeMap<String, String> treeMap, boolean useSign, String signType, String paramType, String signAddSignType) throws CaseErrorEqualException {
        String key = "";
        //只有useSign为true&&传入sign为空才会计算sign
        if (useSign) {
            String oldSign = treeMap.get("sign");
            if (StringUtils.isEmpty(oldSign)) {
                GetPara getPara = new GetPara();
                String path = getPara.getPath();
                File file = new File(path + "suite/key.properties");
                key = StringUtil.getConfig(file, "key", "");
                if (StringUtils.isEmpty(key)) {
                    throw new CaseErrorEqualException("no key");
                }


                treeMap.remove("sign");
                TreeMap<String, String> treeMapForSign = new TreeMap<String, String>();
                treeMapForSign.putAll(treeMap);
                if (signAddSignType.equals("false")) {
                    treeMapForSign.remove("sign_type");
                }
                String withSignType = mapToString(treeMap);
                String sign_sb = mapToString(treeMapForSign);
                String signPara = sign_sb + "&key=" + key;
                String sign = md5(signPara, treeMap.get("input_charset"));
                if (paramType.equalsIgnoreCase("form")) {
                    withSignType += "&sign=" + sign;
                    return withSignType;
                } else if (paramType.equalsIgnoreCase("json")) {
                    treeMap.put("sign", sign);
                    String request = JSONObject.fromObject(treeMap).toString();
                    return request;
                }
            }
        }

        if (paramType.equalsIgnoreCase("form")) {
            String sign_sb = mapToString(treeMap);
            return sign_sb;
        } else if (paramType.equalsIgnoreCase("json")) {
            String request = JSONObject.fromObject(treeMap).toString();
            return request;
        }
        return null;
    }

}
