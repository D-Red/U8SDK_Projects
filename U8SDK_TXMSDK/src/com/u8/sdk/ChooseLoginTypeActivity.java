package com.u8.sdk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * 选择QQ、微信登录方式
 */
public class ChooseLoginTypeActivity extends Activity {
	
	private Button btnQQ;
	private Button btnWX;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int layoutID = getResources().getIdentifier("u8_layout_login_choice", "layout", getPackageName());
		setContentView(layoutID);
		btnQQ = (Button) findViewById(getResources().getIdentifier("btn_qq", "id", getPackageName()));
		btnWX = (Button) findViewById(getResources().getIdentifier("btn_wx", "id", getPackageName()));
		
		btnQQ.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				ChooseLoginTypeActivity.this.finish();
				WGSDK.getInstance().login(WGSDK.LOGIN_TYPE_QQ);
			}
		});
		btnWX.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ChooseLoginTypeActivity.this.finish();
				WGSDK.getInstance().login(WGSDK.LOGIN_TYPE_WX);
			}
		});
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
	}
}
