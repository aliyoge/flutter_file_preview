package com.zzaning.flutter_file_preview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.ColorInt;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * android 4.4以上沉浸式以及bar的管理
 * Created by WenjunHuang on 2020/8/22 11:
 * Email: kongkonghwj@gmail.com
 */
public class StatusNavBar {

    private static final int IMMERSION_STATUS_BAR_VIEW = R.id.immersion_status_bar_view;
    private static final int IMMERSION_NAVIGATION_BAR_VIEW = R.id.immersion_navigation_bar_view;
    private static final String NAVIGATIONBAR_IS_MIN = "navigationbar_is_min";

    private static final int FLAG_FITS_DEFAULT = 0X00;
    private static final int FLAG_FITS_TITLE = 0X01;
    private static final int FLAG_FITS_TITLE_MARGIN_TOP = 0X02;
    private static final int FLAG_FITS_STATUS = 0X03;
    private static final int FLAG_FITS_SYSTEM_WINDOWS = 0X04;

    /**
     * 维护ImmersionBar的集合
     */
    private static Map<String, StatusNavBar> mImmersionBarMap = new HashMap<>();

    private Activity mActivity;
    private Dialog mDialog;
    private Window mWindow;
    private ViewGroup mDecorView;
    private ViewGroup mContentView;

    /**
     * 用户配置的bar参数
     */
    private BarParams mBarParams;
    /**
     * 系统bar相关信息
     */
    private BarConfig mBarConfig;
    /**
     * 沉浸式名字
     */
    private String mImmersionBarName;
    /**
     * 导航栏的高度，适配Emui系统有用
     */
    private int mNavigationBarHeight = 0;
    /**
     * 导航栏的宽度，适配Emui系统有用
     */
    private int mNavigationBarWidth = 0;
    /**
     * 是否是在Activity使用的沉浸式
     */
    private boolean mIsActivity = true;
    /**
     * Emui系统导航栏监听器
     */
    private ContentObserver mNavigationObserver = null;
    /**
     * 用户使用tag增加的bar参数的集合
     */
    private Map<String, BarParams> mTagMap = new HashMap<>();
    /**
     * 是否适配过布局与导航栏重叠了
     */
    private boolean mIsFitsLayoutOverlap = false;
    /**
     * 当前是以哪种方式适配的
     */
    private int mFitsStatusBarType = FLAG_FITS_DEFAULT;
    /**
     * 是否已经获取到当前导航栏颜色了
     */
    private boolean mHasNavigationBarColor = false;

    /**
     * 在Activit里初始化
     * Instantiates a new Immersion bar.
     *
     * @param activity the activity
     */
    private StatusNavBar(Activity activity) {
        mActivity = activity;
        mWindow = mActivity.getWindow();

        mImmersionBarName = mActivity.toString();

        mBarParams = new BarParams();

        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
    }

    /**
     * 在Fragment里初始化
     * Instantiates a new Immersion bar.
     *
     * @param fragment the fragment
     */
    private StatusNavBar(Fragment fragment) {
        this(fragment.getActivity(), fragment);
    }

