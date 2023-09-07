package io.branch.referral.test.mock;

import static io.branch.referral.Defines.RequestPath.GetURL;
import static io.branch.referral.Defines.RequestPath.IdentifyUser;
import static io.branch.referral.Defines.RequestPath.QRCode;
import static io.branch.referral.Defines.RequestPath.RegisterInstall;
import static io.branch.referral.Defines.RequestPath.RegisterOpen;

import org.json.JSONObject;

import java.util.UUID;
import io.branch.referral.BranchLogger;
import io.branch.referral.BranchTest;
import io.branch.referral.PrefHelper;
import io.branch.referral.network.BranchRemoteInterface;

public class MockRemoteInterface extends BranchRemoteInterface {
    private final static String TAG = "MockRemoteInterface";

    // TODO: Revisit with MockWebServer and mock out different response codes
    // since most tests use TEST_TIMEOUT to await network requests, lower it here, so TEST_TIMEOUT
    // ends up including a little bit of a buffer for scheduling network requests.
    private final long networkRequestDuration = BranchTest.TEST_REQUEST_TIMEOUT / 2;

    @Override
    public BranchResponse doRestfulGet(String url) throws BranchRemoteException {
        try {
            Thread.sleep(networkRequestDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BranchLogger.v(TAG + ", doRestfulGet, url: " + url);
        return new BranchResponse(pathForSuccessResponse(url), 200);
    }

    @Override
    public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
        try {
            Thread.sleep(networkRequestDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BranchLogger.v(TAG + ", doRestfulPost, url: " + url + ", payload: " + payload);
        return new BranchResponse(pathForSuccessResponse(url), 200);
    }

    public static String pathForSuccessResponse(String url) {
        if (url.contains(GetURL.getPath())) {
            return "{\"url\":\"https://bnc.lt/l/randomized_test_route_" + UUID.randomUUID().toString() + "\"}";
        } else if (url.contains(IdentifyUser.getPath())) {
            return "{\"session_id\":\"880938553235373649\",\"randomized_bundle_token\":\"880938553226608667\",\"link\":\"https://branchster.test-app.link?%24randomized_bundle_token=880938553226608667\",\"data\":\"{\\\"+clicked_branch_link\\\":false,\\\"+is_first_session\\\":false}\",\"randomized_device_token\":\"867130134518497054\"}";
        } else if (url.contains(RegisterInstall.getPath()) || url.contains(RegisterOpen.getPath())) {
            return "{\"session_id\":\"880938553235373649\",\"randomized_bundle_token\":\"880938553226608667\",\"link\":\"https://branchster.test-app.link?%24randomized_bundle_token=880938553226608667\",\"data\":\"{\\\"+clicked_branch_link\\\":false,\\\"+is_first_session\\\":false}\",\"randomized_device_token\":\"867130134518497054\"}";
        } else if (url.contains(QRCode.getPath())) {
            return "{iVBORw0KGgoAAAANSUhEUgAAAgAAAAIACAYAAAD0eNT6AAAAAXNSR0IArs4c6QAAAARzQklUCAgI\n" +
                    "CHwIZIgAACAASURBVHic7d15tCVlYe7/561h7zPPPdJNAw0ILTSTggjBERWnq6KJN/FeTWLudSUm\n" +
                    "DiuuFZKb+ItZ1xvNYDRiojEYB0RFURABgUbGBqGZoRka6Hk83Wce9lT1/v6o002rQO9zTp29a+/3\n" +
                    "+1lrJyHdvXdV7aGeeuutp8yatWutAACAU7x6LwAAAKg9AgAAAA4iAAAA4CACAAAADiIAAADgIAIA\n" +
                    "AAAOIgAAAOAgAgAAAA4iAAAA4CACAAAADiIAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4CACAAAA\n" +
                    "DiIAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4CACAAAADiIAAADgIAIAAAAOIgAAAOAgAgAAAA4i\n" +
                    "AAAA4CACAAAADiIAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4CACAAAADgrqvQCHO/VH19R7Eerm\n" +
                    "0YvfWe9FqJtq3ne2z/zVchvWcpnT+vxkbZnTktb7nrXt3Iiy9jvGCAAAAA4iAAAA4CACAAAADiIA\n" +
                    "AADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4KBMFQFVI2tFCtVIq/iilkUctXyeamStOKWW6561Ipe0\n" +
                    "ZG37pPVaWSsUasTiHZd/52uJEQAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxE\n" +
                    "AAAAwEENVwRUjayVxtTytRpx3bNWXlTLbVjL0phqpLWdm7UAqpaatWgrLS5/NtLCCAAAAA4iAAAA\n" +
                    "4CACAAAADiIAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4KCmLALCS6tl8U41GrGEpBHLi2pZJNWI\n" +
                    "72ktP/NZK26qRrOW4biMEQAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAA\n" +
                    "wEEUATkoayU/jViG06xFN7V8nmpkrXCpGo1YmNOIy4z5YwQAAAAHEQAAAHAQAQAAAAcRAAAAcBAB\n" +
                    "AAAABxEAAABwEAEAAAAHEQAAAHBQUxYBNWupRdYKT7L2PLWUtUKhWn7ma/l+Ze2zkbUSpGrUsvir\n" +
                    "lhpxmbOGEQAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEENVwSUtWIQ\n" +
                    "vLS0CnNqWXBUS1krbkqrXKVZ3/esrVezfn4a8bvciBgBAADAQQQAAAAcRAAAAMBBBAAAABxEAAAA\n" +
                    "wEEEAAAAHEQAAADAQQQAAAAclKkioLRKJFzWiGUvqI2sfb8asQynGs36/arleqE2GAEAAMBBBAAA\n" +
                    "ABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxk1qxda+u9EFmVtRKbrBWnNGKhR9aK\n" +
                    "U7L2GUtLLdc9a5/DZv0OZq2UqRHXPWsYAQAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxE\n" +
                    "AAAAwEEEAAAAHBTUewEaXdbKKLKmWctDGvG9aES1fL+yVrSVlqx9d2r5Ws1a4JMWRgAAAHAQAQAA\n" +
                    "AAcRAAAAcBABAAAABxEAAABwEAEAAAAHEQAAAHAQAQAAAAc1ZRFQLYsvmrUQppalKNVIa3myVgxC\n" +
                    "CdL8pbU8WVuvrBXU1PK7w3tRG4wAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4CACAAAADiIAAADg\n" +
                    "IAIAAAAOMmvWrrX1XojZSKuMopZFE1krkcjaNqxl6VDWykyy9lpZK0pq1hKkRvzMN+JnoxHXq5YY\n" +
                    "AQAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEEEAAAAHBTUewFmK61ChqwVVlSj\n" +
                    "Ecsoavl+pbWdG7HspRq1LL9yWdaKyGrJ5VKdRlx3RgAAAHAQAQAAAAcRAAAAcBABAAAABxEAAABw\n" +
                    "EAEAAAAHEQAAAHAQAQAAAAeZNWvX2novxEFZK2BJ67UaUS2LSrJWdJO197RZ34tmLczJ2u9Ps5ZN\n" +
                    "ZW3dGxEjAAAAOIgAAACAgwgAAAA4iAAAAICDGu5ugGgu5Z69Omndf6TzXNqbyvOktTxpSWu9qlHL\n" +
                    "96Ka16rluqdlNusVjixZ6MUBXhQjAAAAOIgRANSZqfcCAICTGAEAAMBBDVcEVMvShlqWmWTtedJy\n" +
                    "pOU5ad3Xa7QkQPY8+YYPv+ifZa0EKWu/vbXUrGVBjAAAAOAg5gCgrpgBAAD1QQBAnREBAKAeCACo\n" +
                    "r8zMQAEAtzAHAAAABxEAAABwEAEAAAAHEQAAAHBQUxYBZa1QqJbSKtBIa/scaXkq3ftTeR2gEQWj\n" +
                    "A/VehKpl7be3GYvRao0RAAAAHMRlgKivzIw/AYBbGAEAAMBBBAAAABzEKQDUGVXAAFAPjAAAAOAg\n" +
                    "AgAAAA4iAAAA4KBMzQHIWklCIxZEpFV8UbNCDy4DXBjGyHie5HmSZ2SMkbVWiq1k4+e3u5FkfvPv\n" +
                    "2Dia+bu8QY0ua2U4zVrU1ojLnKkAABcxCTAVRjKeLwW+JCmeLqiyb0jlPYMq79qn8p79qgyNKBqb\n" +
                    "UDxdkC1Xkn8W+PLaWuV3tisY6FW4dJHCo5YoXLZYQV+3vJYW2TiWrURSHNdzDQGkjAAANDAT+DJB\n" +
                    "oLhYUmn7Lk0/vknTjzylwjNbVN49qHh8SrZcTo7sJRljJPNrocvaX/lzkwvld3UqXLlErSefoLYz\n" +
                    "1qhlzWoF/X2SbBIeGBkAGh4BAGg0nicTBlK5ouKz2zRx90OavOdBFZ7ZqnhsQlaS8b1kRMAzMvnc\n" +
                    "EcdZfv3Po9ExVYZGNPXARg19/2cKl/Sr7cyXq/N1r1Lb6Wvkd7YrLpUZFQAaGAEAaBDG92XCQJWh\n" +
                    "EU2sf0BjN92l6UefUjQ+mfxZ4Fe1s6+K58kcDBqSyvuGNPrTX2j0htvVsnqVui66QF1vPE/h4n6C\n" +
                    "ANCgCACoM+YAvDQr4/tSEKi8Y49Gr79NYzfeodL23ZIkEwbyWvILvhTG9yQ/J0kqPLNF0194VsNX\n" +
                    "Xq+ed75RPe94g4L+HsWl0sypAd5ToBEQAIAsspJ8T14YqrR9l4Z/cpPGbrhN5cEhmTCQyYV1WzQT\n" +
                    "BDJBoPLe/dr3le9o9Lpb1f+Bd6nrzb8lE4aHJhgCyDYCAOqLuWS/yRiZfKjKgWEd+PGNGvnJjSoP\n" +
                    "DsnLhTU52q+W8X0Z31dpxx7t/uxXNLZuvRZ95HfVetLxh40GAMgqAgCQIckRdFmjP12nA9/+sYpb\n" +
                    "d2Zux//rTOBL8pOJiE88o/4PvVe9F79Fxvdlo6jeiwfgRZg1a9cS0zOgEUuH0nDyz79Z70XIBs+T\n" +
                    "lws0/fgmDX71Ck3e+3AyEW/muv6GEcey5Yo6X3+ulnz8DxQs6pctleq9VJkVTPTO699n6btcrbSK\n" +
                    "gGr5PNVoxPeCEQDUGRPGTBgqnpzS4Dd+qKErf6Z4YlIml6v3Ys2N58nkcxpbt16lrTu17JI/Vssp\n" +
                    "J8kWi/VeMgC/hnsBAHXkteRVeOpZbf/k32nwP78vWyg17s7/MF5LXsXN27X9U/9P47+4W16+8dcJ\n" +
                    "aDYEAKAeTFLQM3r9rdr+yb/T9GNPJ+f5veYZETFhqGh8Qrs/8y8avvqmpgg2QDPhFADqy8UZKF5y\n" +
                    "o54Dl12p/d/4gay1db2sbyEZ35etRNr7j1+TLZbU+763JXMCXHzfgYwhAKDOmueI98hscnc+K+37\n" +
                    "8jc19P1rk2v6vSYfiPM8KY6171//S/I89V78VtkiEwOBeiMAoK5c2v0f3Pnv/eJlGrrqenm58Ddv\n" +
                    "zNOsDoaAL31Dfmurut/2etkCEwOBemryQw8gI4yRMZ72feVbGr7q+mRSnCs7/4M8Tza22vPP/6HJ\n" +
                    "9fczMRCoMwIAUAMmDLX/Wz/S0A9+2rTn+6thfE9xoaBdf3+pCs9skQnd3RZAvWWqCCitYgeXXyst\n" +
                    "tVrmNTd8Z97PkW1WXj6vkZ+t066/vzQ53+/akf8LsKWSWtecoJX/8NfyOtqlOJJjJ4QkSf5k14v+\n" +
                    "mau/CWm+lsslP9VgBAD1ZZv7YcK8Jh/aqD1fukySYec/w+Rymnrsae37yrdkjCfJq/t7VZcHUEcE\n" +
                    "AGCh+L4qQ8Pa809fTdr9fL5uh/Na8hq5bp1GrlvHfACgDvhFAhaCkYwx2vfv31Zh02bOdb8Yz9fg\n" +
                    "1y5X4dltMgEXJQG1RAAAFoCXy2vsljs1esOt8vLZvZNfvRnfU+XAsAa/+i3ZOOIUCVBDBADUmWm6\n" +
                    "h/EDlfcd0ODXr3h+FfGiTD6n8bvu09iNt8vk8qr3+1fbB1A/BAAgTVYyvq8D3/2xitt2Nt7tfOvF\n" +
                    "eNr/rStVGTzQ/M2IQEbwTUN91XsWduqz/gNNbXxaIz+7mYlts2ACX8VtuzT0w2tlgrDu7yNXAcAF\n" +
                    "BAAgTVYauuIniienOJ89S14u1Mi1N6m4ZZuMz8gJsNAyNe02rWKHrJVRNOt6pfM8zbOTNGGoqYcf\n" +
                    "1/hd9zZW25+1stZKsZVs/Px/HzxCNcn/OPhOWf3qnxljJM97vuRorsHH81QZGtHQVddp2Sc/IhvF\n" +
                    "81otF6RVdJOWZv0dq0YjFr5lKgAAjc1q+CfXyRZLMhke/rdRnDTvGSPj+zL5vIKONnkd7Qq6u+R3\n" +
                    "dcrr7JDf1SGvpUVePicTBjM3M7KylUhxoai4UFA0MqbK8IiiA8OqDA2rMjImWy7P+ZI+LxdqbN0d\n" +
                    "6nvP25RbuVy2EqW89gAOIgAAKTBBoMKmzZq45/5sHP1bKxtFUhTLysrL5WSCQHG5rPzK5Wp52Wrl\n" +
                    "jlqq/OpjlD96pYKBPinwZYIgGX73THJkf+j5fu35zcGXsclrlEqKJiZVOTCskZ/eqOFrbphb94Hn\n" +
                    "qTI8qpEbbtGSj3yIAAAsIAIA6qtJJkIZz9fYjbcpGp+U11KH6/6tZONItlKRMUZeR4fyy5cof+wx\n" +
                    "ajnxOLWcuFoHrviRJtbfp8romKYe3qipRzbKa21VuGyJ2k8/RR3nna3cyqNkKxWpFFX51swkAc+X\n" +
                    "392tcNGA/K4ujd/xS0VjY8mowSyZMNDYLXeq/73vlNfVKcWcCgAWAgEAddYEcwBmzl2P33lPMlRe\n" +
                    "Q7ZSkY0iebmc8iuPUuvLT1LbaS9Xy4mrFS5dLK+tTcYzku9r+JobJCvFE5OKxyclJef6i89t1fgd\n" +
                    "9yi8/EfqvugN6n//u+X3dMuWy7NYEEnWKi6UFC5doo5zztTwdTfNqQTJ+L7Ku/Zo4pcPqOetFyou\n" +
                    "Fmf9HACOjAAAzJMXBBp/4BGVdu6pzfB/HMuWKzJhoJbVx6j97LPUcfYZyh9/rPzOzkND93GpJFsq\n" +
                    "ySrp3Q96e2StTf788Il9vi8jKZqY0P7Lf6jJDQ9p2ac+qpaTTpAtlea0iB3nn6ORn98yj5U0GvvF\n" +
                    "nep+0+vm8RwAXgoBAJgna63G77wn2bku5OtEsVQpy+/rVcerXqHuN1yg1pe/TH5Hh2wcJ6MBpZKs\n" +
                    "tSrt2KX8MUcn8wBm+F2dL/0CnievJa/Cpue0468/q6P+9i/UuuZE2dIsRgKUjEq0nnSCgoE+RUMj\n" +
                    "czsNEASa3viUSjt2MRkQWCD0AKDO6l3FOs+H56u8f0hTjz4hb6GG/+NYtlhU2N+rgQ/+dx3z5c9r\n" +
                    "+SUfV/vZZ8rk8oqLJdlyRbJWJp/X2K3rteVPL9HYujtmhuCNZKWgr/dXJ/a9CJMLVd67X7s/9yVV\n" +
                    "9g/JeP7stkls5ff1/kYAmRXPqDI6rsmHHpPxg9m9fkM9gPohAKC+6t3ENt/mvyBQ4YlNqgwemNOR\n" +
                    "7hE3T7ks09Ki/t99r1Z9+XNa/Ef/U7nlS2VLZdlieeba/Zll8XyVd+/T4GXfUTwxod3/dKlGfv4L\n" +
                    "efm8rJX87i6pylsSm1yowrNbtP+b35e8mVKeWWwXLwiVP+6Y+V3Lb6SpBx6Rje2sX79hHkAdOXsK\n" +
                    "oJaFDI1YjlGNrJVa1IOR0dRDj8pGUeoTAG0lUuvLT9LSP/vfajnpRKlSUVx48QlxJgg1cu3PVdq5\n" +
                    "W15Li+JSSXv+6VIZY9T91gvld3Yk1+dbVXXw6eVzGr35NvW84y1qWX1scnVA1Qsv5Y5aPq+DXOP7\n" +
                    "Kmx6TtHomLy2Nrm2x8za70Y1all6Vs3zZK0oKWsYAQDmyhjFhYKmn3hqYapro0i9/+2taj3lZNli\n" +
                    "8aWH041RZXRM47evT7r0lexA41JJu//pUo2tu13hooGZa/Or3JEao2h8QuO33jXr9bPWJqcc5rFd\n" +
                    "jOepvP+Ayrv3Ug0MLAACAOqs3udg5/4wnq/y4JBKO3cvzF3/fF9+e3tyfv9IyxIEKm7ZptKuPTKH\n" +
                    "DfMb31dcLGr357+kyQ0Pye9ok2z1R9LG9zX18KPJpXhmFtvHSn5b2/x23MbIFgoq7dgl+bOch9Aw\n" +
                    "D6B+CADAXPm+Stt3KBqb0ELc+Md4RiaXq+qA3Xi+Stt3yRZLv7EsxvcVTU1p71f+M1nW2cxV8D2V\n" +
                    "9w3OrONsfi6sFATJssxj5N7GNgk13FgJSB0BAJgjY4yKm7fO7tz4AqqMjCTVvC/AeN6h0qDZMMYo\n" +
                    "ni4qnpya5U7YJA1+1s7vQNdIlf1DL7peAObO2UmAyAbTyL/rsVVp244FG8m1cZzcWEhHPoo2VvKO\n" +
                    "dIQ+16No8/xrVD19QEZ2alo2juc3D8AYRcMjMlHs2hxAYMERAFBnjTu0a8sVlXbvTW6DuxDiWPH4\n" +
                    "hIzxdOSKIZNMuvPS3Z42tvJaW+V3dM5MHaju+Y0xqoyNS1E0c/5+joxRZXxCNqry0gUAVeMUADAX\n" +
                    "xiieLqgyNLwg1/9LM+e/9w5W93ejSLmjV8q0tMxqkt8RxZFyy5fJ7+yY3U15jFF5157nr+GfK2Nk\n" +
                    "p6eT0yzMAwBSRQAA5sAYo3hqUvHExIJNUDPGqLR9u2wVO14bRcqvWqn8qpVzb997weeN1X7W6fJy\n" +
                    "uVn/u+LWbfMfkTBGcbEkZWSeBdBMMnUKoJYFEbUssanlMlPOUyOep2h8MinmWagjU99XYfM2xVNT\n" +
                    "M1cDvMTRtLXy2tvV/cbXafrJp5PCn/mKYwV9veq64LzZTXQ0nqLxCRWe2zK/4f8ZtlKWjWJOALyA\n" +
                    "WhbdpPU7Vo2srVctn6eWGAFAfVnTmA95iienk1vmLtQIgO+pvHu3Sjt2y3jBEZfJlirqufANall9\n" +
                    "3Oxu5fsi4lJJvW+/SPmVK2UrcdXbxviBipu3qrwnhQIfY2Qr0UylsFf/9z31zxFQPwQAYC6MFE9P\n" +
                    "z6/r/oivYRRNTmny/gerO6KPY/nd3Vr6kQ/La2mZ17LFxaLazzhNA79zseLy7Ibfje9p4t77k/Kg\n" +
                    "NHAJILAgCADAnJhk+N8uYABQUvAzftc9iqenqxppsKWSOs4+S0s/+hEZ35tTR0FcLKrl+NVa/qmP\n" +
                    "Jx38s5z8Vxkd1/j6e2bu4pcGjpSBhZCpOQBwUYP+uBtTkwIgE/iafnqTph55XO2vPFO2dOSh/bhY\n" +
                    "Vs9b3yKvo0N7L/2qSnv2ysuFR7xawVYqUhSr89xztOxjf6JwyeKZUwnVv0deLqfx2+9KJgCmMQ/B\n" +
                    "Wsn3ZA51HDTo5wXIIAIAMFe1GJo2RrZU0tA116r9zNOr/me2VFLXBeerZfWxOvC9KzV2x12qjIwm\n" +
                    "Vyx43vOjCXGclPV4nvKrjlbfu96hnre8SSYMD7sHQfXLGk8XNXT1taluG88PklsScyYASBUBAPXV\n" +
                    "qD/qNVxuE4aa+OV9mtjwgDrOfqVsqVTVv7PFknJLlmrZJz+m/vderIn7Nmjq8Y0q79mbnFLwPPld\n" +
                    "Xcofs0rtp5+m9jNOk9/dLVsqzez8Z8fL5TS87kZNPbZx5q6DKbBWJhcmZUuzaCIEcGQEAGAurFK5\n" +
                    "xK0qxsiWyxr89nfVdsoamVy+6vPyNoqkKFLuqOXqX/Ue9b3nXbLFouJSObnZUD5/6BJDW67IznHi\n" +
                    "nvF9lfcNav/lV6R+VYTJ59MLFAAOYRIg6sw07MPL5WvWTmfCUNOPbdSB7/9IXpib9bLaKFZcnDmy\n" +
                    "9wN5ra0y+RbJJiMFtlSeGbafw7YwnmQ87fvGN1XavjPdWyPbpIrY+MHM0X/93/d0H0D9MALwEmpZ\n" +
                    "RlGNtJYnrcKKNJYn1vyvV68La+W1tNQsAEiSyYU6cOWP1PKyE9V1/quTqxDmIuW5C14+rwNX/Vgj\n" +
                    "N94sk58pLLI2nYpka+V3dibPlWLDYVa81HexlsVo1cja72FaslYcV0uMAABzYa28tjaZMKjdderG\n" +
                    "KC6XtfuLX9b0k08lQ/d15rXkNXbHndr39W8ktxyOY5lcqGBgIJXtYmOroLt74W64BDiMbxXqyzbo\n" +
                    "I5a81jZ5YX4htsqLMr6vyv792vF/P6fic1uS+QB12QZGXr5F4+t/qV3/8M+Ki0XZmeVb/vGPq/30\n" +
                    "0xSn0EYoI/m9vcn/Ue/3fCEeQB0RAFBn9T4HO7eHtVZ+R4dMS161bqozYajSjh3a/unPaPrxJ5JT\n" +
                    "EbVcf+PJy+c1uu4X2vn3n1M0OSljjIznafnHP6b8Mas0dsed8lKYuGeMUdjfP7OzrP/7nv4DqB8C\n" +
                    "ADAXM3MAgq6u+d/ydg5MGKq0e5e2/82nNXLTOpkwnH/vfpWvK2s1+K1va+fn/1HR1ExDoedp2Uf/\n" +
                    "RD1vvUiD37lc8eSkUpkf4fsKBvplqQMGUsckQGAuZgKA39sjPRdLqtElgYcxQaDK2Jh2ff4fNPXY\n" +
                    "Y1r0gd9TODCguFRKfVTC+H5yJcKmTdr39cs0ce+9h8KAl8tp2Z9+VL3vfIdGf3Grxu5an85le9bK\n" +
                    "a8knIwBNOAEQqDcCAOqsgYdBg0DhkiXJxLcav7StVJKdsu9L1mr46ms0+cCD6v/t96n7ta+V39Fx\n" +
                    "6Da682GCQMb3Vdq7V8M/+5mGf3qtotFRmVxOtlxW0N2tZZ/8hLouuEDl/Qc0+O1vJzvrFAKAtVZB\n" +
                    "R6f87p6ZPNPAnxUggwgAqK8GHtk18pRbtrymr2mjSPkVK9Rxzjkauvrq54NAPq/S7t3a/YV/0fBP\n" +
                    "r1XPm9+sznPPVbh4cTI7P4qSUiDZF9/mRpLxZHxPxvdly2UVNm/R2K23anTdOpX37ElONeRysqWS\n" +
                    "citWaPmf/7naTz1VimId+OGPVHjqaZl8ShMj41hBb6/89nYpfonlBjAnBABgjmwcK79iRU3OvR9k\n" +
                    "fF+lXbska3XUX/yF9n71qyrt3i0vn0+Ww/dVePZZ7fnyl7X/e99T+9q1aj/rLLWccILCRYvkt7VJ\n" +
                    "vp/cE+DgOXprZa2VLZcVj4+rtHu3pjZu1MSGDZp+8klF4+PJSEA+mfAYF4vqOOssLfvYx5Q76ihZ\n" +
                    "azX58MMauuqqdBv74ljh0qUyufzMTYkApMmsWbvWyVydtVKdajTjMsde457bNUGgwubN2vKJTyQ7\n" +
                    "qFqVAlkrW6lo4Hd+Rz0XXaR9l12msdtvlzzvV8NIHB+6Y6HX3q5wYEDhkiXJUXV3t7yZHXo0Pa1o\n" +
                    "ZETlAwdU3rtXlaEhxYWCjOcld/SbWS9bqch4nnrf+d+0+EMflNfSImutorExbbvkEhWefTbVABAX\n" +
                    "i1r0gQ9oyR/8oeJiIbXnzRIvXvjwWMuyoCz9tkjNu+5pYQQAddbA53Vjq7CvX0F3j0r79tZuJMAY\n" +
                    "mSDQ/u99T5WRUS3/xCfV+apXad9/fVOlPbvl5XKHZuYfLAuypZKKO3aouG2bbBz/xiTBQ3cJ9DwZ\n" +
                    "L7nM7xBrFZdKyi1friUf/iN1v+Y1isbHpUok4/va+9WvqbBpU3pD/4ctU37FioP/lepzAyAAoM4a\n" +
                    "+mc9juV3dChcslilPbtrd3MgKQkBuZxGrr9OpV07ddSff0rHXXqp9l/+HQ3fdJOi8fHng8DBvz9z\n" +
                    "imA229yWSjK5nHovukiLP/T7Cnt7VXj6KYVLlspradG+b39Lo+tuTn3nL2tl8nnljjpKiqLG/pwA\n" +
                    "GUUPAOqr3k1s83x4YU65o1YkR9U1Yg8b2jf5vKYeeUSbP/Fxjd99t5b9yZ/quC98Ub1vfou8lpak\n" +
                    "oW+2l9BZm9wSOIrUfvrpWvWZv9PKS/5KdmpawzfeKL+nT0Ffv4avu06D3/lOcpog9ZW08ju7FA4s\n" +
                    "Tq5kyMB7TRMgmg0jAKizRj+2M8qvPLpmr2ajSC3HHSe/o1OTDz+UFPPlcqqMjGjXP/+Txm6/XYs/\n" +
                    "8D+04i/+UsWtWzVy4w0aW3+Xijt2HDqHf2gS4OHPa60URbJxLL+tTe2nn6G+d7xTXa8+T9HUpPZ9\n" +
                    "61uKxsfUf/H7FC5erJGbb9TuSy9Vajf9+fX1jGOFixYp6OrW83cpBJAmAgAwD9Za5VesXJij4Bdg\n" +
                    "PE/lffvUefY56jr/fA1fe62mn3tWJkiKeibuu1eTjz6izrPPVt/b3qHFH/x9LfrA/9TUxsc1cf8G\n" +
                    "TT/xhEp7diuamEhGEayVCQIFnZ3KrVihjtPOUOe5r1bLsceqPDSkwR98XyM33ajOc16lxR/8fXlt\n" +
                    "bRq+4Xrt/tcvKi4VF2zeg40i5VeulMnnZUulBXkNwHUEAGA+4kjhksXyWltli8WFvxLAGMWTkxr8\n" +
                    "7uXqOu98LfvTj6mwdbOGrrlaxS2bk9evVDR2++0aX79e+WOPVecrzlb7WWdp4OL3yWttVVwoKJoY\n" +
                    "Vzw9PXOuvUV+Z6e8XE6VsVFNP/mkBq+4XBP33SuTy2nZH39U3a97vWy5rMHvXq593/ovKaos+KTH\n" +
                    "/KpVSYfBgr4K4C4CADAPNrIKevsUdHertGdPba4EmJmpP3bnHZra+LgGfvv9WvV3n9X0k09o6Lqf\n" +
                    "aWrj48lliXGswjPPaPrpp2Wu/L6C7m6FixYpHFiUXAaYy8vaWPHUlCrDwyoPDqo8uE+V4WF5Yaie\n" +
                    "C9+kxR/6A+WPXqXS7l3a+5//oZGbb0pOI3gLu57G95VfdUxN51YAriEAoL5sg5/btVZeW7vCgUVJ\n" +
                    "QU8tS4FyOUVjY9rz71/RyI03qP8979PKS/5a5f2DGrvjdo3fe4+KO7bLTk/LlsuqHDigyoEDmrJP\n" +
                    "/MZlgLKxFFt5bW3qevV56r/4t9V5zqtkKxWN3HSj9n37v1TYsllerga3P7ZWfnu7ckuXS5W4lV/K\n" +
                    "3AAAH35JREFU8T8jQEZlKgDUspChlgURaRVWVCOt9arV85yS0jauJxOECpcuk33owdpPVZu51r+w\n" +
                    "ZYt2/uPnlF+1St2vf6N63nihBn77/Srt2a2pxx/T1BMbVdq2VeWhIcXT07KVpFnP+L681laFixar\n" +
                    "7ZRT1X3+a9R2yimyUaTxX96joat/rPEN90pxXJudv5IJgEFfv8K+vqYfAXip70fWim7SUsvfw7Rk\n" +
                    "bXnSkqkAADQi43nKLV+uel7XdXASYnHHDu297Ova/4PvqfWEE9Vx1ivVdsqp6jrvfHm5vOJSSfH0\n" +
                    "tOJCQZKVl8vLa2+T394hG0Uq7dqpwe9/V+Pr12v66Sdly2WZMCcFNbxiOIqUW7ZMXlubbKVxmyKB\n" +
                    "rCMAoM4af3jXxla5pctlTP1rNQ7eIdCWSpp8+CFNPPiAvDCU35Wc/w/6k+ZCr7VVMka2XFY0Nqby\n" +
                    "8JAqg/sOjRAkNwQKDjUJ1pK1VrkVR8v4oWyluUcAgHoiAADzZa3CxUuSnaW1tbsnwEsxJrlz38x/\n" +
                    "RmOjqowMS0/FyTX/h0YrzPM3BnqhGuB6MFJ+xUo1QzgEsowAgPpqhmu8olhBT6+8tjbFExPZCAC/\n" +
                    "bmbnLmV/t+oFoXJLlycNhs3w+QAyqv5jlkCDs3FSW+t3ds0cXWPOrJXX2qqgv19q8gmAQL0xAoA6\n" +
                    "y/rxaBWsldfSqrC3T6Ud22t7U6AmY+NYfkengq4e2ZgKYGAhMQIApMAEoYK+flnLUeu8WCu/u0de\n" +
                    "S0u9lwRoegQAIAXGMwoHBjhnPV9xLL+z8/kJlQAWTMOdAqhl0U1amrVEIo33wsZNMsRrPQW9AyIB\n" +
                    "zJeV394hyaMBsIZqWShUy9/DrJUgZQ0jAEAKrE3uCWAW4Na4LrFW8vIt2bySAmgy/FoBabCx/O4e\n" +
                    "JgCmgRAF1ATfNCAN1srv6JTnh5y7nicbUf8L1ELDzQFAs2mSoV4r+W3tMvm87PRUvZemYRljkvsU\n" +
                    "WKlpPhtARhEAUF9NcrBsYyuTb5EJQ2kqI3XAjchI8eSEFMVN89kAsopTAEAa7Myd9fJ52gDnw3iK\n" +
                    "xsdkyyVCFLDAGAFAXTXTT7zn+zK5Ot9Ip9F5RpWxUcXTBfkdnWIYAFg4BADUWbNEACP5gTwKbObF\n" +
                    "GE/RxLiisdGkDjhiWwILpSkDQC1LLRqxaKKWZUpH+jun/uD6Iz5HozCeJxME4qh1HoxRPD2t0r69\n" +
                    "yh99rOTwBQG1/B1LS9aWBy+NOQCoL9tEDxnJ89n/z5OtlFXcvlXGePV/Txf8MwPUT1OOAKCRNMkp\n" +
                    "gEO1tU2yPvVkjArPbZo5lcL2BBYKIwBAGowk2WSnxT5rXoznq7D5WUVTU1wJACwgAgCQFmul2OGT\n" +
                    "1ikxvq/S3l0q790l41GtDCwUAgCQEhvFikslMQQwT8YompzQ1FMbZyZVAlgIfLtQX01zy1cjW4kU\n" +
                    "FwsMW6dk4sEN6n3TO5voMwJkCyMAQBqMkS0WFRcKMgSAefOCQFNPPqrKgcHkygoAqSMAACkwxiia\n" +
                    "mpBlBCAdnq/y/n2aePRBeZwGABZEpr5Z1RRfVCOtMoq0lictjVhedMTXKjfJztL4ikZHFRemCQAp\n" +
                    "sVYau+sX6nnNm9Ss8yqyUpyTVvFXWq9VjawtTy23YVoYAQBSYDxPpf37ZCvlei9K9sVxVXXJXhhq\n" +
                    "4tEHVdi2WcbP1LEK0BQIAKivejexpdgCWNq5TTaOU95AzcVGkVqOWS2/q/vIIcAYReOjGrn150kA\n" +
                    "qPd7TBMgmgwBAEiBjSIVtj7HBMAjsbHaT3tF1Uf0Xhhq9LabVB7cw2RAIGUEANSZafzHzB3sCts2\n" +
                    "SwxVvzhr5bW2KX/06mSuRDW8pBRo+Obr5IU51f29Tv0B1A8BAJgn4wcq7tym8uAeGY+v1IuxNlbQ\n" +
                    "26+wf9Gs5kqYINTQz3+s0t5dMj6jAEBa+LUC5sn4viYfuZ8rAI4kipVbvEx+W3syEbBKxvdV2rdH\n" +
                    "+39yBZMBgRTxbUJ9NXrLm5Hi6WmNb7iLo9MjsDZWuGipvFzrrIOSl8tr+KZr1H3eG9S25jTZUlEM\n" +
                    "oQPzwwgAMA8myGnq6Y2afvYpeuuPxFqFvQPyWlpnP6HPGMWFae355qWKp6YkQ9gC5itTv1i1LMWo\n" +
                    "ppAhrWKHWhZfZK1kwwXDN12tuFSUl8vXe1Eyz2trlwlDmSCYOYqvnglzmnz8IQ3+8Jta+sE/Vlyc\n" +
                    "3b9vRrX8nmaltOigrJUXNeJvJiMAwByZMKeppx7V2D23zcxQx0sz8nI5mTAvL9ciW0UZ0K/zcjnt\n" +
                    "/8l3NXrXLfLyLQuwjIA7CACos3pfhjX3S/9spaJ937tMEZP/qmRlo1hea5v89g7JzqE0yRjZONKu\n" +
                    "f/8HTT29USZ3MARk4DPBZYBoMAQA1JWxjfnwcy0auu6HmnhgPUf/sxAXpuWHefmdXVXVAb8Q4/uq\n" +
                    "jBzQji98WqUdW+WF+bp/Hub6AOqJAADMkpdv0fiGO7X3u1/lsrRZisZGZYJQYf/iedUmmzCn4o7N\n" +
                    "2vb5S1TatV2G+RfArBEAgFnw8i2aeHSDtn/pM4qLBYnin6oZ46m0d6dkrXJLjprzCMCh5wvzKmze\n" +
                    "pC2f/XMVnnuKOQHALPHrhTqr9znY6h7GC+TlWjRyx03a+rlLVBkZ5uh/tnxPxV3bFU1NKL/8aKUx\n" +
                    "b8Lkcipu36zNf/cJjf7yNnn5ViWXCNb/M1PdA6gfAgDwUjxPXr5FlYlR7frPf9b2L3xa0cQY1/zP\n" +
                    "gfF8lfbtUmH7ZuWPXp0csc9zFEBKTgdURg5o2z/8pfZc/m+yUVmGeRnAEREAgF9nPJkwJy/fomhi\n" +
                    "TPt/9gM995f/S4NXXy7Jpt74Z+NItlJJ9TkzyRjF01Ma33CX2lafpPaT1s7qngAv+dR+IMWx9l7x\n" +
                    "NW35249pcuODMrk8ozTAS2i4b0cty3DS0ojLnJYjrfva79xSoyV5AcbIGE/y/UM38bGViirjIyps\n" +
                    "2aTx++/S2H13qLh7u4zvv3TRTxzPbj5AHCuulGX8QPnlRyvo6tHUpsfnuULZZ/xAY7+8TYvf8yEN\n" +
                    "vO39mnj8gRSf3MjLt2hy44Pa8rd/pp7XvFUDb3+/Wo4+TjaOZ0JWtqbe16I8plkL1tLSiAU+aWm4\n" +
                    "AIBmU8PzoMbI+H5yVGitosK0yiP7VR7crdKe7Srs3Kri9s0q7tis8v69isslmSB88R2/tYfOY/ud\n" +
                    "XYomJ1769W2yE7LWKte/RO1rX6HW405WNDas8Qfv/pXna1YmCFTY8rQmHtmgzldeoPaXrU2O1lMc\n" +
                    "sjdhTjaKdOCGH2r07nXqfvUb1fv6t6v1uJOSAqJKRTaOUjn9ADQyAgCamzEyQSjjeYqmJlTY+qym\n" +
                    "n3lcU5seV2HrMyrv36NoYlxxuSRZm4wE+L6M5x9xVnmyo6lIcaz+i96nA9ddqWhy/Fd34tbKVsrJ\n" +
                    "rXC7etX+8jPVdfZrFPYOaPKJhzR041Uq7twyE07c+DrG5bKGb/mpus6+QP1vea8mn3go/ReZGQ2I\n" +
                    "pyZ14PorNXzrz9R24qnqPue16jj1FcotXZHck8Da5wMB4Bg3fnHgHs+TF+YUTU9q6omHNbbhDk08\n" +
                    "tkGlXdsUTU8lO3s/SHb4njfrHn9bKavj1FcqmhzT1KaN6jj9XE1t2qjxDXfIBKFsVJGNIvntnWpf\n" +
                    "c7q6z7tQ7aecpWhsVCO3X689d69TeWh/Ek6CcIE2QjZ5YajxB9ZratPj6nrVa9V63Emafu7JhdkO\n" +
                    "M5M4FUWafPQ+TTzySwUd3cqvPFZtJ5yitpNPU9vxaxQOLJXxvENBEHABAQDNxRh5YV6V0SEN3flz\n" +
                    "Dd96nQqbn1ZcKiY7/COdy6+SjZNb23acdrYKW55RbmCpus9+jUbvuklBvkWtx69R19mvUdc5r1XY\n" +
                    "26+Jxx/Q7su+oIlHNyguTMnL5dy9bt14qoyPaujnP9KKP/v/1Hfhu7Xj3z67sCeDjJEJczKS4mJB\n" +
                    "U089qsmND8lce4WCnn61vexU9Zx3oTrOOFdBR7ficpEggKZHAEB92fR+9o3vy1qroZt/osEff0uF\n" +
                    "7c8l5/yDIP2drbUyQaiuV75Ww7ddLxPk1HHaq7TsQx9X55nnqXX1GlVGhzV65w0auuUaFbZsOvRv\n" +
                    "nN3xH8YLQ43ceaP6L/oddZ93oQav/o5Ke3emfoXFCzp4WmhmxCEaG9Ho3bdo7J5fqOWYEzTwzg+o\n" +
                    "57fekkxPmUdbIZB1BAA0BeMHqoyPatfXP6/RO2+UfG9BdvrJiyWhxcu3KL/8aPWc/2YZ31fQt0hL\n" +
                    "/8efqbhrq3Z/8180cscNKu/fm4w8ODbMf0Sep8rosPb/7Hta+bHPqOeCi7T3in+rTQB4gWU5OCpU\n" +
                    "2PqMdnzx05p8dIOWf/hTMmF+bjctAhoAPQBoeMkEv3Ft/8JfauT262VyufQn1Fkrv6NTMubQtete\n" +
                    "rkUyRr2vfXtyROkHGr75J3r2kj/Q4I//S5XRYXn5FkqDXoSXy2v0jp9r6qlH1Pu6tyvs6a/7EbcJ\n" +
                    "QplcqKGbfqy9P/iP+gQSoEYIAKizFGp6/VD7fniZxh9cn8zsXgA2qqhl1Yk6+hP/V62r18iWS8lR\n" +
                    "o5X8zm6ZXF6DV39b2774Nyof2Csv33qoWwAvwhhVJse074eXKb98lTpfeYGiwpTiUlFxsaC4VEyu\n" +
                    "oKj5JXtGJpfX6PqbFY2PSsYTVcBoRmbN2rWZmemStYKItGSt5CdLxURmqnt+/97zVR7ap2f+4oOK\n" +
                    "xkYW7uY81sqEOR3/+W8rt2iZDtxwpXJLV6rr7AtkjKcDN1ypnf/xORljuEHQbMzs2I/5P/8qr7VN\n" +
                    "uy/7R+WXHS0FgeKpSZWHB1U5MKjK2LDiwpRsHM9cqhnIeAu3E7WVslpWHqfVn/1GcqfBBQogtm30\n" +
                    "Rf8sa6U6af0m1HJ5srYNs4axSTQ2z1NpcPfC7vwlyRhFk+Mav/9OLXr3hzTwzg8kpwKsNPnkQ9r9\n" +
                    "7S8l+yJ2/rNjjGy5pL1X/JuO/ZtLddxnviYvzEtGM+19ZUWTEyoP7VNx51YVNj+p6c1Pqbhziyoj\n" +
                    "BxSXCs8XPHl+KkVKtlKRPE+L3v0heW0dsuVSCisKZA8BAPU13wOr2MrLt8kEgWwULWiTnvE8TTy2\n" +
                    "QQNv+92kAMgY2WJRey7/sqKpCXncgGZOTJjT5JMPafiWazTw9t9TXJw+/E/ld3Qp6OpR23EnSxdc\n" +
                    "JFsuqTI2rOKubZre/KSmn9mowrZnVBrcrXhqIhklMOb5imdTxUhBHCfdDXGs/FGrtPS//4m6X32h\n" +
                    "bImdP5oXAQANzcaRWo46Vq2r12ji8ftnf42/tbI2TiafWSv7a5PQkssIw0P/d2HrM6qMDsvv6pYX\n" +
                    "5DS8/lpNPv4AO/95Mr6vAzf8QD3nv0Vee8dhkwGtFCfvi9XBGyYZ+V19au9ZpI5Tz5aNY8VTEyrv\n" +
                    "36PCjs0qbHlKhW3PqrR3h8ojBxRPT8iWS7/x3s48lYwfyu/sVuvRx6vrnNep+9wLFfb2K2bnjyZH\n" +
                    "AECdzfOI3Uoml9fS3/uotvz9J1UZHUoKX8zzf25ndiKy8aGdgDFGJgzl5Vvld3TJ7+pV2DMgv7tX\n" +
                    "QUe3vHyrbFzRxKP3aeqph5MQ4PmqDO9TYcdmdZz6SsXFgoZuvGp+yw9JyWWchR2bNfSLa7T43b+v\n" +
                    "uFh46X8Qx4dOEUjJZyC/4ji1rDpBOv/NslGkeHpSlbERlYf3qXxgn6LRIUUTY8lpA0levk1BT59y\n" +
                    "i49SbtnRCvuXyMvlFJdLh+38maiH5kUAQMOzlbLaTjpDx1zyL9p35ddU2LpJcXlmx+Annf7JMHKf\n" +
                    "gv7Fyg0sVbhomXIDSxX0DiQBoLVdJpi5fPDgb74xiqcmte2Lf6WxX66TCfOKSyVNP7dRXWedr4kn\n" +
                    "H9TUM49zmV9KTBBo/7XfVfc5r1du8VHJaZZqWTtTv/z8vzG5vHKLlim35KjDTgW8wD+NYymKZOPo\n" +
                    "yMEDaCL8cqEp2HJJbSeu1TGXfFGV0SFFhWlJVl6YSy7Jy+Xl5VqS67qNmRn6t1IcJf/7BXYgkmTy\n" +
                    "LVryOx/R5KP3KS4VZIzR9LMbJUnjD96tuDhNs19KjOervH+P9v3oP7Xijz89/ye0VtZGyXs8/2cD\n" +
                    "mg5TltE0kmvGrfyuPuWXrFR+ydEK+5bIa+uU8cNkWPjwa8zLpWTi4Mz5/xd8znJJLStWq+3EU5Ph\n" +
                    "Zt9XYdtzqgwf0NSTD8n4fIXS5OXyGrn9Oo3ff0dyNQB7bmDB8OuF+rIm5YeSSWNRNPOIZ87/a+Yx\n" +
                    "++c0Qai2l502cw26r8rwoKaffULFvTtkPAbRUmWMbFTR3h98VdHkhGT8BfiMZOgB1FGmfr2yVliR\n" +
                    "FpfLKI60zGu/eUeNlmQerFV+5epDpw/i4pTGH7lH8fSk5NXjR9w+f97axs8fJRsj43kz8xgad+di\n" +
                    "glBTmx7T0M1XadG7PsR5+ZfQiKU6Wft9Tksj/oZnKgAAWWSjSLlFy+Xl8skpA0mj62+cmSleox3t\n" +
                    "Ydepe/m8wr4lyi9dodySFQq6+2XjSOUD+1TctVmlPdtVGU8a5rwwnKmybSwmCLX/uu+q+9VvUti7\n" +
                    "KKkDBpAqAgDqqhGOU421Crt65bV1HGocLO3fnTTPLSAbR7KVsozxFPQMqHX1yeo4+Sy1nbhW+eWr\n" +
                    "5HcmXQQHN6K1VrYwrdL+3Zp88iGNPXCHpp54UOWRA7/SZ9AIjO+rtG+nhtddpaXv/6jiUnMGAKY4\n" +
                    "oJ4IAKiz7EcAa6389i4F7d1Jz4C8Bdv5J/MWyjJ+oNyi5Wpfc5Y6T3+12k5cq1z/0kONhzZOJi/G\n" +
                    "peKvPoEfKL/sGLWsPF59r3+3Snu2a3TDbRpd/3NNPfeEbKWclBY1wCkCE+Q0fNu16n/Tbyvo6n3h\n" +
                    "Ih8Ac0YAQH01xCGQlcm1KOjtl3Y8K6W87z90pO8Hyi0+Sh2nnqOuM39LbSesVdDTLyMdukTRVo5w\n" +
                    "bby1svHM3zNSbvFRWvzOD6r/wvdp8on7NfSLqzX+0F2KJsZkwnDBRzHmw/i+int3auz+O9R/4Xtl\n" +
                    "mQsApIoAAByJTc6lh31LZG2cypjFoZ2+5yu3aHmy03/Fa9R+4mkKuvtk7cxljaXi3DOS1aGrIYzv\n" +
                    "q/P089R5+qtV2LpJw7f9VCPrf67S4G6ZIEgmDmaRkcY23Kq+172r3ksCNJ2MfuuBjDFG+eWr5jVi\n" +
                    "kez0KzLGKBxYpo6Xv0Jdr3yt2k86Q2HPgGSluFL+zWH9NFh76K52LUcfr+Uf+pQG3vZ7GrnrBg3f\n" +
                    "eo2mtz8rY5Jh9ywxfqDprZtUGRuS39lz2D0CAMwXAQB1lv1z0ZIka9Wy8oTkUsDZ/LPDJvKFA0vV\n" +
                    "ftKZ6n7la9V+0lkK+xYllxVWyr9245mF3Sa2EslWIoU9i7T4XX+ovjdcrLENt2po3VWafPph2Uol\n" +
                    "M/MEjPEUTYyqMjKkoKs/ua8DgFQQAIAq2EqkllUvU9Ddp2h8VPJe5NI6e7CEqCITzEzkO+kMdZ7x\n" +
                    "W+o4+UwFfYtljJGtlBXX+T7zNo5liwV5+Tb1vfZd6jn3zZp4/F4dWHeVJh6+W9HURHJjpRdb11qZ\n" +
                    "uQQyC4EEaCYNFwDSKpGoZTlPLQsiavlaqaz7eCqLsuBsHCm3eJk61p6roVuukt/Slvz/rZ25M10k\n" +
                    "ychv61DL0Uer/aQz1bH2XLUd/3IF3QPJ7Qcq5aR+uL6r8ptsnNwhz/PUdcZvqfP08zS9+UkN33qN\n" +
                    "Rn55s8oH9sh4dbqM0Fp5re0Kuvq4CuAFpPV9z9rvWNY067o3XAAA6ia2WvLuD6u0d4eKuzZLMvJb\n" +
                    "2xX2LVZ+xWq1HX+K2o57uXLLVspv7ZC1sWylIluex0S+WrL20KhE6zEnq+0PT9Gid3xQo/eu0/Ad\n" +
                    "12l6y5OylVJy18QajQrE5aK6Tj5TYf+S2d0dEMAREQBQXw2xZ0zYSkX5ZcfouL/6qiqjByRJXkur\n" +
                    "/LZOebnkjoBJW1/j31bWVsqylbLC3sVa9PYPqu/1F2vyqQc1ctf1Gn94vcpD+2SMSW6FvEBNg7Zc\n" +
                    "Uti3REve80eSTEN9VoBGQABAnTXWeV0bRTJBqNzA8uS/ZaXYvsDM/cZarxdzcJ6ACUJ1nnaeOk87\n" +
                    "X+XBXRp/ZL1G77tFU5seUWVsWJKSywlT6BWwUSRbKallxfFa8b/+Rq2rTlZcLqpZtimQFQQAYLYO\n" +
                    "3mfeJYddRhj2LVb/G9+nvte9S8W9OzSx8T5NPHK3pp99XOWhvckVDcYk9cPGSybvvdAEPpvcotHG\n" +
                    "VoqTdkPjh8ovWaGe8y5S/5ver6B3YGbnDyBtBAAAs2LjSHammz+/ZIValh+r/tdfrMrYkArbn9X0\n" +
                    "s49pevMTKu7eqvLIfsXTE4rLZSmqJDt9YyTfl/FDefkWBR3dChctV+sxJ6n95DPVdsJpCrv7D02a\n" +
                    "BLAwCAAA5uxg06Ak+R3d6jjlbHWufZVsHCsuTCXX8I+PKBofUTQ9KcWR5Pny8q3y2zsVdHTL7+iW\n" +
                    "39YpE4bJKYdKJbkqAcCCIgCgzjiv2zRiKxuXD83VM0GosHexwr6lkufJHHYawFor2Tj5NzaemWtw\n" +
                    "+FA/nwtgoREAUF/M7G5eVrKKJcUH/xNAhpg1a9dm5ntZy5KfrMlawVFajrQ8p112d42WBMieh//g\n" +
                    "3AV/jVp+36tRy9KztDTiPqUade74BAAA9UAAAADAQcwBQJ0x2QsA6oERAAAAHEQAAADAQZwCQH1l\n" +
                    "5hoUAHALIwAAADiIEQDUGZMAAaAeMhUAalm2kLVyjLRUs16Z2s4jtVkOoNE0YtFNWsvcrL/PWcMp\n" +
                    "AAAAHEQAAADAQQQAAAAcRAAAAMBBmZoECAdZrgIAgHogAKCu2P0DQH1wCgAAAAcRAAAAcFCmTgE0\n" +
                    "YvlDLUt1aimt9+JI2+f0r9+byusALmrE35+sFRzVsrwoa+8XIwAAADgoUyMAcBHTAAGgHhgBAADA\n" +
                    "QYwAoK4e+vArq/p7tZqT0MyvVY1GXPdqNOLyAAuNEQAAABxEAAAAwEEEAAAAHEQAAADAQZmaBJi1\n" +
                    "iUxpydrypCVrE5mytjxpSWsCW7Nun1rK2mTCrE0iTeu1+DzXBiMAAAA4iAAAAICDCAAAADiIAAAA\n" +
                    "gIMIAAAAOIgAAACAgwgAAAA4iAAAAICDMlUE1IjlD2ktcy3XvZbLnJasLU8jylppTNa+F7Vcnqzd\n" +
                    "dbERvzuN+HnOGkYAAABwEAEAAAAHEQAAAHAQAQAAAAcRAAAAcBABAAAABxEAAABwEAEAAAAHmTVr\n" +
                    "19p6L8RsZK0sKGtlFFkr9GjEUpS0XqsRNeL2acRCqqxtn0b8PDdiKVPWMAIAAICDCAAAADiIAAAA\n" +
                    "gIMIAAAAOIgAAACAgwgAAAA4iAAAAICDCAAAADioKYuA0pK1YodaFno0YslGLZe5luteS1krqKmG\n" +
                    "y+9p1t6vRvzNTEvW1r0ajAAAAOAgAgAAAA4iAAAA4CACAAAADiIAAADgIAIAAAAOIgAAAOAgAgAA\n" +
                    "AA4K6r0Ah2vW0oZaFp5UoxELPWrJ5WKZrH1W09KIpVVpyVqBWC1l7Xc+a98LRgAAAHAQAQAAAAcR\n" +
                    "AAAAcBABAAAABxEAAABwEAEAAAAHEQAAAHAQAQAAAAeZNWvX2novBKqTtaKkRiwYydrypCVrhSfV\n" +
                    "yFphTjWyVuTi8vbJ2ucwa5+NajACAACAgwgAAAA4iAAAAICDCAAAADiIAAAAgIMIAAAAOIgAAACA\n" +
                    "gwgAAAA4KKj3Ahwua6UWtZRWiUTWim5qWaCRtXXP2vI0a5lJI27najTr9yJrn8OsLU8tMQIAAICD\n" +
                    "CAAAADiIAAAAgIMIAAAAOIgAAACAgwgAAAA4iAAAAICDCAAAADgoU0VA1WjEQgaXC46qkbUClrRk\n" +
                    "rWAka6+V1vbJ2nbOmqx9Lxrxc9isGAEAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAA\n" +
                    "AMBBBAAAABzUcEVA1ahlsUMjFozUsjglayUtjbg8tXyeRpS172Ba5TON+FnN2ucwa9+vrH1WGQEA\n" +
                    "AMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABzUlEVAzSqtEom0ikrSklbh\n" +
                    "SSOWfqS1XlkrGKlG1ta9EZenGmktcy23TyN+5rNWglQNRgAAAHAQAQAAAAcRAAAAcBABAAAABxEA\n" +
                    "AABwEAEAAAAHEQAAAHAQAQAAAAdRBNRkallGUcuikrQ06zI3YgFLI5ZN4aU1a8lYNRrx88MIAAAA\n" +
                    "DiIAAADgIAIAAAAOIgAAAOAgAgAAAA4iAAAA4CACAAAADiIAAADgILNm7Vpb74U4qFnLOrK2Xllb\n" +
                    "nrTUsmCkGrUszMlaAUstNeK6N2IpU7P+RqX12WjE30xGAAAAcBABAAAABxEAAABwEAEAAAAHEQAA\n" +
                    "AHAQAQAAAAcRAAAAcBABAAAABwX1XoDZylqhR9bUcvtkrUCjEQthGlEtS2yqeZ5avlbWymdqKWvf\n" +
                    "96zJWplSNRgBAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcZNasXWvr\n" +
                    "vRAAAKC2GAEAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEEE\n" +
                    "AAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAA\n" +
                    "ABxEAAAAwEEEAAAAHEQAAADAQQQAAAAcRAAAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQQQAAAAc\n" +
                    "RAAAAMBBBAAAABxEAAAAwEEEAAAAHEQAAADAQf8/4/hOoUm2ZMoAAAAASUVORK5CYII=}";
        } else {
            return "{}";
        }
    }
}