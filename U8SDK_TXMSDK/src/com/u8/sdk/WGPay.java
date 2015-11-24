package com.u8.sdk;

import android.app.Activity;

public class WGPay implements IPay {

	public WGPay(Activity context){
		
	}
	
	@Override
	public boolean isSupportMethod(String methodName) {
		return true;
	}

	@Override
	public void pay(PayParams data) {
		WGSDK.getInstance().pay(data);
	}

}