    private StatusNavBar(Activity activity, Fragment fragment) {

        mActivity = activity;
        if (mActivity == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        if (mImmersionBarMap.get(mActivity.toString()) == null) {
            throw new IllegalArgumentException("必须先在宿主Activity初始化");
        }

        mIsActivity = false;
        mWindow = mActivity.getWindow();

        mImmersionBarName = activity.toString() + fragment.toString();

        mBarParams = new BarParams();

        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
    }


    /**
     * 在dialogFragment里使用
     * Instantiates a new Immersion bar.
     *
     * @param dialogFragment the dialog fragment
     */
    private StatusNavBar(DialogFragment dialogFragment) {
        this(dialogFragment, dialogFragment.getDialog());
    }

    private StatusNavBar(DialogFragment dialogFragment, Dialog dialog) {
        mActivity = dialogFragment.getActivity();
        mDialog = dialog;
        if (mActivity == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        if (mDialog == null) {
            throw new IllegalArgumentException("DialogFragment中的dialog不能为空");
        }
        if (mImmersionBarMap.get(mActivity.toString()) == null) {
            throw new IllegalArgumentException("必须先在宿主Activity初始化");
        }

        mWindow = mDialog.getWindow();

        mImmersionBarName = mActivity.toString() + dialogFragment.toString();

        mBarParams = new BarParams();

        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
    }

    /**
     * 在Dialog里初始化
     * Instantiates a new Immersion bar.
     *
     * @param activity the activity
     * @param dialog   the dialog
     */
    private StatusNavBar(Activity activity, Dialog dialog) {
        this(activity, dialog, "");
    }

    /**
     * 在Dialog里初始化
     * Instantiates a new Immersion bar.
     *
     * @param activity  the activity
     * @param dialog    the dialog
     * @param dialogTag the dialog tag  dialog标识，不能为空
     */
    private StatusNavBar(Activity activity, Dialog dialog, String dialogTag) {
        mActivity = activity;
        mDialog = dialog;
        if (mActivity == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        if (mDialog == null) {
            throw new IllegalArgumentException("dialog不能为空");
        }
        if (mImmersionBarMap.get(mActivity.toString()) == null) {
            throw new IllegalArgumentException("必须先在宿主Activity初始化");
        }

        mWindow = mDialog.getWindow();
        mImmersionBarName = activity.toString() + dialog.toString() + dialogTag;

        mBarParams = new BarParams();

        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
    }

    /**
     * 初始化Activity
     * With immersion bar.
     *
     * @param activity the activity
     * @return the immersion bar
     */
    public static StatusNavBar with(@NonNull Activity activity) {
        StatusNavBar statusNavBar = mImmersionBarMap.get(activity.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(activity);
            mImmersionBarMap.put(activity.toString(), statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * 调用该方法必须保证加载Fragment的Activity先初始化
     * With immersion bar.
     *
     * @param fragment the fragment
     * @return the immersion bar
     */
    public static StatusNavBar with(@NonNull Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        StatusNavBar statusNavBar = mImmersionBarMap.get(fragment.getActivity().toString() + fragment.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(fragment);
            mImmersionBarMap.put(fragment.getActivity().toString() + fragment.toString(), statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * With immersion bar.
     *
     * @param activity the activity
     * @param fragment the fragment
     * @return the immersion bar
     * @deprecated 请使用ImmersionBar with(@NonNull Fragment fragment)
     */
    public static StatusNavBar with(@NonNull Activity activity, @NonNull Fragment fragment) {
        StatusNavBar statusNavBar = mImmersionBarMap.get(activity.toString() + fragment.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(activity, fragment);
            mImmersionBarMap.put(activity.toString() + fragment.toString(), statusNavBar);
        }
        return statusNavBar;
    }


    /**
     * 在DialogFragment使用
     * With immersion bar.
     *
     * @param dialogFragment the dialog fragment
     * @return the immersion bar
     */
    public static StatusNavBar with(@NonNull DialogFragment dialogFragment) {
        if (dialogFragment.getActivity() == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        StatusNavBar statusNavBar = mImmersionBarMap.get(dialogFragment.getActivity().toString() + dialogFragment.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(dialogFragment);
            mImmersionBarMap.put(dialogFragment.getActivity().toString() + dialogFragment.toString(), statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * 在DialogFragment使用，已过时
     *
     * @param dialogFragment the dialog fragment
     * @param dialog         the dialog
     * @return the immersion bar
     * @deprecated 请使用ImmersionBar with(@NonNull DialogFragment dialogFragment)
     */
    @Deprecated
    public static StatusNavBar with(@NonNull DialogFragment dialogFragment, @NonNull Dialog dialog) {
        if (dialogFragment.getActivity() == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        StatusNavBar statusNavBar = mImmersionBarMap.get(dialogFragment.getActivity().toString() + dialogFragment.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(dialogFragment, dialog);
            mImmersionBarMap.put(dialogFragment.getActivity().toString() + dialogFragment.toString(), statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * 在dialog里使用
     * With immersion bar.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @return the immersion bar
     */
    public static StatusNavBar with(@NonNull Activity activity, @NonNull Dialog dialog) {
        StatusNavBar statusNavBar = mImmersionBarMap.get(activity.toString() + dialog.toString());
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(activity, dialog);
            mImmersionBarMap.put(activity.toString() + dialog.toString(), statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * 在dialog里使用，已过时
     * With immersion bar.
     *
     * @param activity  the activity
     * @param dialog    the dialog
     * @param dialogTag the dialog tag
     * @return the immersion bar
     * @deprecated 请使用ImmersionBar with(@NonNull Activity activity, @NonNull Dialog dialog)
     */
    @Deprecated
    public static StatusNavBar with(@NonNull Activity activity, @NonNull Dialog dialog, @NonNull String dialogTag) {
        StatusNavBar statusNavBar = mImmersionBarMap.get(activity.toString() + dialog.toString() + dialogTag);
        if (statusNavBar == null) {
            statusNavBar = new StatusNavBar(activity, dialog, dialogTag);
            mImmersionBarMap.put(activity.toString() + dialog.toString() + dialogTag, statusNavBar);
        }
        return statusNavBar;
    }

    /**
     * 透明状态栏，默认透明
     *
     * @return the immersion bar
     */
    public StatusNavBar transparentStatusBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        return this;
    }

    /**
     * 透明导航栏，默认黑色
     *
     * @return the immersion bar
     */
    public StatusNavBar transparentNavigationBar() {
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        mBarParams.fullScreen = true;
        return this;
    }

    /**
     * 透明状态栏和导航栏
     *
     * @return the immersion bar
     */
    public StatusNavBar transparentBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        mBarParams.fullScreen = true;
        return this;
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor 状态栏颜色，资源文件（R.color.xxx）
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(@ColorRes int statusBarColor) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor));
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor 状态栏颜色，资源文件（R.color.xxx）
     * @param alpha          the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(@ColorRes int statusBarColor,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor), alpha);
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor          状态栏颜色，资源文件（R.color.xxx）
     * @param statusBarColorTransform the status bar color transform 状态栏变换后的颜色
     * @param alpha                   the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(@ColorRes int statusBarColor,
                                       @ColorRes int statusBarColorTransform,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor),
            ContextCompat.getColor(mActivity, statusBarColorTransform),
            alpha);
    }

    /**
     * 状态栏颜色
     * Status bar color int immersion bar.
     *
     * @param statusBarColor the status bar color
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(String statusBarColor) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor));
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor 状态栏颜色
     * @param alpha          the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(String statusBarColor,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor), alpha);
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor          状态栏颜色
     * @param statusBarColorTransform the status bar color transform 状态栏变换后的颜色
     * @param alpha                   the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColor(String statusBarColor,
                                       String statusBarColorTransform,
                                       @FloatRange(from = 0f, to = 1f) float alpha) {
        return this.statusBarColorInt(Color.parseColor(statusBarColor),
            Color.parseColor(statusBarColorTransform),
            alpha);
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor 状态栏颜色，资源文件（R.color.xxx）
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorInt(@ColorInt int statusBarColor) {
        mBarParams.statusBarColor = statusBarColor;
        return this;
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor 状态栏颜色，资源文件（R.color.xxx）
     * @param alpha          the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorInt(@ColorInt int statusBarColor,
                                          @FloatRange(from = 0f, to = 1f) float alpha) {
        mBarParams.statusBarColor = statusBarColor;
        mBarParams.statusBarAlpha = alpha;
        return this;
    }

    /**
     * 状态栏颜色
     *
     * @param statusBarColor          状态栏颜色，资源文件（R.color.xxx）
     * @param statusBarColorTransform the status bar color transform 状态栏变换后的颜色
     * @param alpha                   the alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorInt(@ColorInt int statusBarColor,
                                          @ColorInt int statusBarColorTransform,
                                          @FloatRange(from = 0f, to = 1f) float alpha) {
        mBarParams.statusBarColor = statusBarColor;
        mBarParams.statusBarColorTransform = statusBarColorTransform;
        mBarParams.statusBarAlpha = alpha;
        return this;
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(@ColorRes int navigationBarColor) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor));
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @param navigationAlpha    the navigation alpha 透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(@ColorRes int navigationBarColor,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor), navigationAlpha);
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor          the navigation bar color 导航栏颜色
     * @param navigationBarColorTransform the navigation bar color transform  导航栏变色后的颜色
     * @param navigationAlpha             the navigation alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(@ColorRes int navigationBarColor,
                                           @ColorRes int navigationBarColorTransform,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor),
            ContextCompat.getColor(mActivity, navigationBarColorTransform), navigationAlpha);
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(String navigationBarColor) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor));
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @param navigationAlpha    the navigation alpha 透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(String navigationBarColor,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor), navigationAlpha);
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor          the navigation bar color 导航栏颜色
     * @param navigationBarColorTransform the navigation bar color transform  导航栏变色后的颜色
     * @param navigationAlpha             the navigation alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColor(String navigationBarColor,
                                           String navigationBarColorTransform,
                                           @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        return this.navigationBarColorInt(Color.parseColor(navigationBarColor),
            Color.parseColor(navigationBarColorTransform), navigationAlpha);
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorInt(@ColorInt int navigationBarColor) {
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        return this;
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor the navigation bar color 导航栏颜色
     * @param navigationAlpha    the navigation alpha 透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorInt(@ColorInt int navigationBarColor,
                                              @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarAlpha = navigationAlpha;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        return this;
    }

    /**
     * 导航栏颜色
     *
     * @param navigationBarColor          the navigation bar color 导航栏颜色
     * @param navigationBarColorTransform the navigation bar color transform  导航栏变色后的颜色
     * @param navigationAlpha             the navigation alpha  透明度
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorInt(@ColorInt int navigationBarColor,
                                              @ColorInt int navigationBarColorTransform,
                                              @FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarColorTransform = navigationBarColorTransform;
        mBarParams.navigationBarAlpha = navigationAlpha;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        return this;
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public StatusNavBar barColor(@ColorRes int barColor) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor));
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColor(@ColorRes int barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor), barColor);
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColor(@ColorRes int barColor,
                                 @ColorRes int barColorTransform,
                                 @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(ContextCompat.getColor(mActivity, barColor),
            ContextCompat.getColor(mActivity, barColorTransform), barAlpha);
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public StatusNavBar barColor(String barColor) {
        return this.barColorInt(Color.parseColor(barColor));
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColor(String barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(Color.parseColor(barColor), barAlpha);
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColor(String barColor,
                                 String barColorTransform,
                                 @FloatRange(from = 0f, to = 1f) float barAlpha) {
        return this.barColorInt(Color.parseColor(barColor), Color.parseColor(barColorTransform), barAlpha);
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @return the immersion bar
     */
    public StatusNavBar barColorInt(@ColorInt int barColor) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColor = barColor;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        return this;
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor the bar color
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColorInt(@ColorInt int barColor, @FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColor = barColor;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        return this;
    }

    /**
     * 状态栏和导航栏颜色
     *
     * @param barColor          the bar color
     * @param barColorTransform the bar color transform
     * @param barAlpha          the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barColorInt(@ColorInt int barColor,
                                    @ColorInt int barColorTransform,
                                    @FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarColor = barColor;
        mBarParams.navigationBarColor = barColor;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;

        mBarParams.statusBarColorTransform = barColorTransform;
        mBarParams.navigationBarColorTransform = barColorTransform;

        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        return this;
    }


    /**
     * 状态栏根据透明度最后变换成的颜色
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorTransform(@ColorRes int statusBarColorTransform) {
        return this.statusBarColorTransformInt(ContextCompat.getColor(mActivity, statusBarColorTransform));
    }

    /**
     * 状态栏根据透明度最后变换成的颜色
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorTransform(String statusBarColorTransform) {
        return this.statusBarColorTransformInt(Color.parseColor(statusBarColorTransform));
    }

    /**
     * 状态栏根据透明度最后变换成的颜色
     *
     * @param statusBarColorTransform the status bar color transform
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorTransformInt(@ColorInt int statusBarColorTransform) {
        mBarParams.statusBarColorTransform = statusBarColorTransform;
        return this;
    }

    /**
     * 导航栏根据透明度最后变换成的颜色
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorTransform(@ColorRes int navigationBarColorTransform) {
        return this.navigationBarColorTransformInt(ContextCompat.getColor(mActivity, navigationBarColorTransform));
    }

    /**
     * 导航栏根据透明度最后变换成的颜色
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorTransform(String navigationBarColorTransform) {
        return this.navigationBarColorTransformInt(Color.parseColor(navigationBarColorTransform));
    }

    /**
     * 导航栏根据透明度最后变换成的颜色
     *
     * @param navigationBarColorTransform the m navigation bar color transform
     * @return the immersion bar
     */
    public StatusNavBar navigationBarColorTransformInt(@ColorInt int navigationBarColorTransform) {
        mBarParams.navigationBarColorTransform = navigationBarColorTransform;
        return this;
    }

    /**
     * 状态栏和导航栏根据透明度最后变换成的颜色
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public StatusNavBar barColorTransform(@ColorRes int barColorTransform) {
        return this.barColorTransformInt(ContextCompat.getColor(mActivity, barColorTransform));
    }

    /**
     * 状态栏和导航栏根据透明度最后变换成的颜色
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public StatusNavBar barColorTransform(String barColorTransform) {
        return this.barColorTransformInt(Color.parseColor(barColorTransform));
    }

    /**
     * 状态栏和导航栏根据透明度最后变换成的颜色
     *
     * @param barColorTransform the bar color transform
     * @return the immersion bar
     */
    public StatusNavBar barColorTransformInt(@ColorInt int barColorTransform) {
        mBarParams.statusBarColorTransform = barColorTransform;
        mBarParams.navigationBarColorTransform = barColorTransform;
        return this;
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view the view
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColor(View view) {
        return this.addViewSupportTransformColorInt(view, mBarParams.statusBarColorTransform);
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColor(View view, @ColorRes int viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view, ContextCompat.getColor(mActivity, viewColorAfterTransform));
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColor(View view, @ColorRes int viewColorBeforeTransform,
                                                     @ColorRes int viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view,
            ContextCompat.getColor(mActivity, viewColorBeforeTransform),
            ContextCompat.getColor(mActivity, viewColorAfterTransform));
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColor(View view, String viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view, Color.parseColor(viewColorAfterTransform));
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColor(View view, String viewColorBeforeTransform,
                                                     String viewColorAfterTransform) {
        return this.addViewSupportTransformColorInt(view,
            Color.parseColor(viewColorBeforeTransform),
            Color.parseColor(viewColorAfterTransform));
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                    the view
     * @param viewColorAfterTransform the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColorInt(View view, @ColorInt int viewColorAfterTransform) {
        if (view == null) {
            throw new IllegalArgumentException("View参数不能为空");
        }
        Map<Integer, Integer> map = new HashMap<>();
        map.put(mBarParams.statusBarColor, viewColorAfterTransform);
        mBarParams.viewMap.put(view, map);
        return this;
    }

    /**
     * Add 颜色变换支持View
     *
     * @param view                     the view
     * @param viewColorBeforeTransform the view color before transform
     * @param viewColorAfterTransform  the view color after transform
     * @return the immersion bar
     */
    public StatusNavBar addViewSupportTransformColorInt(View view, @ColorInt int viewColorBeforeTransform,
                                                        @ColorInt int viewColorAfterTransform) {
        if (view == null) {
            throw new IllegalArgumentException("View参数不能为空");
        }
        Map<Integer, Integer> map = new HashMap<>();
        map.put(viewColorBeforeTransform, viewColorAfterTransform);
        mBarParams.viewMap.put(view, map);
        return this;
    }

    /**
     * view透明度
     * View alpha immersion bar.
     *
     * @param viewAlpha the view alpha
     * @return the immersion bar
     */
    public StatusNavBar viewAlpha(@FloatRange(from = 0f, to = 1f) float viewAlpha) {
        mBarParams.viewAlpha = viewAlpha;
        return this;
    }

    /**
     * Remove support view immersion bar.
     *
     * @param view the view
     * @return the immersion bar
     */
    public StatusNavBar removeSupportView(View view) {
        if (view == null) {
            throw new IllegalArgumentException("View参数不能为空");
        }
        Map<Integer, Integer> map = mBarParams.viewMap.get(view);
        if (map.size() != 0) {
            mBarParams.viewMap.remove(view);
        }
        return this;
    }

    /**
     * Remove support all view immersion bar.
     *
     * @return the immersion bar
     */
    public StatusNavBar removeSupportAllView() {
        if (mBarParams.viewMap.size() != 0) {
            mBarParams.viewMap.clear();
        }
        return this;
    }

    /**
     * 有导航栏的情况下，Activity是否全屏显示
     *
     * @param isFullScreen the is full screen
     * @return the immersion bar
     */
    public StatusNavBar fullScreen(boolean isFullScreen) {
        mBarParams.fullScreen = isFullScreen;
        return this;
    }

    /**
     * 状态栏透明度
     *
     * @param statusAlpha the status alpha
     * @return the immersion bar
     */
    public StatusNavBar statusBarAlpha(@FloatRange(from = 0f, to = 1f) float statusAlpha) {
        mBarParams.statusBarAlpha = statusAlpha;
        return this;
    }

    /**
     * 导航栏透明度
     *
     * @param navigationAlpha the navigation alpha
     * @return the immersion bar
     */
    public StatusNavBar navigationBarAlpha(@FloatRange(from = 0f, to = 1f) float navigationAlpha) {
        mBarParams.navigationBarAlpha = navigationAlpha;
        return this;
    }

    /**
     * 状态栏和导航栏透明度
     *
     * @param barAlpha the bar alpha
     * @return the immersion bar
     */
    public StatusNavBar barAlpha(@FloatRange(from = 0f, to = 1f) float barAlpha) {
        mBarParams.statusBarAlpha = barAlpha;
        mBarParams.navigationBarAlpha = barAlpha;
        return this;
    }

    /**
     * 状态栏字体深色或亮色
     *
     * @param isDarkFont true 深色
     * @return the immersion bar
     */
    public StatusNavBar statusBarDarkFont(boolean isDarkFont) {
        return statusBarDarkFont(isDarkFont, 0f);
    }

    /**
     * 状态栏字体深色或亮色，判断设备支不支持状态栏变色来设置状态栏透明度
     * Status bar dark font immersion bar.
     *
     * @param isDarkFont  the is dark font
     * @param statusAlpha the status alpha 如果不支持状态栏字体变色可以使用statusAlpha来指定状态栏透明度，比如白色状态栏的时候可以用到
     * @return the immersion bar
     */
    public StatusNavBar statusBarDarkFont(boolean isDarkFont, @FloatRange(from = 0f, to = 1f) float statusAlpha) {
        mBarParams.darkFont = isDarkFont;
        if (!isDarkFont) {
            mBarParams.flymeOSStatusBarFontColor = 0;
        }
        if (isSupportStatusBarDarkFont()) {
            mBarParams.statusBarAlpha = 0;
        } else {
            mBarParams.statusBarAlpha = statusAlpha;
        }
        return this;
    }

    /**
     * 修改 Flyme OS系统手机状态栏字体颜色，优先级高于statusBarDarkFont(boolean isDarkFont)方法
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public StatusNavBar flymeOSStatusBarFontColor(@ColorRes int flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = ContextCompat.getColor(mActivity, flymeOSStatusBarFontColor);
        return this;
    }

    /**
     * 修改 Flyme OS系统手机状态栏字体颜色，优先级高于statusBarDarkFont(boolean isDarkFont)方法
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public StatusNavBar flymeOSStatusBarFontColor(String flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = Color.parseColor(flymeOSStatusBarFontColor);
        return this;
    }

    /**
     * 修改 Flyme OS系统手机状态栏字体颜色，优先级高于statusBarDarkFont(boolean isDarkFont)方法
     * Flyme os status bar font color immersion bar.
     *
     * @param flymeOSStatusBarFontColor the flyme os status bar font color
     * @return the immersion bar
     */
    public StatusNavBar flymeOSStatusBarFontColorInt(@ColorInt int flymeOSStatusBarFontColor) {
        mBarParams.flymeOSStatusBarFontColor = flymeOSStatusBarFontColor;
        return this;
    }

    /**
     * 隐藏导航栏或状态栏
     *
     * @param barHide the bar hide
     * @return the immersion bar
     */
    public StatusNavBar hideStatusOrNavBar(int barHide) {
        mBarParams.barHide = barHide;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT || BarHelperUtils.isEMUI3_1()) {
            if ((mBarParams.barHide == BarParams.FLAG_HIDE_NAVIGATION_BAR) ||
                (mBarParams.barHide == BarParams.FLAG_HIDE_BAR)) {
                mBarParams.hideNavigationBar = true;
            } else {
                mBarParams.hideNavigationBar = false;
            }
        }
        return this;
    }

    /**
     * 解决布局与状态栏重叠问题
     *
     * @param fits the fits
     * @return the immersion bar
     */
    public StatusNavBar fitsSystemWindows(boolean fits) {
        mBarParams.fits = fits;
        if (mBarParams.fits) {
            if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
                mFitsStatusBarType = FLAG_FITS_SYSTEM_WINDOWS;
            }
        } else {
            mFitsStatusBarType = FLAG_FITS_DEFAULT;
        }
        return this;
    }

    /**
     * 解决布局与状态栏重叠问题，支持侧滑返回
     * Fits system windows immersion bar.
     *
     * @param fits         the fits
     * @param contentColor the content color 整体界面背景色
     * @return the immersion bar
     */
    public StatusNavBar fitsSystemWindows(boolean fits, @ColorRes int contentColor) {
        return fitsSystemWindowsInt(fits, ContextCompat.getColor(mActivity, contentColor));
    }

    /**
     * 解决布局与状态栏重叠问题，支持侧滑返回
     * Fits system windows immersion bar.
     *
     * @param fits                  the fits
     * @param contentColor          the content color 整体界面背景色
     * @param contentColorTransform the content color transform  整体界面变换后的背景色
     * @param contentAlpha          the content alpha 整体界面透明度
     * @return the immersion bar
     */
    public StatusNavBar fitsSystemWindows(boolean fits, @ColorRes int contentColor
        , @ColorRes int contentColorTransform, @FloatRange(from = 0f, to = 1f) float contentAlpha) {
        return fitsSystemWindowsInt(fits, ContextCompat.getColor(mActivity, contentColor),
            ContextCompat.getColor(mActivity, contentColorTransform), contentAlpha);
    }

    /**
     * 解决布局与状态栏重叠问题，支持侧滑返回
     * Fits system windows int immersion bar.
     *
     * @param fits         the fits
     * @param contentColor the content color 整体界面背景色
     * @return the immersion bar
     */
    public StatusNavBar fitsSystemWindowsInt(boolean fits, @ColorInt int contentColor) {
        return fitsSystemWindowsInt(fits, contentColor, Color.BLACK, 0);
    }

    /**
     * 解决布局与状态栏重叠问题，支持侧滑返回
     * Fits system windows int immersion bar.
     *
     * @param fits                  the fits
     * @param contentColor          the content color 整体界面背景色
     * @param contentColorTransform the content color transform 整体界面变换后的背景色
     * @param contentAlpha          the content alpha 整体界面透明度
     * @return the immersion bar
     */
    public StatusNavBar fitsSystemWindowsInt(boolean fits, @ColorInt int contentColor
        , @ColorInt int contentColorTransform, @FloatRange(from = 0f, to = 1f) float contentAlpha) {
        mBarParams.fits = fits;
        mBarParams.contentColor = contentColor;
        mBarParams.contentColorTransform = contentColorTransform;
        mBarParams.contentAlpha = contentAlpha;
        if (mBarParams.fits) {
            if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
                mFitsStatusBarType = FLAG_FITS_SYSTEM_WINDOWS;
            }
        } else {
            mFitsStatusBarType = FLAG_FITS_DEFAULT;
        }
        mContentView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.contentColor,
            mBarParams.contentColorTransform, mBarParams.contentAlpha));
        return this;
    }

    /**
     * 通过状态栏高度动态设置状态栏布局
     *
     * @param view the view
     * @return the immersion bar
     */
    public StatusNavBar statusBarView(View view) {
        if (view == null) {
            return this;
        }
        mBarParams.statusBarView = view;
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_STATUS;
        }
        return this;
    }

    /**
     * 通过状态栏高度动态设置状态栏布局,只能在Activity中使用
     *
     * @param viewId the view id
     * @return the immersion bar
     */
    public StatusNavBar statusBarView(@IdRes int viewId) {
        return statusBarView(mActivity.findViewById(viewId));
    }

    /**
     * 通过状态栏高度动态设置状态栏布局
     * Status bar view immersion bar.
     *
     * @param viewId   the view id
     * @param rootView the root view
     * @return the immersion bar
     */
    public StatusNavBar statusBarView(@IdRes int viewId, View rootView) {
        return statusBarView(rootView.findViewById(viewId));
    }

    /**
     * 支持有actionBar的界面,调用该方法，布局讲从actionBar下面开始绘制
     * Support action bar immersion bar.
     *
     * @param isSupportActionBar the is support action bar
     * @return the immersion bar
     */
    public StatusNavBar supportActionBar(boolean isSupportActionBar) {
        mBarParams.isSupportActionBar = isSupportActionBar;
        return this;
    }

    /**
     * 解决状态栏与布局顶部重叠又多了种方法
     * Title bar immersion bar.
     *
     * @param view the view
     * @return the immersion bar
     */
    public StatusNavBar titleBar(View view) {
        if (view == null) {
            return this;
        }
        return titleBar(view, true);
    }

    /**
     * 解决状态栏与布局顶部重叠又多了种方法
     * Title bar immersion bar.
     *
     * @param view          the view
     * @param statusBarFlag the status bar flag 默认为true false表示状态栏不支持变色，true表示状态栏支持变色
     * @return the immersion bar
     */
    public StatusNavBar titleBar(View view, boolean statusBarFlag) {
        if (view == null) {
            return this;
        }
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_TITLE;
        }
        mBarParams.titleBarView = view;
        mBarParams.statusBarFlag = statusBarFlag;
        return this;
    }

    /**
     * 解决状态栏与布局顶部重叠又多了种方法，只支持Activity
     * Title bar immersion bar.
     *
     * @param viewId the view id
     * @return the immersion bar
     */
    public StatusNavBar titleBar(@IdRes int viewId) {
        return titleBar(mActivity.findViewById(viewId), true);
    }

    /**
     * Title bar immersion bar.
     *
     * @param viewId        the view id
     * @param statusBarFlag the status bar flag
     * @return the immersion bar
     */
    public StatusNavBar titleBar(@IdRes int viewId, boolean statusBarFlag) {
        return titleBar(mActivity.findViewById(viewId), statusBarFlag);
    }

    /**
     * Title bar immersion bar.
     *
     * @param viewId   the view id
     * @param rootView the root view
     * @return the immersion bar
     */
    public StatusNavBar titleBar(@IdRes int viewId, View rootView) {
        return titleBar(rootView.findViewById(viewId), true);
    }

    /**
     * 解决状态栏与布局顶部重叠又多了种方法，支持任何view
     * Title bar immersion bar.
     *
     * @param viewId        the view id
     * @param rootView      the root view
     * @param statusBarFlag the status bar flag 默认为true false表示状态栏不支持变色，true表示状态栏支持变色
     * @return the immersion bar
     */
    public StatusNavBar titleBar(@IdRes int viewId, View rootView, boolean statusBarFlag) {
        return titleBar(rootView.findViewById(viewId), statusBarFlag);
    }

    /**
     * 绘制标题栏距离顶部的高度为状态栏的高度
     * Title bar margin top immersion bar.
     *
     * @param viewId the view id   标题栏资源id
     * @return the immersion bar
     */
    public StatusNavBar titleBarMarginTop(@IdRes int viewId) {
        return titleBarMarginTop(mActivity.findViewById(viewId));
    }

    /**
     * 绘制标题栏距离顶部的高度为状态栏的高度
     * Title bar margin top immersion bar.
     *
     * @param viewId   the view id  标题栏资源id
     * @param rootView the root view  布局view
     * @return the immersion bar
     */
    public StatusNavBar titleBarMarginTop(@IdRes int viewId, View rootView) {
        return titleBarMarginTop(rootView.findViewById(viewId));
    }

    /**
     * 绘制标题栏距离顶部的高度为状态栏的高度
     * Title bar margin top immersion bar.
     *
     * @param view the view  要改变的标题栏view
     * @return the immersion bar
     */
    public StatusNavBar titleBarMarginTop(View view) {
        if (view == null) {
            return this;
        }
        if (mFitsStatusBarType == FLAG_FITS_DEFAULT) {
            mFitsStatusBarType = FLAG_FITS_TITLE_MARGIN_TOP;
        }
        mBarParams.titleBarView = view;
        return this;
    }

    /**
     * Status bar color transform enable immersion bar.
     *
     * @param statusBarFlag the status bar flag
     * @return the immersion bar
     */
    public StatusNavBar statusBarColorTransformEnable(boolean statusBarFlag) {
        mBarParams.statusBarFlag = statusBarFlag;
        return this;
    }

    /**
     * 一键重置所有参数
     * Reset immersion bar.
     *
     * @return the immersion bar
     */
    public StatusNavBar reset() {
        mBarParams = new BarParams();
        return this;
    }

    /**
     * 给某个页面设置tag来标识这页bar的属性.
     * Add tag bar tag.
     *
     * @param tag the tag
     * @return the bar tag
     */
    public StatusNavBar addTag(String tag) {
        if (isEmpty(tag)) {
            throw new IllegalArgumentException("tag不能为空");
        }
        BarParams barParams = mBarParams.clone();
        mTagMap.put(tag, barParams);
        return this;
    }

    /**
     * 根据tag恢复到某次调用时的参数
     * Recover immersion bar.
     *
     * @param tag the tag
     * @return the immersion bar
     */
    public StatusNavBar getTag(String tag) {
        if (isEmpty(tag)) {
            throw new IllegalArgumentException("tag不能为空");
        }
        BarParams barParams = mTagMap.get(tag);
        if (barParams != null) {
            mBarParams = barParams.clone();
        }
        return this;
    }

    /**
     * 解决软键盘与底部输入框冲突问题 ，默认是false
     * Keyboard enable immersion bar.
     *
     * @param enable the enable
     * @return the immersion bar
     */
    public StatusNavBar keyboardEnable(boolean enable) {
        return keyboardEnable(enable, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    /**
     * 解决软键盘与底部输入框冲突问题 ，默认是false
     *
     * @param enable       the enable
     * @param keyboardMode the keyboard mode
     * @return the immersion bar
     */
    public StatusNavBar keyboardEnable(boolean enable, int keyboardMode) {
        mBarParams.keyboardEnable = enable;
        mBarParams.keyboardMode = keyboardMode;
        return this;
    }

    /**
     * 修改键盘模式
     * Keyboard mode immersion bar.
     *
     * @param keyboardMode the keyboard mode
     * @return the immersion bar
     */
    public StatusNavBar keyboardMode(int keyboardMode) {
        mBarParams.keyboardMode = keyboardMode;
        return this;
    }

    /**
     * 软键盘弹出关闭的回调监听
     * Sets on keyboard listener.
     *
     * @param onKeyboardListener the on keyboard listener
     * @return the on keyboard listener
     */
    public StatusNavBar setOnKeyboardListener(OnKeyboardListener onKeyboardListener) {
        if (mBarParams.onKeyboardListener == null) {
            mBarParams.onKeyboardListener = onKeyboardListener;
        }
        return this;
    }

    /**
     * 是否可以修改导航栏颜色，默认为true
     * Navigation bar enable immersion bar.
     *
     * @param navigationBarEnable the enable
     * @return the immersion bar
     */
    public StatusNavBar navigationBarEnable(boolean navigationBarEnable) {
        mBarParams.navigationBarEnable = navigationBarEnable;
        return this;
    }

    /**
     * 是否可以修改4.4设备导航栏颜色，默认为true
     *
     * @param navigationBarWithKitkatEnable the navigation bar with kitkat enable
     * @return the immersion bar
     */
    public StatusNavBar navigationBarWithKitkatEnable(boolean navigationBarWithKitkatEnable) {
        mBarParams.navigationBarWithKitkatEnable = navigationBarWithKitkatEnable;
        return this;
    }

    /**
     * 当xml里使用android:fitsSystemWindows="true"属性时，
     * 解决4.4和emui3.1手机底部有时会出现多余空白的问题 ，已过时，代码中没用的此处
     * Fix margin atbottom immersion bar.
     *
     * @param fixMarginAtBottom the fix margin atbottom
     * @return the immersion bar
     * @deprecated
     */
    @Deprecated
    public StatusNavBar fixMarginAtBottom(boolean fixMarginAtBottom) {
        mBarParams.fixMarginAtBottom = fixMarginAtBottom;
        return this;
    }

    /**
     * 通过上面配置后初始化后方可成功调用
     */
    public void init() {
        //设置沉浸式
        setBar();
        //适配状态栏与布局重叠问题
        fitsLayoutOverlap();
        //变色view
        transformView();
        //解决软键盘与底部输入框冲突问题
        keyboardEnable();
    }

    /**
     * 当Activity/Fragment/Dialog关闭的时候调用
     */
    public void destroy() {
        unRegisterEMUI3_x();
        if (mBarParams.keyboardPatch != null) {
            //取消监听
            mBarParams.keyboardPatch.disable(mBarParams.keyboardMode);
            mBarParams.keyboardPatch = null;
        }
        //删除当前界面对应的ImmersionBar对象
        Iterator<Map.Entry<String, StatusNavBar>> iterator = mImmersionBarMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, StatusNavBar> entry = iterator.next();
            if (entry.getKey().contains(mImmersionBarName) || (entry.getKey().equals(mImmersionBarName))) {
                iterator.remove();
            }
        }
    }

    /**
     * 初始化状态栏和导航栏
     */
    private void setBar() {
        //获得Bar相关信息
        mBarConfig = new BarConfig(mActivity);
        //如果是非Activity中使用，让Activity同步非Activity的BarParams的导航栏参数
        if (!mIsActivity) {
            Objects.requireNonNull(mImmersionBarMap.get(mActivity.toString())).mBarParams.navigationBarEnable
                = mBarParams.navigationBarEnable;
            Objects.requireNonNull(mImmersionBarMap.get(mActivity.toString())).mBarParams.navigationBarWithKitkatEnable
                = mBarParams.navigationBarWithKitkatEnable;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //防止系统栏隐藏时内容区域大小发生变化
            int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !BarHelperUtils.isEMUI3_1()) {
                //初始化5.0以上，包含5.0
                uiFlags = initBarAboveLOLLIPOP(uiFlags);
                //android 6.0以上设置状态栏字体为暗色
                uiFlags = setStatusBarDarkFont(uiFlags);
            } else {
                //初始化5.0以下，4.4以上沉浸式
                initBarBelowLOLLIPOP();
            }
            //隐藏状态栏或者导航栏
            uiFlags = hideBar(uiFlags);
            //修正界面显示
            fitsWindows();
            mWindow.getDecorView().setSystemUiVisibility(uiFlags);
        }
        //修改miui状态栏字体颜色
        if (BarHelperUtils.isMIUI6Later()) {
            setMIUIStatusBarDarkFont(mWindow, mBarParams.darkFont);
        }
        // 修改Flyme OS状态栏字体颜色
        if (BarHelperUtils.isFlymeOS4Later()) {
            if (mBarParams.flymeOSStatusBarFontColor != 0) {
                FlymeOSStatusBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.flymeOSStatusBarFontColor);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    FlymeOSStatusBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.darkFont);
                }
            }
        }
    }

    /**
     * 初始化android 5.0以上状态栏和导航栏
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int initBarAboveLOLLIPOP(int uiFlags) {
        //获得默认导航栏颜色
        if (!mHasNavigationBarColor) {
            mBarParams.defaultNavigationBarColor = mWindow.getNavigationBarColor();
            mHasNavigationBarColor = true;
        }
        //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态栏遮住。
        uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (mBarParams.fullScreen && mBarParams.navigationBarEnable) {
            //Activity全屏显示，但导航栏不会被隐藏覆盖，导航栏依然可见，Activity底部布局部分会被导航栏遮住。
            uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //判断是否存在导航栏
        if (mBarConfig.hasNavigationBar()) {
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //需要设置这个才能设置状态栏和导航栏颜色
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        if (mBarParams.statusBarFlag) {
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));
        } else {
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                Color.TRANSPARENT, mBarParams.statusBarAlpha));
        }
        //设置导航栏颜色
        if (mBarParams.navigationBarEnable) {
            mWindow.setNavigationBarColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));
        } else {
            mWindow.setNavigationBarColor(mBarParams.defaultNavigationBarColor);
        }
        return uiFlags;
    }

    /**
     * 初始化android 4.4和emui3.1状态栏和导航栏
     */
    private void initBarBelowLOLLIPOP() {
        //透明状态栏
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //创建一个假的状态栏
        setupStatusBarView();
        //判断是否存在导航栏，是否禁止设置导航栏
        if (mBarConfig.hasNavigationBar() || BarHelperUtils.isEMUI3_1() || BarHelperUtils.isEMUI3_0()) {
            if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                //透明导航栏，设置这个，如果有导航栏，底部布局会被导航栏遮住
                mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else {
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            if (mNavigationBarHeight == 0) {
                mNavigationBarHeight = mBarConfig.getNavigationBarHeight();
            }
            if (mNavigationBarWidth == 0) {
                mNavigationBarWidth = mBarConfig.getNavigationBarWidth();
            }
            //创建一个假的导航栏
            setupNavBarView();
        }
    }

    /**
     * 设置一个可以自定义颜色的状态栏
     */
    private void setupStatusBarView() {
        View statusBarView = mDecorView.findViewById(IMMERSION_STATUS_BAR_VIEW);
        if (statusBarView == null) {
            statusBarView = new View(mActivity);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                mBarConfig.getStatusBarHeight());
            params.gravity = Gravity.TOP;
            statusBarView.setLayoutParams(params);
            statusBarView.setVisibility(View.VISIBLE);
            statusBarView.setId(IMMERSION_STATUS_BAR_VIEW);
            mDecorView.addView(statusBarView);
        }
        if (mBarParams.statusBarFlag) {
            statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));
        } else {
            statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                Color.TRANSPARENT, mBarParams.statusBarAlpha));
        }
    }

    /**
     * 设置一个可以自定义颜色的导航栏
     */
    private void setupNavBarView() {
        View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW);
        if (navigationBarView == null) {
            navigationBarView = new View(mActivity);
            navigationBarView.setId(IMMERSION_NAVIGATION_BAR_VIEW);
            mDecorView.addView(navigationBarView);
        }

        FrameLayout.LayoutParams params;
        if (mBarConfig.isNavigationAtBottom()) {
            params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mBarConfig.getNavigationBarHeight());
            params.gravity = Gravity.BOTTOM;
        } else {
            params = new FrameLayout.LayoutParams(mBarConfig.getNavigationBarWidth(), FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.END;
        }
        navigationBarView.setLayoutParams(params);
        navigationBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
            mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));

