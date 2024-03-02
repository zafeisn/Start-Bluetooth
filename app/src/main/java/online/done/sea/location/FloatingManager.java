package online.done.sea.location;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import online.done.sea.R;
import online.done.sea.util.MyHandler;

public class FloatingManager {

    private View mAnchorView;

    private String mTitle;

    private ViewGroup mRootView;

    ImageView imageView;

    public static Builder getBuilder() {
        return new Builder();
    }

    static class Builder {
        private FloatingManager mManager;

        public FloatingManager build() {
            return mManager;
        }


        public Builder() {
            mManager = new FloatingManager();
        }

        public Builder setAnchorView(View view) {
            mManager.setAnchorView(view);
            return this;
        }

        public Builder setTitle(String title) {
            mManager.setTitle(title);
            return this;
        }

    }

    public void setAnchorView(View view) {
        mAnchorView = view;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showCenterView(float[] location, MyHandler myHandler) {
        if (mAnchorView == null) {
            return;
        }

        Activity activity = (Activity) mAnchorView.getContext();
        mRootView = activity.findViewById(android.R.id.content);

        Rect anchorRect = new Rect();
        Rect rootViewRect = new Rect();

        mAnchorView.getGlobalVisibleRect(anchorRect);
        mRootView.getGlobalVisibleRect(rootViewRect);

        // 创建 imageView
        imageView = new ImageView(activity);
        imageView.setImageDrawable(activity.getResources().getDrawable(R.mipmap.marker));

        // 判断是否重复添加
        if (mRootView.getChildCount() > 1 ){

            mRootView.removeViewAt(1);

        }

        mRootView.addView(imageView);

        // 调整显示区域大小
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) imageView.getLayoutParams();
        params.width = 100;
        params.height = 100;
        imageView.setLayoutParams(params);

        // 设置居中显示
        imageView.setX(location[0]);
        imageView.setY(location[1]);
        int[] ints = new int[2];
        imageView.getLocationOnScreen(ints);

        Message message = new Message();
        message.what = 2;
        myHandler.sendMessage(message);

    }
}
