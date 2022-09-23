package com.test.server.nva.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.server.nva.NvaSession;
import com.test.server.nva.model.Format;
import com.test.server.nva.model.VideoInfo;
import com.test.server.utils.MapUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * receive nva:NvaRequest{seq=1,
 * payload={proj_type=1, epId=0, mobileVersion=6780300,
 * seekTs=21, sessionId=f162a661-4517-4f9f-9dea-7cbc6c56f745,
 * oid=515780389, type=0, userDesireQn=64,
 * isOpen=true, seasonId=0, accessKey=6d1f23bfafbc74ba7d5cdbf47cb00891,
 * otype=0, autoNext=true, biz_id=0, userDesireSpeed=1,
 * aid=515780389, contentType=0, cid=840381348,
 * danmakuSwitchSave=false, desc=0},
 * version=1, type='Command',
 * name='Play'}
 */
public class Play extends CommandExecutor {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static TypeReference<HashMap<String, Object>> typeRef
            = new TypeReference<HashMap<String, Object>>() {};
    private static final String PLAY_URL_TYPE_1 = "1";
    private static final String PLAY_URL_TYPE_2 = "2";

    private OkHttpClient client = new OkHttpClient();

    public Play(NvaSession session) {
        super(session);
    }


    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        try {
            VideoInfo videoInfo = getVideoUrl(payload);
            session.setCurrentVideoInfo(videoInfo);
            session.getController().prepare(videoInfo.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private VideoInfo getVideoUrl(Map<String, Object> payload) throws Exception {
        String cid = String.valueOf(payload.get("cid"));
        String oid = String.valueOf(payload.get("oid"));
        String qn = String.valueOf(payload.get("userDesireQn"));
        String key = String.valueOf(payload.get("accessKey"));
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
                .addQueryParameter("fourk", "0")
                .addQueryParameter("playurl_type", PLAY_URL_TYPE_1)//todo
                .addQueryParameter("protocol", "1")
                .addQueryParameter("cid", cid)
                .addQueryParameter("qn", qn)
                .addQueryParameter("object_id", oid)
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


    /**
     *
     * get video url result ....
     * {"code":0,"message":"0","ttl":1,
     * "data":{"result":"suee","from":"local","quality":64,
     * "format":"flv720","timelength":324707,
     * "accept_format":"hdflv2,flv,flv720,flv480,mp4",
     * "accept_description":["高清 1080P+","高清 1080P","高清 720P","清晰 480P","流畅 360P"],
     * "accept_quality":[112,80,64,32,16],"accept_watermark":null,
     * "video_codecid":7,"video_project":true,"seek_param":"start",
     * "seek_type":"offset",
     * "durl":[{"order":1,"length":324707,
     * "size":43402860,"ahead":"","vhead":"",
     * "url":"http://42.101.85.196/upgcxcode/05/66/839716605/839716605_nb3-1-64.flv?e=ig8euxZM2rNcNbRzhzdVhwdlhWhzhwdVhoNvNC8BqJIzNbfqXBvEuENvNC8aNEVEtEvE9IMvXBvE2ENvNCImNEVEIj0Y2J_aug859r1qXg8gNEVE5XREto8z5JZC2X2gkX5L5F1eTX1jkXlsTXHeux_f2o859IMvNC8xNbLEkF6MuwLStj8fqJ0EkX1ftx7Sqr_aio8_\u0026ua=tvproj\u0026uipk=5\u0026nbs=1\u0026deadline=1663944570\u0026gen=playurlv2\u0026os=bcache\u0026oi=2088697866\u0026trid=00008d594408010f4ebcaa56ebc9dec10e91T\u0026mid=30617445\u0026upsig=ca3aedff7e89dbeead98e45ef799dbbb\u0026uparams=e,ua,uipk,nbs,deadline,gen,os,oi,trid,mid\u0026cdnid=3842\u0026bvc=vod\u0026nettype=0\u0026bw=133959\u0026orderid=0,1\u0026logo=80000000",
     * "backup_url":null,"md5":""}],
     * "dash":null,"qn_extras":[{"qn":112,"need_vip":true,"need_login":false,"icon":"http://i0.hdslb.com/bfs/app/81dab3a04370aafa93525053c4e760ac834fcc2f.png","icon2":"http://i0.hdslb.com/bfs/app/4e6f14c2806f7cc508d8b6f5f1d8306f94a71ecc.png","attribute":0},{"qn":80,"need_vip":false,"need_login":false,"icon":"","icon2":"","attribute":0},{"qn":64,"need_vip":false,"need_login":false,"icon":"","icon2":"","attribute":0},{"qn":32,"need_vip":false,"need_login":false,"icon":"","icon2":"","attribute":0},{"qn":16,"need_vip":false,"need_login":false,"icon":"","icon2":"","attribute":0}],
     * "type":1,"support_formats":[{"quality":112,"format":"hdflv2","description":"高清 1080P+","new_description":"1080P 高码率","display_desc":"1080P","superscript":"高码率"},
     * {"quality":80,"format":"flv","description":"高清 1080P","new_description":"1080P 高清","display_desc":"1080P"},
     * {"quality":64,"format":"flv720","description":"高清 720P","new_description":"720P 高清","display_desc":"720P"},
     * {"quality":32,"format":"flv480","description":"清晰 480P","new_description":"480P 清晰","display_desc":"480P"},
     * {"quality":16,"format":"mp4","description":"流畅 360P","new_description":"360P 流畅","display_desc":"360P"}],
     * "has_dolby":false,"is_login":true,"is_vip":true,"degrade_type":0,"bp":0,"is_preview":0,"fnval":0,"fnver":0,
     * "no_rexcode":0,"has_paid":false,"status":0,"vip_type":0,"vip_status":0,"proj_vip_type":1,"clip":null,
     * "play_tag":null,"is_drm":false,"drm_tech_type":0}}
     */

    private VideoInfo parseVideoInfo(Map<String, Object> result) {
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
            String desc = String.valueOf(formatData.get("description"));
            videoInfo.addSupportFormat(new Format(qn, newFormat, desc));
        }
        return videoInfo;
    }
}