        if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable && !mBarParams.hideNavigationBar) {
            navigationBarView.setVisibility(View.VISIBLE);
        } else {
            navigationBarView.setVisibility(View.GONE);
        }
    }

    /**
     * Hide bar.
     * 隐藏或显示状态栏和导航栏。
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    private int hideBar(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            switch (mBarParams.barHide) {
                case BarParams.FLAG_HIDE_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.INVISIBLE;
                    break;
                case BarParams.FLAG_HIDE_STATUS_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.INVISIBLE;
                    break;
                case BarParams.FLAG_HIDE_NAVIGATION_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                    break;
                case BarParams.FLAG_SHOW_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_VISIBLE;
                    break;
                default:
                    break;
            }
        }
        return uiFlags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    /**
     * 修正界面显示
     */
    private void fitsWindows() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !BarHelperUtils.isEMUI3_1()) {
            //android 5.0以上解决状态栏和布局重叠问题
            fitsWindowsAboveLOLLIPOP();
        } else {
            //解决android4.4有导航栏的情况下，activity底部被导航栏遮挡的问题和android 5.0以下解决状态栏和布局重叠问题
            fitsWindowsBelowLOLLIPOP();
            //解决华为emui3.1或者3.0导航栏手动隐藏的问题
            if (mIsActivity && ((BarHelperUtils.isEMUI3_0() || BarHelperUtils.isEMUI3_1()))) {
                fitsWindowsEMUI();
            }
        }
    }

    /**
     * android 5.0以上解决状态栏和布局重叠问题
     * Fits windows above lollipop.
     */
    private void fitsWindowsAboveLOLLIPOP() {
        int top = 0;
        for (int i = 0, count = mContentView.getChildCount(); i < count; i++) {
            View childView = mContentView.getChildAt(i);
            if (childView instanceof ViewGroup) {
                if (childView instanceof DrawerLayout) {
                    continue;
                }
                if (childView.getFitsSystemWindows()) {
                    if (mBarParams.isSupportActionBar) {
                        top = mBarConfig.getActionBarHeight();
                    }
                    mContentView.setPadding(0, top, 0, 0);
                    return;
                }
            }
        }
        if (mBarParams.fits && mFitsStatusBarType == FLAG_FITS_SYSTEM_WINDOWS) {
            top = mBarConfig.getStatusBarHeight();
        }
        if (mBarParams.isSupportActionBar) {
            top = mBarConfig.getStatusBarHeight() + mBarConfig.getActionBarHeight();
        }
        mContentView.setPadding(0, top, 0, 0);
    }

    /**
     * 解决android4.4有导航栏的情况下，activity底部被导航栏遮挡的问题和android 5.0以下解决状态栏和布局重叠问题
     * Fits windows below lollipop.
     */
    private void fitsWindowsBelowLOLLIPOP() {
        int top = 0, right = 0, bottom = 0;
        for (int i = 0, count = mContentView.getChildCount(); i < count; i++) {
            View childView = mContentView.getChildAt(i);
            if (childView instanceof ViewGroup) {
                if (childView instanceof DrawerLayout) {
                    continue;
                }
                if (childView.getFitsSystemWindows()) {
                    if (mBarParams.isSupportActionBar) {
                        top = mBarConfig.getActionBarHeight();
                    }
                    mContentView.setPadding(0, top, right, bottom);
                    return;
                }
            }
        }
        if (mBarParams.fits && mFitsStatusBarType == FLAG_FITS_SYSTEM_WINDOWS) {
            top = mBarConfig.getStatusBarHeight();
        }
        if (mBarParams.isSupportActionBar) {
            top = mBarConfig.getStatusBarHeight() + mBarConfig.getActionBarHeight();
        }
        if (mBarConfig.hasNavigationBar() && mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
            if (!mBarParams.fullScreen) {
                if (mBarConfig.isNavigationAtBottom()) {
                    bottom = mBarConfig.getNavigationBarHeight();
                } else {
                    right = mBarConfig.getNavigationBarWidth();
                }
            }
            if (mBarParams.hideNavigationBar) {
                if (mBarConfig.isNavigationAtBottom()) {
                    bottom = 0;
                } else {
                    right = 0;
                }
            } else {
                if (!mBarConfig.isNavigationAtBottom()) {
                    right = mBarConfig.getNavigationBarWidth();
                }
            }

        }
        mContentView.setPadding(0, top, right, bottom);
    }

    /**
     * 注册emui3.x导航栏监听函数
     * Register emui 3 x.
     */
    private void fitsWindowsEMUI() {
        final View navigationBarView = mDecorView.findViewById(IMMERSION_NAVIGATION_BAR_VIEW);
        if (navigationBarView != null && mNavigationObserver == null) {
            mNavigationObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                        mBarConfig = new BarConfig(mActivity);
                        int bottom = mContentView.getPaddingBottom(), right = mContentView.getPaddingRight();
                        if (mActivity != null && mActivity.getContentResolver() != null) {
                            int navigationBarIsMin = Settings.System.getInt(mActivity.getContentResolver(),
                                NAVIGATIONBAR_IS_MIN, 0);
                            if (navigationBarIsMin == 1) {
                                //导航键隐藏了
                                navigationBarView.setVisibility(View.GONE);
                                bottom = 0;
                                right = 0;
                            } else {
                                if (mNavigationBarHeight == 0) {
                                    mNavigationBarHeight = mBarConfig.getNavigationBarHeight();
                                }
                                if (mNavigationBarWidth == 0) {
                                    mNavigationBarWidth = mBarConfig.getNavigationBarWidth();
                                }
                                if (!mBarParams.hideNavigationBar) {
                                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) navigationBarView.getLayoutParams();
                                    if (mBarConfig.isNavigationAtBottom()) {
                                        params.height = mNavigationBarHeight;
                                        params.gravity = Gravity.BOTTOM;
                                        bottom = mNavigationBarHeight;
                                        right = 0;
                                    } else {
                                        params.width = mNavigationBarWidth;
                                        params.gravity = Gravity.END;
                                        bottom = 0;
                                        right = mNavigationBarWidth;
                                    }
                                    navigationBarView.setLayoutParams(params);
                                    //导航键显示了
                                    navigationBarView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                        mContentView.setPadding(0, mContentView.getPaddingTop(), right, bottom);
                    } else {
                        navigationBarView.setVisibility(View.GONE);
                    }
                }
            };
            if (mActivity != null && mActivity.getContentResolver() != null && mNavigationObserver != null) {
                mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor
                    (NAVIGATIONBAR_IS_MIN), true, mNavigationObserver);
            }
        }
    }

    /**
     * Sets status bar dark font.
     * 设置状态栏字体颜色，android6.0以上
     */
    private int setStatusBarDarkFont(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mBarParams.darkFont) {
            return uiFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            return uiFlags;
        }
    }

    /**
     * 设置状态栏字体图标为深色，需要MIUIV6以上
     *
     * @param window   the window
     * @param darkFont the dark font
     */
    @SuppressLint("PrivateApi")
    private void setMIUIStatusBarDarkFont(Window window, boolean darkFont) {
        if (window != null) {
            Class<? extends Window> clazz = window.getClass();
            try {
                int darkModeFlag;
                Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (darkFont) {
                    //状态栏透明且黑色字体
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);
                } else {
                    //清除黑色字体
                    extraFlagField.invoke(window, 0, darkModeFlag);
                }
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * 适配状态栏与布局重叠问题
     * Fits layout overlap.
     */
    private void fitsLayoutOverlap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !mIsFitsLayoutOverlap) {
            switch (mFitsStatusBarType) {
                case FLAG_FITS_TITLE:
                    //通过设置paddingTop重新绘制标题栏高度
                    setTitleBar(mActivity, mBarParams.titleBarView);
                    mIsFitsLayoutOverlap = true;
                    break;
                case FLAG_FITS_TITLE_MARGIN_TOP:
                    //通过设置marginTop重新绘制标题栏高度
                    setTitleBarMarginTop(mActivity, mBarParams.titleBarView);
                    mIsFitsLayoutOverlap = true;
                    break;
                case FLAG_FITS_STATUS:
                    //通过状态栏高度动态设置状态栏布局
                    setStatusBarView(mActivity, mBarParams.statusBarView);
                    mIsFitsLayoutOverlap = true;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 变色view
     * Transform view.
     */
    private void transformView() {
        if (mBarParams.viewMap.size() != 0) {
            Set<Map.Entry<View, Map<Integer, Integer>>> entrySet = mBarParams.viewMap.entrySet();
            for (Map.Entry<View, Map<Integer, Integer>> entry : entrySet) {
                View view = entry.getKey();
                Map<Integer, Integer> map = entry.getValue();
                Integer colorBefore = mBarParams.statusBarColor;
                Integer colorAfter = mBarParams.statusBarColorTransform;
                for (Map.Entry<Integer, Integer> integerEntry : map.entrySet()) {
                    colorBefore = integerEntry.getKey();
                    colorAfter = integerEntry.getValue();
                }
                if (view != null) {
                    if (Math.abs(mBarParams.viewAlpha - 0.0f) == 0) {
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.statusBarAlpha));
                    } else {
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.viewAlpha));
                    }
                }
            }
        }
    }

    /**
     * 取消注册emui3.x导航栏监听函数
     * Un register emui 3 x.
     */
    private void unRegisterEMUI3_x() {
        if (mActivity != null && mNavigationObserver != null) {
            mActivity.getContentResolver().unregisterContentObserver(mNavigationObserver);
            mNavigationObserver = null;
        }
    }

    /**
     * 解决底部输入框与软键盘问题
     * Keyboard enable.
     */
    private void keyboardEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mBarParams.keyboardPatch == null) {
                mBarParams.keyboardPatch = KeyboardPatch.patch(mActivity, mWindow);
            }
            mBarParams.keyboardPatch.setBarParams(mBarParams);
            //解决软键盘与底部输入框冲突问题
            if (mBarParams.keyboardEnable) {
                mBarParams.keyboardPatch.enable(mBarParams.keyboardMode);
            } else {
                mBarParams.keyboardPatch.disable(mBarParams.keyboardMode);
            }
        }
    }

    /**
     * Gets bar params.
     *
     * @return the bar params
     */
    public BarParams getBarParams() {
        return mBarParams;
    }

    /**
     * 判断手机支不支持状态栏字体变色
     * Is support status bar dark font boolean.
     *
     * @return the boolean
     */
    public static boolean isSupportStatusBarDarkFont() {
        return BarHelperUtils.isMIUI6Later() || BarHelperUtils.isFlymeOS4Later()
            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    /**
     * 单独设置标题栏的高度
     * Sets title bar.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setTitleBar(final Activity activity, final View view) {
        if (activity == null) {
            return;
        }
        if (view == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT ||
                layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        layoutParams.height = view.getHeight() + getStatusBarHeight(activity);
                        view.setPadding(view.getPaddingLeft(),
                            view.getPaddingTop() + getStatusBarHeight(activity),
                            view.getPaddingRight(),
                            view.getPaddingBottom());
                    }
                });
            } else {
                layoutParams.height += getStatusBarHeight(activity);
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + getStatusBarHeight(activity),
                    view.getPaddingRight(), view.getPaddingBottom());
            }
        }
    }

    /**
     * 设置标题栏MarginTop值为导航栏的高度
     * Sets title bar margin top.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setTitleBarMarginTop(Activity activity, View view) {
        if (activity == null) {
            return;
        }
        if (view == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            layoutParams.setMargins(layoutParams.leftMargin,
                layoutParams.topMargin + getStatusBarHeight(activity),
                layoutParams.rightMargin,
                layoutParams.bottomMargin);
        }
    }

    /**
     * 单独在标题栏的位置增加view，高度为状态栏的高度
     * Sets status bar view.
     *
     * @param activity the activity
     * @param view     the view
     */
    public static void setStatusBarView(Activity activity, View view) {
        if (activity == null) {
            return;
        }
        if (view == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = getStatusBarHeight(activity);
            view.setLayoutParams(params);
        }
    }

    /**
     * 解决顶部与布局重叠问题
     * Sets fits system windows.
     *
     * @param activity the activity
     */
    public static void setFitsSystemWindows(Activity activity) {
        if (activity == null) {
            return;
        }
        ViewGroup parent = activity.findViewById(android.R.id.content);
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            View childView = parent.getChildAt(i);
            if (childView instanceof ViewGroup) {
                childView.setFitsSystemWindows(true);
                ((ViewGroup) childView).setClipToPadding(true);
            }
        }
    }

    /**
     * Has navigtion bar boolean.
     * 判断是否存在导航栏
     *
     * @param activity the activity
     * @return the boolean
     */
    @TargetApi(14)
    public static boolean hasNavigationBar(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.hasNavigationBar();
    }

    /**
     * Gets navigation bar height.
     * 获得导航栏的高度
     *
     * @param activity the activity
     * @return the navigation bar height
     */
    @TargetApi(14)
    public static int getNavigationBarHeight(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.getNavigationBarHeight();
    }

    /**
     * Gets navigation bar width.
     * 获得导航栏的宽度
     *
     * @param activity the activity
     * @return the navigation bar width
     */
    @TargetApi(14)
    public static int getNavigationBarWidth(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.getNavigationBarWidth();
    }

    /**
     * Is navigation at bottom boolean.
     * 判断导航栏是否在底部
     *
     * @param activity the activity
     * @return the boolean
     */
    @TargetApi(14)
    public static boolean isNavigationAtBottom(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.isNavigationAtBottom();
    }

    /**
     * Gets status bar height.
     * 或得状态栏的高度
     *
     * @param activity the activity
     * @return the status bar height
     */
    @TargetApi(14)
    public static int getStatusBarHeight(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.getStatusBarHeight();
    }

    /**
     * Gets action bar height.
     * 或得ActionBar得高度
     *
     * @param activity the activity
     * @return the action bar height
     */
    @TargetApi(14)
    public static int getActionBarHeight(@NonNull Activity activity) {
        BarConfig config = new BarConfig(activity);
        return config.getActionBarHeight();
    }

    /**
     * 隐藏状态栏
     * Hide status bar.
     *
     * @param window the window
     */
    public static void hideStatusBar(@NonNull Window window) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }
}

