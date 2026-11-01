package vn.hometube.app.core

import java.security.MessageDigest

object RemotePolicy {
    fun check(expected:String,token:String?,origin:String?,host:String,write:Boolean,length:Long):Int {
        if(token==null||!MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8),token.toByteArray(Charsets.UTF_8)))return 403
        if(write&&origin!=null&&origin!="http://$host")return 403
        if(write&&length<0)return 411
        if(write&&length !in 1..16384)return 413
        return 200
    }
}
