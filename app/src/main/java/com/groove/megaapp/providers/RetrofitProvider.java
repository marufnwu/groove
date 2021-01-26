package com.groove.megaapp.providers;

import android.content.Context;
import android.text.TextUtils;

import androidx.multidex.BuildConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixplicity.easyprefs.library.Prefs;
import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import java.util.concurrent.TimeUnit;

import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.data.api.REST;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitProvider implements Provider {

    private final Context mContext;

    public RetrofitProvider(Context context) {
        mContext = context;
    }

    @Override
    public void provide(MutableContainer container) {
        container.factory(OkHttpClient.Builder.class, c -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(SharedConstants.TIMEOUT_CONNECT, TimeUnit.MILLISECONDS)
                    .readTimeout(SharedConstants.TIMEOUT_READ, TimeUnit.MILLISECONDS)
                    .writeTimeout(SharedConstants.TIMEOUT_WRITE, TimeUnit.MILLISECONDS);
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(interceptor);
            }

            return builder;
        });
        container.factory(Retrofit.Builder.class, c -> {
            ObjectMapper om = c.get(ObjectMapper.class);
            return new Retrofit.Builder()
                    .baseUrl(mContext.getString(R.string.server_url))
                    .addConverterFactory(JacksonConverterFactory.create(om));
        });
        container.factory(Retrofit.class, c -> {
            OkHttpClient client = c.get(OkHttpClient.Builder.class)
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        String token =
                                Prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
                        if (!TextUtils.isEmpty(token)) {
                            request = request.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    .build();
            return c.get(Retrofit.Builder.class)
                    .client(client)
                    .build();
        });
        container.singleton(REST.class, c -> c.get(Retrofit.class).create(REST.class));
    }
}