class BarParams implements Cloneable {
    /**
     * 状态栏颜色
     * The Status bar color.
     */
    @ColorInt
    int statusBarColor = Color.TRANSPARENT;
    /**
     * 导航栏颜色
     * The Navigation bar color.
     */
    @ColorInt
    int navigationBarColor = Color.BLACK;

    int defaultNavigationBarColor = Color.BLACK;
    /**
     * 状态栏透明度
     * The Status bar alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    float statusBarAlpha = 0.0f;
    /**
     * 导航栏透明度
     * The Navigation bar alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    float navigationBarAlpha = 0.0f;
    /**
     * 有导航栏的情况，全屏显示
     * The Full screen.
     */
    public boolean fullScreen = false;
    /**
     * 是否隐藏了导航栏
     * The Hide navigation bar.
     */
    boolean hideNavigationBar = false;
    /**
     * 隐藏Bar
     * The Bar hide.
     */
    public int barHide = FLAG_SHOW_BAR;
    /**
     * 状态栏字体深色与亮色标志位
     * The Dark font.
     */
    boolean darkFont = false;
    /**
     * 是否可以修改状态栏颜色
     * The Status bar flag.
     */
    boolean statusBarFlag = true;
    /**
     * 状态栏变换后的颜色
     * The Status bar color transform.
     */
    @ColorInt
    int statusBarColorTransform = Color.BLACK;
    /**
     * 导航栏变换后的颜色
     * The Navigation bar color transform.
     */
    @ColorInt
    int navigationBarColorTransform = Color.BLACK;
    /**
     * 支持view变色
     * The View map.
     */
    Map<View, Map<Integer, Integer>> viewMap = new HashMap<>();
    /**
     * The View alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    float viewAlpha = 0.0f;
    /**
     * The Status bar color content view.
     */
    @ColorInt
    int contentColor = Color.TRANSPARENT;
    /**
     * The Status bar color content view transform.
     */
    @ColorInt
    int contentColorTransform = Color.BLACK;
    /**
     * The Status bar content view alpha.
     */
    @FloatRange(from = 0f, to = 1f)
    float contentAlpha = 0.0f;
    /**
     * The Navigation bar color temp.
     */
    int navigationBarColorTemp = navigationBarColor;
    /**
     * 解决标题栏与状态栏重叠问题
     * The Fits.
     */
    public boolean fits = false;
    /**
     * 解决标题栏与状态栏重叠问题
     * The Title bar view.
     */
    View titleBarView;
    /**
     * 解决标题栏与状态栏重叠问题
     * The Status bar view by height.
     */
    View statusBarView;
    /**
     * flymeOS状态栏字体变色
     * The Flyme os status bar font color.
     */
    @ColorInt
    int flymeOSStatusBarFontColor;
    /**
     * 结合actionBar使用
     * The Is support action bar.
     */
    boolean isSupportActionBar = false;
    /**
     * 解决软键盘与输入框冲突问题
     * The Keyboard enable.
     */
    public boolean keyboardEnable = false;
    /**
     * 软键盘属性
     * The Keyboard mode.
     */
    int keyboardMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    /**
     * 是否能修改导航栏颜色
     * The Navigation bar enable.
     */
    boolean navigationBarEnable = true;
    /**
     * 是否能修改4.4手机导航栏颜色
     * The Navigation bar with kitkat enable.
     */
    boolean navigationBarWithKitkatEnable = true;
    /**
     * 解决出现底部多余导航栏高度，默认为false，已废弃
     * The Fix margin at bottom.
     */
    @Deprecated
    boolean fixMarginAtBottom = false;
    /**
     * xml是否使用fitsSystemWindows属性
     * The System windows.
     */
    boolean systemWindows = false;
    /**
     * 软键盘监听类
     * The Keyboard patch.
     */
    KeyboardPatch keyboardPatch;
    /**
     * 软键盘监听类
     * The On keyboard listener.
     */
    OnKeyboardListener onKeyboardListener;
    /**
     * 隐藏状态栏
     * Flag hide status bar bar hide.
     */
    public static final int FLAG_HIDE_STATUS_BAR = 0;
    /**
     * 隐藏导航栏
     * Flag hide navigation bar bar hide.
     */
    public static final int FLAG_HIDE_NAVIGATION_BAR = 1;
    /**
     * 隐藏状态栏和导航栏
     * Flag hide bar bar hide.
     */
    public static final int FLAG_HIDE_BAR = 2;
    /**
     * 显示状态栏和导航栏
     * Flag show bar bar hide.
     */
    public static final int FLAG_SHOW_BAR = 3;

