/*
Open Markdown Notes (android application to take and organize everyday notes)

Copyright (c) 2017 Mikhail Basov (https://github.com/mvbasov/OMN)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.basov.omn;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URISyntaxException;

import net.basov.util.MyLog;
import net.basov.util.FileIO;

/**
 * Redirect external URLs to browser
 */

public class MyWebViewClient extends WebViewClient {

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final Uri uri = Uri.parse(url);
        return processUri(uri, view);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        return processUri(uri, view);
    }

    private boolean processUri(final Uri uri, WebView view) {
        Context c = view.getContext();
        switch(uri.getScheme()){
            case "file":
                String pageName = uri.toString()
                        .replace("file://","")
                        .replace("//","/")
                        .replace(FileIO.getFilesDir(c).getPath(),"")
                        .replace("/html/","")
                        .replace("/android_asset","") //TODO: Why it is need?
                        .replace(".html","")
						.replace(".md","");
                        
                /* For relative uri add / to begin because relative already translated to absolute */        
                if(pageName.charAt(0) != '/') pageName = "/" + pageName;
                
                Intent intentDisplayPage = new Intent(view.getContext().getApplicationContext(),
                        MainActivity.class);
                intentDisplayPage.putExtra("page_name", pageName);
                intentDisplayPage.setAction(Intent.ACTION_MAIN);
                view.getContext().startActivity(intentDisplayPage);

                return false;
            case "http":
            case "https":
            case "geo":
            case "tel":
			case "mailto":
			case "sms":
			case "market":
                final Intent intentMarket = new Intent(Intent.ACTION_VIEW, uri);
                c.startActivity(intentMarket);
                return true;
            case "intent":
                try {
                    // TODO: remove double translation string - uri - string
                    final Intent intentApp = new Intent().parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    if (intentApp != null) {
                        view.stopLoading();
                        PackageManager packageManager = c.getPackageManager();
                        ResolveInfo info = packageManager.resolveActivity(intentApp, PackageManager.MATCH_DEFAULT_ONLY);
                        if (info != null) {             
                            StrictMode.VmPolicy old = StrictMode.getVmPolicy();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(old)
                                                       .detectFileUriExposure()
                                                       //.penaltyLog()
                                                       .build());
                                c.startActivity(intentApp);
                                StrictMode.setVmPolicy(old);
                            } else {
                                c.startActivity(intentApp);
                            }
                        } else {
                            String fallbackUrl = intentApp.getStringExtra("browser_fallback_url");
                            view.loadUrl(fallbackUrl);
                        }
                        return true;
                    }
                } catch (URISyntaxException e) {
                    MyLog.LogE(e, "href processing error");
                    Log.e(Constants.TAG, e.getMessage());
                    e.printStackTrace();
                }
            default:
                return true;
                
         }              
    }
}
