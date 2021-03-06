package com.github.hairless.plink.rpc.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arronlong.httpclientutil.HttpClientUtil;
import com.arronlong.httpclientutil.common.HttpConfig;
import com.github.hairless.plink.common.FlinkConfigUtil;
import com.github.hairless.plink.model.exception.PlinkMessageException;
import com.github.hairless.plink.model.exception.PlinkRuntimeException;
import com.github.hairless.plink.rpc.FlinkRestRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: silence
 * @date: 2020/1/27
 */
@Slf4j
@Service
public class FlinkRestRpcServiceImpl implements FlinkRestRpcService {
    private static final String VERSION = "/v1";
    private static final String JARS = VERSION + "/jars";
    private static final String JARS_UPLOAD = JARS + "/upload";
    private static final String JARS_JARID = JARS + "/%s";
    private static final String JARS_JARID_RUN = JARS_JARID + "/run";
    private static final String JOBS = VERSION + "/jobs";
    private static final String JOBS_JOBId = JOBS + "/%s";

    //flink ui address
    private static final String JOB_UI_ADDRESS = "/#/job/%s";

    @Override
    public String uploadJar(String localJarPath) {
        try {
            HttpConfig httpConfig = HttpConfig.custom().url(FlinkConfigUtil.getRestAddress() + JARS_UPLOAD).files(new String[]{localJarPath});
            String resJson = HttpClientUtil.post(httpConfig);
            JSONObject flinkRestRes = JSON.parseObject(resJson);
            if (!"success".equals(flinkRestRes.getString("status"))) {
                throw new PlinkRuntimeException("upload jar to cluster fail");
            }
            String filename = flinkRestRes.getString("filename");
            //兼容windows
            filename = filename.replace("\\", "/");
            String[] filenames = filename.split("/");
            return filenames[filenames.length - 1];
        } catch (Exception e) {
            throw new PlinkRuntimeException("uploadJar error", e);
        }
    }

    @Override
    public void deleteJar(String jarId) {
        try {
            HttpConfig httpConfig = HttpConfig.custom().url(String.format(FlinkConfigUtil.getRestAddress() + JARS_JARID, jarId));
            String resJson = HttpClientUtil.delete(httpConfig);
            String errors = JSON.parseObject(resJson).getString("errors");
            if (errors != null) {
                throw new PlinkRuntimeException("deleteJar error:" + errors);
            }
        } catch (Exception e) {
            throw new PlinkRuntimeException("deleteJar error", e);
        }
    }

    @Override
    public String runJar(String jarId, RunConfig runConfig) {
        try {
            HttpConfig httpConfig = HttpConfig.custom().url(String.format(FlinkConfigUtil.getRestAddress() + JARS_JARID_RUN, jarId)).json(JSON.toJSONString(runConfig));
            String resJson = HttpClientUtil.post(httpConfig);
            String appId = JSON.parseObject(resJson).getString("jobid");
            if (appId == null) {
                throw new PlinkRuntimeException("runJar error:" + resJson);
            }
            return appId;
        } catch (Exception e) {
            throw new PlinkRuntimeException("runJar error", e);
        }
    }

    @Override
    public String queryJobStatus(String jobId) {
        try {
            HttpConfig httpConfig = HttpConfig.custom().url(String.format(FlinkConfigUtil.getRestAddress() + JOBS_JOBId, jobId));
            String resJson = HttpClientUtil.get(httpConfig);
            return JSON.parseObject(resJson).getString("state");
        } catch (Exception e) {
            throw new PlinkRuntimeException("queryJobStatus error", e);
        }
    }

    @Override
    public void stopJob(String jobId) {
        try {
            HttpConfig httpConfig = HttpConfig.custom().url(String.format(FlinkConfigUtil.getRestAddress() + JOBS_JOBId, jobId));
            String resJson = HttpClientUtil.patch(httpConfig);
            String errors = JSON.parseObject(resJson).getString("errors");
            if (errors != null) {
                throw new PlinkRuntimeException("stopJob error:" + errors);
            }
        } catch (Exception e) {
            throw new PlinkRuntimeException("stopJob error", e);
        }
    }

    @Override
    public String getJobUiAddress(String jobId) {
        try {
            return String.format(FlinkConfigUtil.getRestAddress() + JOB_UI_ADDRESS, jobId);
        } catch (Exception e) {
            throw new PlinkRuntimeException("getJobUiAddress error", e);
        }
    }


}