    @Override

    protected BarParams clone() {
        BarParams barParams = null;
        try {
            barParams = (BarParams) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return barParams;
    }
}

interface OnKeyboardListener {
    /**
     * On keyboard change.
     *
     * @param isPopup        the is popup  是否弹出
     * @param keyboardHeight the keyboard height  软键盘高度
     */
    void onKeyboardChange(boolean isPopup, int keyboardHeight);
}

class KeyboardPatch {

    private Activity mActivity;
    private Window mWindow;
    private View mDecorView;
    private View mContentView;
    private View mChildView;

    private BarParams mBarParams;

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    private int keyboardHeightPrevious;
    private int statusBarHeight;
    private int actionBarHeight;
    private int navigationBarHeight;
    private boolean navigationAtBottom;

    private KeyboardPatch(Activity activity) {
        this(activity, ((FrameLayout) activity.getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0));
    }

    private KeyboardPatch(Activity activity, View contentView) {
        this(activity, null, "", contentView);
    }

    private KeyboardPatch(Activity activity, Dialog dialog, String tag) {
        this(activity, dialog, tag, dialog.getWindow().findViewById(android.R.id.content));
    }

    private KeyboardPatch(Activity activity, Dialog dialog, String tag, View contentView) {
        this.mActivity = activity;
        this.mWindow = dialog != null ? dialog.getWindow() : activity.getWindow();
        this.mDecorView = mWindow.getDecorView();
        this.mContentView = contentView != null ? contentView
            : mWindow.getDecorView().findViewById(android.R.id.content);
        this.mBarParams = dialog != null ? StatusNavBar.with(activity, dialog, tag).getBarParams()
            : StatusNavBar.with(activity).getBarParams();
        if (mBarParams == null)
            throw new IllegalArgumentException("先使用ImmersionBar初始化");
    }

