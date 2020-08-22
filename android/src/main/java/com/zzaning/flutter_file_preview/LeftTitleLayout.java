package com.zzaning.flutter_file_preview;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 左title
 */
public class LeftTitleLayout extends FrameLayout {

    private ImageView leftBack;
    private TextView leftTitleTv;
    private LinearLayout leftLl;
    private TextView middleTitleTv;
    private TextView rightTitleTv;
    private ImageView rightTitleIv;
    private RelativeLayout rightLl;
    private Context mContext;
    private boolean leftFinish = true;

    public LeftTitleLayout(Context context) {
        this(context, null, 0);
    }

    public LeftTitleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftTitleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView();
    }

    public void initView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.left_title_layout, null, false);
        addView(view);
        leftBack = findViewById(R.id.left_back);
        leftTitleTv = findViewById(R.id.left_title_tv);
        leftLl = findViewById(R.id.left_ll);
        middleTitleTv = findViewById(R.id.middle_title_tv);
        rightTitleTv = findViewById(R.id.right_title_tv);
        rightTitleIv = findViewById(R.id.right_title_iv);
        rightLl = findViewById(R.id.right_ll);
        leftLl.setOnClickListener(v -> {
            if (leftFinish) {
                ((Activity) mContext).finish();
            } else {
                if (specialLeftFinish != null) {
                    specialLeftFinish.specialLeftOption();
                }
            }
        });
    }

    private SpecialLeftOptionListener specialLeftFinish;

    public void setSpecialLeftFinish(SpecialLeftOptionListener specialLeftFinish) {
        this.leftFinish = false;
        this.specialLeftFinish = specialLeftFinish;
    }

    public interface SpecialLeftOptionListener {
        void specialLeftOption();
    }

    /**
     * 获取左边返回控件
     */
    public LinearLayout getLeftLlLayout() {
        return leftLl;
    }


    /**
     * 获取有右边textView
     */
    public TextView getRightTextView() {
        return rightTitleTv;
    }

    /**
     * 获取右边总布局，设置点击事件
     */
    public RelativeLayout getRightRl() {
        return rightLl;
    }

    /**
     * 获取中间textView
     */
    public TextView getTitleTextView() {
        return middleTitleTv;
    }


    public LeftTitleLayout setTitleTextColor(int color) {
        middleTitleTv.setTextColor(color);
        return this;
    }

    public LeftTitleLayout setRightTextColor(int color) {
        rightTitleTv.setTextColor(color);
        return this;
    }

    public LeftTitleLayout setRightText(String text) {
        rightTitleTv.setVisibility(VISIBLE);
        rightTitleTv.setText(text);
        return this;
    }

    public LeftTitleLayout setRightImage(int rightImg) {
        rightTitleIv.setVisibility(VISIBLE);
        rightTitleIv.setImageResource(rightImg);
        return this;
    }

    /**
     * 设置数据
     */
    public LeftTitleLayout item(int res, String title) {
        leftBack.setBackgroundResource(res);
        leftTitleTv.setText(title);
        return this;
    }

    /**
     * 设置是否按返回键关闭页面
     */
    public LeftTitleLayout leftFinish(boolean isFinish) {
        this.leftFinish = isFinish;
        return this;
    }

    /**
     * 设置中间标题
     */
    public LeftTitleLayout setTitle(String title) {
        middleTitleTv.setText(title);
        return this;
    }

    /**
     * 改变返回按钮的颜色
     */
    public LeftTitleLayout setLeftColorFilter(int color) {
        leftBack.setColorFilter(getResources().getColor(color));
        return this;
    }

}
