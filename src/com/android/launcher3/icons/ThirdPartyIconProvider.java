package com.android.launcher3.icons;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.android.launcher3.icons.pack.IconResolver;
import com.android.launcher3.util.ComponentKey;

import com.android.launcher3.util.MainThreadInitializedObject;
import com.android.launcher3.util.SafeCloseable;

@SuppressWarnings("unused")
public class ThirdPartyIconProvider extends LauncherIconProvider implements SafeCloseable {
    public static final MainThreadInitializedObject<ThirdPartyIconProvider> INSTANCE =
            new MainThreadInitializedObject<>(ThirdPartyIconProvider::new);

    private final Context mContext;

    public ThirdPartyIconProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void close() { }

    @Override
    public String getSystemIconState() {
        return super.getSystemIconState() + ",icon-pack-artwork-v1";
    }

    @SuppressLint("WrongConstant")
    @Override
    public Drawable getIcon(ComponentInfo info, int iconDpi) {
        ComponentKey key = new ComponentKey(
                info.getComponentName(), UserHandle.getUserHandleForUid(info.applicationInfo.uid));

        IconResolver.DefaultDrawableProvider fallback =
                () -> super.getIcon(info, iconDpi);
        Drawable icon = ThirdPartyIconUtils.getByKey(mContext, key, iconDpi, fallback);

        if (icon == null) {
            return fallback.get();
        }
        return icon;
    }
}
