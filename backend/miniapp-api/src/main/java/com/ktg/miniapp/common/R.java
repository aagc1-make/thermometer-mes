package com.ktg.miniapp.common;

import java.util.HashMap;
import java.util.Map;

/** 统一返回结构：{ code, msg, data } */
public class R {

    private int code;
    private String msg;
    private Object data;

    public R() {
    }

    public R(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static R ok(Object data) {
        return new R(200, "ok", data);
    }

    public static R ok() {
        return ok(null);
    }

    public static R fail(String msg) {
        return new R(500, msg, null);
    }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            result.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
