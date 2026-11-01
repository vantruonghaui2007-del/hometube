package vn.hometube.app.core

import org.junit.Test
import org.junit.Assert.*

class RemotePolicyTest {
    @Test fun tokenIsRequired(){assertEquals(403,RemotePolicy.check("key",null,null,"192.168.1.2:8687",false,0));assertEquals(403,RemotePolicy.check("key","wrong",null,"host",false,0))}
    @Test fun validPairCanRead(){assertEquals(200,RemotePolicy.check("key","key",null,"host",false,0))}
    @Test fun crossSiteMutationDenied(){assertEquals(403,RemotePolicy.check("key","key","http://evil.test","192.168.1.2:8687",true,20))}
    @Test fun sameOriginMutationAllowed(){assertEquals(200,RemotePolicy.check("key","key","http://192.168.1.2:8687","192.168.1.2:8687",true,20))}
    @Test fun oversizeAndAbsentLengthRejected(){assertEquals(413,RemotePolicy.check("key","key",null,"host",true,20000));assertEquals(411,RemotePolicy.check("key","key",null,"host",true,-1))}
}
