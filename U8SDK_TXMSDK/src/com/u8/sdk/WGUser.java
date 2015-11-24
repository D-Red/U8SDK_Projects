package com.u8.sdk;

import com.u8.sdk.utils.Arrays;

import android.app.Activity;

public class WGUser extends U8UserAdapter {

	private String[] supportedMethods = {"login","switchLogin","logout","submitExtraData"};
	
	public WGUser(Activity context){
		WGSDK.getInstance().initSDK(U8SDK.getInstance().getSDKParams());
	}
	
	@Override
	public void login() {
		WGSDK.getInstance().login();
	}
	
	@Override
	public void switchLogin() {
		WGSDK.getInstance().login();
	}
	
	@Override
	public void logout() {
		WGSDK.getInstance().logout();
	}

	@Override
	public void submitExtraData(UserExtraData extraData) {
		WGSDK.getInstance().submitExtendData(extraData);
	}
	
	@Override
	public boolean isSupportMethod(String methodName) {
		return Arrays.contain(supportedMethods, methodName);
	}

}
