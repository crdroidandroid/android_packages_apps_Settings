package com.android.settings.dashboard;

import android.content.Context;
import android.os.UserManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.pm.UserInfo;

import com.android.settings.R;
import com.android.settingslib.Utils;

public class DashboardProfileIcon extends RelativeLayout {

    public DashboardProfileIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
	
	public DashboardProfileIcon(Context context) {
        super(context);
		init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_dashboard_profile, this);
        TextView name = view.findViewById(R.id.user_name);
        ImageView icon = view.findViewById(R.id.user_icon);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserInfo info = com.android.settings.Utils.getExistingUser(userManager, android.os.Process.myUserHandle());
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);
        name.setText(info.name);
        icon.setImageDrawable(Utils.getUserIcon(context, userManager, info));
    }
}