    private KeyboardPatch(Activity activity, Window window) {
        this.mActivity = activity;
        this.mWindow = window;
        this.mDecorView = mWindow.getDecorView();
        FrameLayout frameLayout = (FrameLayout) mDecorView.findViewById(android.R.id.content);
        this.mChildView = frameLayout.getChildAt(0);
        this.mContentView = mChildView != null ? mChildView : frameLayout;

        this.paddingLeft = mContentView.getPaddingLeft();
        this.paddingTop = mContentView.getPaddingTop();
        this.paddingRight = mContentView.getPaddingRight();
        this.paddingBottom = mContentView.getPaddingBottom();

        BarConfig barConfig = new BarConfig(mActivity);
        this.statusBarHeight = barConfig.getStatusBarHeight();
        this.navigationBarHeight = barConfig.getNavigationBarHeight();
        this.actionBarHeight = barConfig.getActionBarHeight();
        navigationAtBottom = barConfig.isNavigationAtBottom();

    }

    public static KeyboardPatch patch(Activity activity) {
        return new KeyboardPatch(activity);
    }

    public static KeyboardPatch patch(Activity activity, View contentView) {
        return new KeyboardPatch(activity, contentView);
    }

    public static KeyboardPatch patch(Activity activity, Dialog dialog, String tag) {
        return new KeyboardPatch(activity, dialog, tag);
    }

