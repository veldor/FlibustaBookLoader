package net.veldor.flibustaloader.http;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

public class GlobalWebClient {
    public static String request(String requestString) throws TorNotLoadedException, IOException {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if(App.getInstance().isExternalVpn()){
            HttpResponse response = ExternalVpnVewClient.rawRequest(requestString);
            if(response != null){
                return EntityUtils.toString(response.getEntity());
            }
        }
        else{
            TorWebClient webClient = new TorWebClient();
            return webClient.request(requestString);
        }
        return null;
    }
}
