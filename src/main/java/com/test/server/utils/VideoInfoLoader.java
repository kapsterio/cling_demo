package com.test.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.server.nva.model.Format;
import com.test.server.nva.model.VideoInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoInfoLoader {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static TypeReference<HashMap<String, Object>> typeRef
            = new TypeReference<HashMap<String, Object>>() {};
    private static OkHttpClient client = new OkHttpClient();

    public static VideoInfo loadVideoInfo(int cid, int epId, int oid,
                                          int qn, String key) throws Exception {
        System.out.println("cid: "+ cid + " epId: " + epId +  " oid: " + oid + " qn: " + qn + " key: " + key);
        int objectId = epId == 0 ? oid : epId;
        int playUrlType = epId == 0 ? 1 : 2;
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.bilibili.com")
                .addPathSegment("x")
                .addPathSegment("tv")
                .addPathSegment("playurl")
                .addQueryParameter("build", "104600")
                .addQueryParameter("is_proj", "1")
                .addQueryParameter("device_type", "1")
                .addQueryParameter("mobi_app", "android_tv_yst")
                .addQueryParameter("platform", "android")
                .addQueryParameter("fnval", "0")
                .addQueryParameter("fnver", "0")
                .addQueryParameter("fourk", "1")
                .addQueryParameter("playurl_type", String.valueOf(playUrlType))//todo
                .addQueryParameter("protocol", "1")
                .addQueryParameter("cid", String.valueOf(cid))
                .addQueryParameter("qn", String.valueOf(qn))
                .addQueryParameter("object_id", String.valueOf(objectId))
                .addQueryParameter("mobile_access_key", key)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            System.out.println("get video url result ...." + json);
            Map<String, Object> result = objectMapper.readValue(json, typeRef);
            return parseVideoInfo(result);
        }

    }

    private static VideoInfo parseVideoInfo(Map<String, Object> result) {
        if (!result.get("code").equals(0)) {
            System.out.println("fail to parse video info .........");
            return null;
        }
        String format = MapUtil.get(result, "data", "format");
        Integer duration = MapUtil.get(result, "data", "timelength");
        Integer quality = MapUtil.get(result, "data", "quality");
        List<Map<String, Object>> durl = MapUtil.get(result, "data", "durl");
        String url = MapUtil.get(durl.get(0), "url");
        Integer size = MapUtil.get(durl.get(0), "size");

        VideoInfo videoInfo = new VideoInfo(url, format, duration, size, quality);
        List<Map<String, Object>> supportedFormats = MapUtil.get(result, "data", "support_formats");
        for (Map<String, Object> formatData : supportedFormats) {

            Integer qn = MapUtil.get(formatData, "quality");
            String newFormat = String.valueOf(formatData.get("format"));
            String desc = String.valueOf(formatData.get("new_description"));
            String displayDesc = String.valueOf(formatData.get("display_desc"));
            String superScript = (String) formatData.get("superscript");

            videoInfo.addSupportFormat(String.valueOf(qn), new Format(newFormat, desc, displayDesc, superScript));
        }

        List<Map<String, Object>> qnExtras = MapUtil.get(result, "data", "qn_extras");
        for (Map<String, Object> qnExtra : qnExtras) {
            Integer qn = MapUtil.get(qnExtra, "qn");
            Boolean needVip = MapUtil.get(qnExtra, "need_vip");
            Boolean needLogin = MapUtil.get(qnExtra, "need_login");
            Format formatObj = videoInfo.getSupportedQuality().get(String.valueOf(qn));
            if (formatObj != null) {
                formatObj.setQn(qn);
                formatObj.setNeedVip(needVip);
                formatObj.setNeedLogin(needLogin);
            }
        }
        return videoInfo;
    }

}