    public static KeyboardPatch patch(Activity activity, Dialog dialog, String tag, View contentView) {
        return new KeyboardPatch(activity, dialog, tag, contentView);
    }

    protected static KeyboardPatch patch(Activity activity, Window window) {
        return new KeyboardPatch(activity, window);
    }

    protected void setBarParams(BarParams barParams) {
        this.mBarParams = barParams;
    }

    /**
     * 监听layout变化
     */
    public void enable() {
        enable(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void enable(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWindow.setSoftInputMode(mode);
            mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);//当在一个视图树中全局布局发生改变或者视图树中的某个视图的可视状态发生改变时，所要调用的回调函数的接口类
        }
    }

    /**
     * 取消监听
     */
    public void disable() {
        disable(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void disable(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWindow.setSoftInputMode(mode);
            mDecorView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            //如果布局根节点使用了android:fitsSystemWindows="true"属性或者导航栏不在底部，无需处理
            if (!navigationAtBottom)
                return;
            Rect r = new Rect();
            mDecorView.getWindowVisibleDisplayFrame(r); //获取当前窗口可视区域大小
            int diff;
            int keyboardHeight;
            boolean isPopup = false;
            if (mBarParams.systemWindows) {
                keyboardHeight = mContentView.getHeight() - r.bottom - navigationBarHeight;
                if (mBarParams.onKeyboardListener != null) {
                    if (keyboardHeight > navigationBarHeight)
                        isPopup = true;
                    mBarParams.onKeyboardListener.onKeyboardChange(isPopup, keyboardHeight);
                }
                return;
            }
            if (mChildView != null) {
                if (mBarParams.isSupportActionBar)
                    diff = mContentView.getHeight() + statusBarHeight + actionBarHeight - r.bottom;
                else if (mBarParams.fits)
                    diff = mContentView.getHeight() + statusBarHeight - r.bottom;
                else
                    diff = mContentView.getHeight() - r.bottom;
                if (mBarParams.fullScreen)
                    keyboardHeight = diff - navigationBarHeight;
                else
                    keyboardHeight = diff;
                if (mBarParams.fullScreen && diff == navigationBarHeight) {
                    diff -= navigationBarHeight;
                }
                if (keyboardHeight != keyboardHeightPrevious) {
                    mContentView.setPadding(paddingLeft, paddingTop, paddingRight, diff + paddingBottom);
                    keyboardHeightPrevious = keyboardHeight;
                    if (mBarParams.onKeyboardListener != null) {
                        if (keyboardHeight > navigationBarHeight)
                            isPopup = true;
                        mBarParams.onKeyboardListener.onKeyboardChange(isPopup, keyboardHeight);
                    }
                }
            } else {
                diff = mContentView.getHeight() - r.bottom;

                if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                        keyboardHeight = diff - navigationBarHeight;
                    } else {
                        if (!mBarParams.fullScreen)
                            keyboardHeight = diff;
                        else
                            keyboardHeight = diff - navigationBarHeight;
                    }
                    if (mBarParams.fullScreen && diff == navigationBarHeight)
                        diff -= navigationBarHeight;
                } else
                    keyboardHeight = diff;
                if (keyboardHeight != keyboardHeightPrevious) {
                    if (mBarParams.isSupportActionBar) {
                        mContentView.setPadding(0, statusBarHeight + actionBarHeight, 0, diff);
                    } else if (mBarParams.fits) {
                        mContentView.setPadding(0, statusBarHeight, 0, diff);
                    } else
                        mContentView.setPadding(0, 0, 0, diff);
                    keyboardHeightPrevious = keyboardHeight;
                    if (mBarParams.onKeyboardListener != null) {
                        if (keyboardHeight > navigationBarHeight)
                            isPopup = true;
                        mBarParams.onKeyboardListener.onKeyboardChange(isPopup, keyboardHeight);
                    }
                }
            }
        }
    };

}

class BarConfig {
    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape";
    private static final String NAV_BAR_WIDTH_RES_NAME = "navigation_bar_width";
    private static final String MIUI_FORCE_FSG_NAV_BAR = "force_fsg_nav_bar";

    private final int mStatusBarHeight;
    private final int mActionBarHeight;
    private final boolean mHasNavigationBar;
    private final int mNavigationBarHeight;
    private final int mNavigationBarWidth;
    private final boolean mInPortrait;
    private final float mSmallestWidthDp;



    BarConfig(Activity activity) {
        Resources res = activity.getResources();
        mInPortrait = (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        mSmallestWidthDp = getSmallestWidthDp(activity);
        mStatusBarHeight = getInternalDimensionSize(STATUS_BAR_HEIGHT_RES_NAME);
        mActionBarHeight = getActionBarHeight(activity);
        mNavigationBarHeight = getNavigationBarHeight(activity);
        mNavigationBarWidth = getNavigationBarWidth(activity);
        mHasNavigationBar = (mNavigationBarHeight > 0);
    }

    private int getActionBarHeight(Context context) {
        int result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TypedValue tv = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            result = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return result;
    }

    private int getNavigationBarHeight(Context context) {
        int result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (hasNavBar((Activity) context)) {
                String key;
                if (mInPortrait) {
                    key = NAV_BAR_HEIGHT_RES_NAME;
                } else {
                    key = NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME;
                }
                return getInternalDimensionSize(key);
            }
        }
        return result;
    }

