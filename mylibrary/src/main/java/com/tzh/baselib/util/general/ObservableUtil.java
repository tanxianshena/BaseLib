package com.tzh.baselib.util.general;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

//定时器工具类
public class ObservableUtil {
    /**
     * 定时器数组
     */
    private static final Map<String,Disposable> disposableMap = new HashMap<>();
    /**
     * 回调函数数组
     */
    private static final Map<String,ObservableLister> listerMap = new HashMap<>();
    /**
     * 开始定时执行
     * @param time 间隔
     * @param key 定时器key
     * @param lister 回调监听
     */
    public static synchronized void startTimer(long time,String key,ObservableLister lister){
        if (time <= 0) throw new IllegalArgumentException("time must be positive");
        stopTimer(key);
        listerMap.put(key, lister);
        disposableMap.put(key, getDisposable(time, key));
    }

    //创建一个定时器
    private static Disposable getDisposable(long time,String key){
        // 每隔time毫秒执行一次逻辑代码
        return Observable.interval(time, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {

                        synchronized (ObservableUtil.class) {
                            ObservableLister listener = listerMap.get(key);
                            if (listener != null) listener.ok();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    /**
     * 停止定时执行
     */
    public static synchronized void stopTimer(String key) {
        Disposable disposable = disposableMap.remove(key);
        listerMap.remove(key);
        if (disposable != null) disposable.dispose();
    }

    public interface ObservableLister{
        void ok();
    }
}
