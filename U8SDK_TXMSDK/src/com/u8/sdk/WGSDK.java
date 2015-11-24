package com.u8.sdk;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.msdk.api.LoginRet;
import com.tencent.msdk.api.MsdkBaseInfo;
import com.tencent.msdk.api.TokenRet;
import com.tencent.msdk.api.WGPlatform;
import com.tencent.msdk.api.WGPlatformObserver;
import com.tencent.msdk.api.WGQZonePermissions;
import com.tencent.msdk.api.WakeupRet;
import com.tencent.msdk.consts.CallbackFlag;
import com.tencent.msdk.consts.EPlatform;
import com.tencent.msdk.consts.TokenType;
import com.tencent.msdk.remote.api.RelationRet;
import com.tencent.unipay.plugsdk.IUnipayServiceCallBackPro;
import com.tencent.unipay.plugsdk.UnipayPlugAPI;
import com.tencent.unipay.plugsdk.UnipayPlugTools;
import com.tencent.unipay.plugsdk.UnipayResponse;
import com.tencent.unipay.request.UnipayGameRequest;
import com.u8.sdk.utils.ResourceHelper;
import com.u8.sdk.utils.U8HttpUtils;

public class WGSDK {
	public static final int LOGIN_TYPE_QQ = 1;		//QQ登录类型
	public static final int LOGIN_TYPE_WX = 2;		//微信登录类型
	
	private static WGSDK instance;
	
	private ProgressDialog loadingActivity = null;
	
	private String qqAppId;
	private String qqAppKey;
	private String wxAppId;
	private String msdkKey;
	private String offerId;
	private String coinIconName;
	private String env;
	private boolean debugMode;
	private boolean fixedPay;
	private boolean multiServers;
	
	private String payUrl;
	
	//临时存储的数据
    String openId;
    String pf;
    String pfKey;
    String payToken = "";
    String accessToken = "";
	
	private WGSDK(){
		
	}
	
	public static WGSDK getInstance(){
		if(instance == null){
			instance = new WGSDK();
		}
		return instance;
	}
	
	public void initSDK(SDKParams params){
		this.parseSDKParams(params);
		this.initSDK();
	}
	
	private void parseSDKParams(SDKParams params){
		this.qqAppId = params.getString("WG_QQAPPID");
		this.qqAppKey = params.getString("WG_QQAPPKEY");
		this.wxAppId = params.getString("WG_WXAPPID");
		this.msdkKey = params.getString("WG_MSDKKEY");
		this.offerId = params.getString("WG_OFFERID");
		this.env = params.getString("WG_ENV");
		this.debugMode = params.getBoolean("WG_DEBUG_MODE");
		this.fixedPay = params.getBoolean("WG_FIXEDPAY");
		this.coinIconName = params.getString("WG_COIN_ICON_NAME");
		this.multiServers = params.getBoolean("WG_MULTI_SERVERS");
		this.payUrl = params.getString("WG_PAY_URL");
	}
	