    @TargetApi(14)
    private int getNavigationBarWidth(Context context) {
        int result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (hasNavBar((Activity) context)) {
                return getInternalDimensionSize(NAV_BAR_WIDTH_RES_NAME);
            }
        }
        return result;
    }

    private boolean hasNavBar(Activity activity) {
        //判断小米手机是否开启了全面屏,开启了，直接返回false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (Settings.Global.getInt(activity.getContentResolver(), MIUI_FORCE_FSG_NAV_BAR, 0) != 0) {
                return false;
            }
        }
        //其他手机根据屏幕真实高度与显示高度是否相同来判断
        WindowManager windowManager = activity.getWindowManager();
        Display d = windowManager.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            d.getRealMetrics(realDisplayMetrics);
        }

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }

    private int getInternalDimensionSize(String key) {
        int result = 0;
        try {
            int resourceId = Resources.getSystem().getIdentifier(key, "dimen", "android");
            if (resourceId > 0) {
                result = Resources.getSystem().getDimensionPixelSize(resourceId);
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    private float getSmallestWidthDp(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float widthDp = metrics.widthPixels / metrics.density;
        float heightDp = metrics.heightPixels / metrics.density;
        return Math.min(widthDp, heightDp);
    }

    /**
     * Should a navigation bar appear at the bottom of the screen in the current
     * device configuration? A navigation bar may appear on the right side of
     * the screen in certain configurations.
     *
     * @return True if navigation should appear at the bottom of the screen, False otherwise.
     */
    boolean isNavigationAtBottom() {
        return (mSmallestWidthDp >= 600 || mInPortrait);
    }

    /**
     * Get the height of the system status bar.
     *
     * @return The height of the status bar (in pixels).
     */
    int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    /**
     * Get the height of the action bar.
     *
     * @return The height of the action bar (in pixels).
     */
    int getActionBarHeight() {
        return mActionBarHeight;
    }

    /**
     * Does this device have a system navigation bar?
     *
     * @return True if this device uses soft key navigation, False otherwise.
     */
    boolean hasNavigationBar() {
        return mHasNavigationBar;
    }

    /**
     * Get the height of the system navigation bar.
     *
     * @return The height of the navigation bar (in pixels). If the device does not have
     * soft navigation keys, this will always return 0.
     */
    int getNavigationBarHeight() {
        return mNavigationBarHeight;
    }

    /**
     * Get the width of the system navigation bar when it is placed vertically on the screen.
     *
     * @return The width of the navigation bar (in pixels). If the device does not have
     * soft navigation keys, this will always return 0.
     */
    int getNavigationBarWidth() {
        return mNavigationBarWidth;
    }
}

/**
* 手机系统判断
*/
class BarHelperUtils {

    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_EMUI_VERSION_NAME = "ro.build.version.emui";
    private static final String KEY_DISPLAY = "ro.build.display.id";

    /**
     * 判断是否为miui
     * Is miui boolean.
     *
     * @return the boolean
     */
    public static boolean isMIUI() {
        String property = getSystemProperty(KEY_MIUI_VERSION_NAME, "");
        return !TextUtils.isEmpty(property);
    }

    /**
     * 判断miui版本是否大于等于6
     * Is miui 6 later boolean.
     *
     * @return the boolean
     */
    public static boolean isMIUI6Later() {
        String version = getMIUIVersion();
        int num;
        if ((!version.isEmpty())) {
            try {
                num = Integer.valueOf(version.substring(1));
                return num >= 6;
            } catch (NumberFormatException e) {
                return false;
            }
        } else
            return false;
    }

    /**
     * 获得miui的版本
     * Gets miui version.
     *
     * @return the miui version
     */
    public static String getMIUIVersion() {
        return isMIUI() ? getSystemProperty(KEY_MIUI_VERSION_NAME, "") : "";
    }

    /**
     * 判断是否为emui
     * Is emui boolean.
     *
     * @return the boolean
     */
    public static boolean isEMUI() {
        String property = getSystemProperty(KEY_EMUI_VERSION_NAME, "");
        return !TextUtils.isEmpty(property);
    }

    /**
     * 得到emui的版本
     * Gets emui version.
     *
     * @return the emui version
     */
    public static String getEMUIVersion() {
        return isEMUI() ? getSystemProperty(KEY_EMUI_VERSION_NAME, "") : "";
    }

    /**
     * 判断是否为emui3.1版本
     * Is emui 3 1 boolean.
     *
     * @return the boolean
     */
    public static boolean isEMUI3_1() {
        String property = getEMUIVersion();
        if ("EmotionUI 3".equals(property) || property.contains("EmotionUI_3.1")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为emui3.0版本
     * Is emui 3 1 boolean.
     *
     * @return the boolean
     */
    public static boolean isEMUI3_0() {
        String property = getEMUIVersion();
        if (property.contains("EmotionUI_3.0")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为flymeOS
     * Is flyme os boolean.
     *
     * @return the boolean
     */
    public static boolean isFlymeOS() {
        return getFlymeOSFlag().toLowerCase().contains("flyme");
    }

    /**
     * 判断flymeOS的版本是否大于等于4
     * Is flyme os 4 later boolean.
     *
     * @return the boolean
     */
    public static boolean isFlymeOS4Later() {
        String version = getFlymeOSVersion();
        int num;
        if (!version.isEmpty()) {
            try {
                if (version.toLowerCase().contains("os")) {
                    num = Integer.valueOf(version.substring(9, 10));
                } else {
                    num = Integer.valueOf(version.substring(6, 7));
                }
                return num >= 4;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 判断flymeOS的版本是否等于5
     * Is flyme os 5 boolean.
     *
     * @return the boolean
     */
    public static boolean isFlymeOS5() {
        String version = getFlymeOSVersion();
        int num;
        if (!version.isEmpty()) {
            try {
                if (version.toLowerCase().contains("os")) {
                    num = Integer.valueOf(version.substring(9, 10));
                } else {
                    num = Integer.valueOf(version.substring(6, 7));
                }
                return num == 5;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }


    /**
     * 得到flymeOS的版本
     * Gets flyme os version.
     *
     * @return the flyme os version
     */
    public static String getFlymeOSVersion() {
        return isFlymeOS() ? getSystemProperty(KEY_DISPLAY, "") : "";
    }

    private static String getFlymeOSFlag() {
        return getSystemProperty(KEY_DISPLAY, "");
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("get", String.class, String.class);
            return (String) method.invoke(clz, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }
}

/**
 * Flyme OS 修改状态栏字体颜色工具类
 */
class FlymeOSStatusBarFontUtils {
    private static Method mSetStatusBarColorIcon;
    private static Method mSetStatusBarDarkIcon;
    private static Field mStatusBarColorFiled;
    private static int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 0;

    static {
        try {
            mSetStatusBarColorIcon = Activity.class.getMethod("setStatusBarDarkIcon", int.class);
        } catch (NoSuchMethodException ignored) {

        }
        try {
            mSetStatusBarDarkIcon = Activity.class.getMethod("setStatusBarDarkIcon", boolean.class);
        } catch (NoSuchMethodException ignored) {

        }
        try {
            mStatusBarColorFiled = WindowManager.LayoutParams.class.getField("statusBarColor");
        } catch (NoSuchFieldException ignored) {

        }
        try {
            Field field = View.class.getField("SYSTEM_UI_FLAG_LIGHT_STATUS_BAR");
            SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = field.getInt(null);
        } catch (NoSuchFieldException ignored) {

        } catch (IllegalAccessException ignored) {

        }
    }

    /**
     * 判断颜色是否偏黑色
     *
     * @param color 颜色
     * @param level 级别
     * @return boolean boolean
     */
    public static boolean isBlackColor(int color, int level) {
        int grey = toGrey(color);
        return grey < level;
    }

    /**
     * 颜色转换成灰度值
     *
     * @param rgb 颜色
     * @return the int
     * @return　灰度值
     */
    public static int toGrey(int rgb) {
        int blue = rgb & 0x000000FF;
        int green = (rgb & 0x0000FF00) >> 8;
        int red = (rgb & 0x00FF0000) >> 16;
        return (red * 38 + green * 75 + blue * 15) >> 7;
    }

    /**
     * 设置状态栏字体图标颜色
     *
     * @param activity 当前activity
     * @param color    颜色
     */
    public static void setStatusBarDarkIcon(Activity activity, int color) {
        if (mSetStatusBarColorIcon != null) {
            try {
                mSetStatusBarColorIcon.invoke(activity, color);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            boolean whiteColor = isBlackColor(color, 50);
            if (mStatusBarColorFiled != null) {
                setStatusBarDarkIcon(activity, whiteColor, whiteColor);
                setStatusBarDarkIcon(activity.getWindow(), color);
            } else {
                setStatusBarDarkIcon(activity, whiteColor);
            }
        }
    }

    /**
     * 设置状态栏字体图标颜色(只限全屏非activity情况)
     *
     * @param window 当前窗口
     * @param color  颜色
     */
    public static void setStatusBarDarkIcon(Window window, int color) {
        try {
            setStatusBarColor(window, color);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                setStatusBarDarkIcon(window.getDecorView(), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置状态栏字体图标颜色
     *
     * @param activity 当前activity
     * @param dark     是否深色 true为深色 false 为白色
     */
    public static void setStatusBarDarkIcon(Activity activity, boolean dark) {
        setStatusBarDarkIcon(activity, dark, true);
    }

    private static boolean changeMeizuFlag(WindowManager.LayoutParams winParams, String flagName, boolean on) {
        try {
            Field f = winParams.getClass().getDeclaredField(flagName);
            f.setAccessible(true);
            int bits = f.getInt(winParams);
            Field f2 = winParams.getClass().getDeclaredField("meizuFlags");
            f2.setAccessible(true);
            int meizuFlags = f2.getInt(winParams);
            int oldFlags = meizuFlags;
            if (on) {
                meizuFlags |= bits;
            } else {
                meizuFlags &= ~bits;
            }
            if (oldFlags != meizuFlags) {
                f2.setInt(winParams, meizuFlags);
                return true;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置状态栏颜色
     *
     * @param view
     * @param dark
     */
    private static void setStatusBarDarkIcon(View view, boolean dark) {
        int oldVis = view.getSystemUiVisibility();
        int newVis = oldVis;
        if (dark) {
            newVis |= SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            newVis &= ~SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (newVis != oldVis) {
            view.setSystemUiVisibility(newVis);
        }
    }

    /**
     * 设置状态栏颜色
     *
     * @param window
     * @param color
     */
    private static void setStatusBarColor(Window window, int color) {
        WindowManager.LayoutParams winParams = window.getAttributes();
        if (mStatusBarColorFiled != null) {
            try {
                int oldColor = mStatusBarColorFiled.getInt(winParams);
                if (oldColor != color) {
                    mStatusBarColorFiled.set(winParams, color);
                    window.setAttributes(winParams);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置状态栏字体图标颜色(只限全屏非activity情况)
     *
     * @param window 当前窗口
     * @param dark   是否深色 true为深色 false 为白色
     */
    public static void setStatusBarDarkIcon(Window window, boolean dark) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            changeMeizuFlag(window.getAttributes(), "MEIZU_FLAG_DARK_STATUS_BAR_ICON", dark);
        } else {
            View decorView = window.getDecorView();
            setStatusBarDarkIcon(decorView, dark);
            setStatusBarColor(window, 0);
        }
    }

    private static void setStatusBarDarkIcon(Activity activity, boolean dark, boolean flag) {
        if (mSetStatusBarDarkIcon != null) {
            try {
                mSetStatusBarDarkIcon.invoke(activity, dark);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            if (flag) {
                setStatusBarDarkIcon(activity.getWindow(), dark);
            }
        }
    }
}