	private void initSDK(){
		try{
			
			U8SDK.getInstance().setActivityCallback(new ActivityCallbackAdapter(){
				
				public void onResume() {
					WGPlatform.onResume();
				}

				@Override
				public void onRestart() {
					WGPlatform.onRestart();
				}

				public void onPause() {
					WGPlatform.onPause();
				}

				@Override
				public void onStop() {
					WGPlatform.onStop();
				}

				public void onDestroy() {
					WGPlatform.onDestory(U8SDK.getInstance().getContext());
				}

				public void onNewIntent(Intent intent) {
					WGPlatform.handleCallback(intent);
				}
				
			});
			
			MsdkBaseInfo baseInfo = new MsdkBaseInfo();
			baseInfo.qqAppId = this.qqAppId;
			baseInfo.qqAppKey = this.qqAppKey;
			baseInfo.wxAppId = this.wxAppId;
			baseInfo.msdkKey = this.msdkKey;
			baseInfo.offerId = this.offerId;
			// TODO GAME 自2.7.1a开始游戏可在初始化msdk时动态设置版本号，灯塔和bugly的版本号由msdk统一设置
			// 1、版本号组成 = versionName + versionCode
			// 2、游戏如果不赋值给appVersionName（或者填为""）和appVersionCode(或者填为-1)，
			// msdk默认读取AndroidManifest.xml中android:versionCode="51"及android:versionName="2.7.1"
			// 3、游戏如果在此传入了appVersionName（非空）和appVersionCode（正整数）如下，则灯塔和bugly上获取的版本号为2.7.1.271
			//baseInfo.appVersionName = "2.8.0";
			//baseInfo.appVersionCode = 280;
			
			WGPlatform.Initialized(U8SDK.getInstance().getContext(), baseInfo);
			WGPlatform.WGSetPermission(WGQZonePermissions.eOPEN_ALL);
			WGPlatform.WGSetObserver(new MsdkCallback());
			WGPlatform.handleCallback(U8SDK.getInstance().getContext().getIntent());
			
			U8SDK.getInstance().onResult(U8Code.CODE_INIT_SUCCESS, "init success");
			
		}catch(Exception e){
			U8SDK.getInstance().onResult(U8Code.CODE_INIT_FAIL, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void login(){
		if(!SDKTools.isNetworkAvailable(U8SDK.getInstance().getContext())){
			U8SDK.getInstance().onResult(U8Code.CODE_NO_NETWORK, "The network now is unavailable");
			return;
		}
		
		Intent intent = new Intent(U8SDK.getInstance().getContext(), ChooseLoginTypeActivity.class);
		U8SDK.getInstance().getContext().startActivity(intent);
	}
	
	public void login(int loginType){
		logout();
		
		EPlatform platform = getPlatform();
		
		switch(loginType){
		case LOGIN_TYPE_QQ:
			switch(platform){
			case ePlatform_QQ:
				WGPlatform.WGLoginWithLocalInfo();
				break;
			case ePlatform_None:
				WGPlatform.WGLogin(EPlatform.ePlatform_QQ);
				break;
			default:
				U8SDK.getInstance().onResult(U8Code.CODE_LOGIN_FAIL, "请重新点击QQ登录");
			}
				
			break;
		case LOGIN_TYPE_WX:
			switch(platform){
			case ePlatform_Weixin:
				WGPlatform.WGLoginWithLocalInfo();
				break;
			case ePlatform_None:
				WGPlatform.WGLogin(EPlatform.ePlatform_Weixin);
				break;
			default:
				U8SDK.getInstance().onResult(U8Code.CODE_LOGIN_FAIL, "请重新点击微信登录");
			}
			break;
		}
	}
	
	public void logout(){
		WGPlatform.WGLogout();
	}
	
	// 提交扩展数据
	public void submitExtendData(UserExtraData extraData) {
		switch(extraData.getDataType()){
			case UserExtraData.TYPE_ENTER_GAME:
				//为了防止丢单，每次进入游戏的时候，都重新查询余额，并扣费
				//此时不用传订单号
				PayReqTask payReqTask = new PayReqTask(false);
				payReqTask.execute("2", "", (multiServers ? extraData.getServerID()+"":"1"));
			break;
		}
	}
	
	public void pay(final PayParams data){
		
		UnipayPlugAPI  unipayAPI = new UnipayPlugAPI(U8SDK.getInstance().getContext());
		unipayAPI.setEnv(this.env);
        unipayAPI.setLogEnable(true);
        if(this.debugMode){
	        UnipayPlugTools unipayPlugTools = new UnipayPlugTools(U8SDK.getInstance().getContext());
			unipayPlugTools.setUrlForTest();
        }
		
		
		UnipayGameRequest request = new UnipayGameRequest();
		
		if(this.multiServers){
			//如果在腾讯后台配置了分区，那么游戏层穿过来的服务器ID，必须和后台配置的一致
			request.zoneId = data.getServerId();
		}else{
			request.zoneId = "1";
		}
		
		request.offerId = this.offerId;
		request.openId = openId;
		request.pf = pf;
		request.pfKey = pfKey;
		
		EPlatform platform = getPlatform();
		
		if(platform == EPlatform.ePlatform_QQ){
			request.openKey = payToken;
			request.sessionId = "openid";
        	request.sessionType = "kp_actoken";
        }
        else if(platform == EPlatform.ePlatform_Weixin){ 
        	request.openKey = accessToken;
        	request.sessionId = "hy_gameid";
        	request.sessionType = "wc_actoken";
        }

		request.acctType = UnipayPlugAPI.ACCOUNT_TYPE_COMMON;
		request.saveValue = data.getPrice() * 10+"";
		request.isCanChange = !this.fixedPay;
		request.resId = ResourceHelper.getIdentifier(U8SDK.getInstance().getContext(), "R.drawable."+this.coinIconName);
		
		Bitmap bmp = BitmapFactory.decodeResource(U8SDK.getInstance().getContext().getResources(), request.resId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		request.resData = baos.toByteArray();

		Log.d("U8SDK", "TMXSDK Pay:::::"+"offerID=" + request.offerId +",openId="+request.openId+",openKey="+request.openKey+",sessionId="+request.sessionId+",sessionType=" + request.sessionType
				+",zoneId="+request.zoneId + ",pf=" + request.pf + ",pfKey=" + request.pfKey + ",acctType="+ request.acctType+",saveValue="+request.saveValue
				+ ",isCanChange=" + request.isCanChange+",resId="+request.resId);
		
		unipayAPI.LaunchPay(request, new IUnipayServiceCallBackPro.Stub() {
			
			@Override
			public void UnipayNeedLogin() throws RemoteException
			{
				showTip("登录失效，请重新登录再充值");
				U8SDK.getInstance().onLogout();
			}

			@Override
			public void UnipayCallBack(UnipayResponse res) throws RemoteException {
				
				Log.e("U8SDK" , "UnipayPlugAPI:::::"+"UnipayCallBack \n" + 
						"\nresultCode = " + res.resultCode + 
						"\npayChannel = "+ res.payChannel + 
						"\npayState = "+ res.payState + 
						"\nproviderState = " + res.provideState + 
						"\nsaveType = "+ res.extendInfo);
				
				if(res.resultCode == UnipayResponse.PAYRESULT_SUCC){
					U8SDK.getInstance().onResult(U8Code.CODE_PAY_SUCCESS, "pay success.");
					
					//支付成功向U8Server发送通知，调用查询余额接口并扣费
					Log.e("U8SDK", "pay success. now to req charge to u8server");
					PayReqTask payReqTask = new PayReqTask(true);
					payReqTask.execute("1", data.getOrderID(), (multiServers ? data.getServerId() : "1"));
					
				}else{
					U8SDK.getInstance().onResult(U8Code.CODE_PAY_FAIL, "pay fail.");
				}
			}
		});		
	}
	
	private String reqCharge(int opType, String orderID, String serverID){
		
		if(this.payUrl == null || TextUtils.isEmpty(this.payUrl)){
			Log.d("U8SDK", "the pay url is null");
			return null;
		}
		
		try{
			
			EPlatform p = getPlatform();
			int accountType = 0;
			if(p == EPlatform.ePlatform_Weixin){
				accountType = 1;
			}
			
			Map<String, String> params = new HashMap<String, String>();
			params.put("opType", opType+"");		//opType:1；支付之后正常查询;2:登录查询掉单
			params.put("channelID", U8SDK.getInstance().getCurrChannel()+"");
			params.put("userID", U8SDK.getInstance().getUToken().getUserID()+"");
			params.put("accountType", accountType+"");
			params.put("openID", this.openId);
			params.put("openKey", this.accessToken);
			params.put("pay_token", accountType == 0 ? this.payToken : "");
			params.put("pf", this.pf);
			params.put("pfkey", this.pfKey);
			params.put("zoneid", serverID);
			
			String authResult = U8HttpUtils.httpGet(this.payUrl, params);
			
			Log.d("U8SDK", "the pay req to u8server response : "+ authResult);
			
			return authResult;
			
		}catch(Exception e){
			
			e.printStackTrace();
		}
		
		return "";
	}
	
    // 获取当前登录平台
    private EPlatform getPlatform() {
        LoginRet ret = new LoginRet();
        WGPlatform.WGGetLoginRecord(ret);
        if (ret.flag == CallbackFlag.eFlag_Succ) {
            return EPlatform.getEnum(ret.platform);
        }
        return EPlatform.ePlatform_None;
    }
	
    //拉去异常号
    private void showDiffLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(U8SDK.getInstance().getContext());
        builder.setTitle("异账号提示");
        builder.setMessage("你当前拉起的账号与你本地的账号不一致，请选择使用哪个账号登陆：");
        builder.setPositiveButton("本地账号",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        Toast.makeText(U8SDK.getInstance().getContext(), "选择使用本地账号", Toast.LENGTH_LONG).show();
                        if(!WGPlatform.WGSwitchUser(false)){
                        	U8SDK.getInstance().onLogout();
                        }
                    }
                });
        builder.setNeutralButton("拉起账号",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        Toast.makeText(U8SDK.getInstance().getContext(), "选择使用拉起账号", Toast.LENGTH_LONG).show();
                    	if(!WGPlatform.WGSwitchUser(true)){
                    		U8SDK.getInstance().onLogout();
                        }
                    }
                });
        builder.show();
    } 
    
    private String encodeLoginResult(int requesttype, String openId) {

		
		JSONObject ext = new JSONObject();
		try {
			ext.put("accountType", requesttype);
			ext.put("openId", openId);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return ext.toString();
	}
    
    private void showTip(final String tip){
    	U8SDK.getInstance().runOnMainThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(U8SDK.getInstance().getContext(), tip, Toast.LENGTH_SHORT).show();
				
			}
		});
    }
    
	class PayReqTask extends AsyncTask<String, Void, String>{
		
		private boolean showTip = false;
		
		public PayReqTask(boolean showTip){
			this.showTip = showTip;
		}
		
		protected void onPreExecute(){
			if(showTip){
				U8SDK.getInstance().runOnMainThread(new Runnable() {
					
					@Override
					public void run() {
						showProgressDialog("正在处理,请稍候...");
						
					}
				});
				
			}
				
		}
		
		@Override
		protected String doInBackground(String... args) {
			
			String opType = args[0];
			String orderID = args[1];
			String zoneID = args[2];
			
			
			return reqCharge(Integer.valueOf(opType), orderID, zoneID);
		}
		
		protected void onPostExecute(final String result){
			
			if(showTip){
				U8SDK.getInstance().runOnMainThread(new Runnable() {
					
					@Override
					public void run() {

						hideProgressDialog();
						if("1".equals(result)){
							U8SDK.getInstance().onResult(U8Code.CODE_PAY_SUCCESS, "pay success");
							showTip("支付成功,到账可能稍有延迟");
						}else{
							U8SDK.getInstance().onResult(U8Code.CODE_PAY_FAIL, "pay fail");
							showTip("支付失败,重新登录后,会重新查询并支付");
						}		
					}
				});
			}
		}
		
	}
    
	//SDK回调
	class MsdkCallback implements WGPlatformObserver{


	    long payTokenExp = 0;
		
	    private void letUserLogin(){
	    	
	    	
	    	long accessTokenExpire = 0;
	    	String refreshToken = "";
	    	long refreshTokenExpire = 0;
	    	
			LoginRet ret = new LoginRet();
	        WGPlatform.WGGetLoginRecord(ret);
	    	
	        int requesttype = -1; // 0:qq  1:微信
            openId = ret.open_id;
            pf = ret.pf;
            pfKey = ret.pf_key;
                        
            Log.d("U8SDK","openId = "+openId+",pf = "+pf+",pfKey = "+pfKey);
            EPlatform platform = getPlatform();
            if(getPlatform()== EPlatform.ePlatform_QQ){
            	requesttype = 0;
            	for (TokenRet tr : ret.token) {
            		switch (tr.type) {
            		case TokenType.eToken_QQ_Access:
            			accessToken = tr.value;
            			accessTokenExpire = tr.expiration;
            			break;
            		case TokenType.eToken_QQ_Pay:
            			payToken = tr.value;
            			payTokenExp = tr.expiration;
            			break;
            		default:
            			break;
            		}
            	}
            }
            else if(platform == EPlatform.ePlatform_Weixin){ 
            	requesttype = 1;
            	for (TokenRet tr : ret.token) {
            		switch (tr.type) {
            		case TokenType.eToken_WX_Access:
            			accessToken = tr.value;
            			accessTokenExpire = tr.expiration;
            			break;
            		case TokenType.eToken_WX_Refresh:
            			refreshToken = tr.value;
            			refreshTokenExpire = tr.expiration;
            			break;
            		default:
            			break;
            		}
            	}
            }
            Log.d("U8SDK", "payToken = "+payToken);
            Log.d("U8SDK", "payTokenExp"+payTokenExp);
            Log.d("U8SDK", "AccessToken = "+accessToken);
            Log.d("U8SDK", "AccessTokenExpire"+accessTokenExpire);
            Log.d("U8SDK", "RefreshToken"+refreshToken);
            Log.d("U8SDK", "RefreshTokenExpire = "+refreshTokenExpire);
            
            U8SDK.getInstance().onResult(U8Code.CODE_LOGIN_SUCCESS, accessToken);
            String result = encodeLoginResult(requesttype, openId);
			U8SDK.getInstance().onLoginResult(result);
	    }
	    
		@Override
		public String OnCrashExtMessageNotify() {
			return null;
		}

		@Override
		public void OnLoginNotify(LoginRet ret) {
	        
	        switch (ret.flag) {
	            case CallbackFlag.eFlag_Succ:
	                // 登陆成功, 读取各种票据
	            	letUserLogin();
	                break;
	            case CallbackFlag.eFlag_WX_RefreshTokenSucc:
	            	//开启票据自动刷新，刷新成功，不需要特殊处理
	            	break;

	            case CallbackFlag.eFlag_WX_UserCancel:
	            case CallbackFlag.eFlag_WX_NotInstall:
	            case CallbackFlag.eFlag_WX_NotSupportApi:
	            case CallbackFlag.eFlag_WX_LoginFail:
	            case CallbackFlag.eFlag_Local_Invalid:
	            	Log.e("U8SDK", "登录失败ret;;;;;flag=="+ret.flag);
	            	if(CallbackFlag.eFlag_WX_NotInstall == ret.flag)
	            		Toast.makeText(U8SDK.getInstance().getContext(), "尚未安装微信", Toast.LENGTH_LONG).show();;
	            	U8SDK.getInstance().onResult(U8Code.CODE_LOGIN_FAIL, ret.desc);
	            default:
	            	U8SDK.getInstance().onResult(U8Code.CODE_LOGIN_FAIL, ret.desc);
	                break;
	        }			
		}

		@Override
		public void OnRelationNotify(RelationRet arg0) {
			
		}

		@Override
		public void OnWakeupNotify(WakeupRet ret) {
			switch(ret.flag){
			case CallbackFlag.eFlag_Succ:
			case CallbackFlag.eFlag_AccountRefresh:
				letUserLogin();
				break;
			case CallbackFlag.eFlag_UrlLogin:
				// 用拉起的账号登录，登录结果在OnLoginNotify()中回调
				break;
			case CallbackFlag.eFlag_NeedSelectAccount:
				// 异账号时，游戏需要弹出提示框让用户选择需要登陆的账号
				showDiffLogin();
				break;
			case CallbackFlag.eFlag_NeedLogin:
				U8SDK.getInstance().onLogout();
				break;
				
			default:
				U8SDK.getInstance().onLogout();
			}			
		}
		
	}
	
	private void showProgressDialog(String msg){
		if(loadingActivity != null){
			return;
		}
		
        loadingActivity = new ProgressDialog(U8SDK.getInstance().getContext());
        loadingActivity.setIndeterminate(true);
        loadingActivity.setCancelable(false);
        loadingActivity.setMessage(msg);
        loadingActivity.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {

			}
		});		
        loadingActivity.show();
	}
	
	private void hideProgressDialog(){
		if(loadingActivity == null){
			return;
		}
		loadingActivity.dismiss();
		loadingActivity = null;
	}
}
