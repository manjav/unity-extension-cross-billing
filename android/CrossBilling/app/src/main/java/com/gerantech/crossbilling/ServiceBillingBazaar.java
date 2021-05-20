/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.content.pm.PackageManager
 *  android.content.pm.PackageManager$NameNotFoundException
 *  android.os.Bundle
 *  android.util.Log
 *  com.unity3d.player.UnityPlayer
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.farsitel.bazaariab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.farsitel.bazaariab.util.IabHelper;
import com.farsitel.bazaariab.util.IabResult;
import com.farsitel.bazaariab.util.Inventory;
import com.farsitel.bazaariab.util.Purchase;
import com.farsitel.bazaariab.util.SkuDetails;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServiceBillingBazaar {
    private IabHelper mHelper;
    private static ServiceBillingBazaar instance;
    private static String PUBLIC_KEY;
    private static final int RC_REQUEST = 10001;
    private static final String _SKU = "ID#sku";
    private static final String _PAYLOAD = "ID#payload";
    private static final String _CONSUMABLE = "ID#consumable";
    private static final String _OPERATION_TYPE = "ID#operationType";
    private static final String _ITEM_TYPE = "ID#itemType";
    private static final String _ORIGINAL_JSON = "ID#originalJson";
    private static final String _SIGNATURE = "ID#signature";
    private static final String TAG = "BazaarIabService";
    private static final int PURCHASE = 1;
    private static final int CHECK_INVENTORY = 2;
    private static final int CONSUME_PURCHASE = 3;
    private static final int PRODUCT_DETAILS = 4;
    private static final String PURCHASE_RESULT_FUNCTION = "JNI_PurchaseResult";
    private static final String INVENTORY_RESULT_FUNCTION = "JNI_InventoryResult";
    private static final String CONSUME_RESULT_FUNCTION = "JNI_ConsumeResult";
    private static final String INITIALIZE_RESULT_FUNCTION = "JNI_InitializeResult";
    private static final String DETAILS_RESULT_FUNCTION = "JNI_ProductsDetailsResult";
    private static final String MARKET_APP_PACKAGE = "com.farsitel.bazaar";
    private static final int ERROR_WRONG_SETTINGS = 1;
    private static final int ERROR_market_NOT_INSTALLED = 2;
    private static final int ERROR_SERVICE_NOT_INITIALIZED = 3;
    private static final int ERROR_INTERNAL = 4;
    private static final int ERROR_OPERATION_CANCELLED = 5;
    private static final int ERROR_CONSUME_PURCHASE = 6;
    private static final int ERROR_NOT_LOGGED_IN = 7;
    private static final int ERROR_HAS_NOT_PRODUCT_IN_INVENTORY = 8;
    private static final int ERROR_WRONG_PRODUCT_ID = 13;
    private static String GameObjectName;

    protected static final String MANAGER_NAME = "BazaarPlugin.IABEventManager";
    private static Class<?> mUnityPlayerClass;
    private static Field mUnityPlayerActivityField;
    private static Method mUnitySendMessageMethod;

    private ServiceBillingBazaar() {
        instance = this;

        try
        {
            // Using reflection to remove reference to Unity library.
            mUnityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            mUnityPlayerActivityField = mUnityPlayerClass.getField("currentActivity");

            mUnitySendMessageMethod = mUnityPlayerClass.getMethod("UnitySendMessage", new Class[] { String.class, String.class, String.class });
        }
        catch (ClassNotFoundException e)
        {
            Log.i(TAG, "Could not find UnityPlayer class: " + e.getMessage());
        }
        catch (NoSuchFieldException e)
        {
            Log.i(TAG, "Could not find currentActivity field: " + e.getMessage());
        }
        catch (Exception e)
        {
            Log.i(TAG, "Unkown exception occurred locating UnitySendMessage(): " + e.getMessage());
        }
    }

    public static ServiceBillingBazaar GetInstance() {
        if (instance == null) {
            instance = new ServiceBillingBazaar();
        }
        return instance;
    }

    private static Activity GetCurrentActivity() {
        if (mUnityPlayerActivityField != null)
            try {
                Activity activity = (Activity)mUnityPlayerActivityField.get(mUnityPlayerClass);
                if (activity == null)
                    Log.e("[BazaarPlugin]", "The Unity Activity does not exist. This could be due to a low memory situation");
                return activity;
            } catch (Exception e) {
                Log.i("[BazaarPlugin]", "Error getting currentActivity: " + e.getMessage());
            }
        return null;
    }

    private static Context GetCurrentContext() {
        return GetCurrentActivity().getBaseContext();
    }

    protected static void UnitySendMessage(String methodName, String methodParam)
    {
        if (methodParam == null)
            methodParam = "";

        if (mUnitySendMessageMethod != null)
        {
            try
            {
                mUnitySendMessageMethod.invoke(null, new Object[]{ GameObjectName, methodName, methodParam });
            }
            catch (IllegalArgumentException e)
            {
                Log.i(TAG, "could not find UnitySendMessage method: " + e.getMessage());
            }
            catch (IllegalAccessException e)
            {
                Log.i(TAG, "could not find UnitySendMessage method: " + e.getMessage());
            }
            catch (InvocationTargetException e)
            {
                Log.i(TAG, "could not find UnitySendMessage method: " + e.getMessage());
            }
        }
        else
        {
            //Toast.makeText(getActivity(), "UnitySendMessage:\n" + methodName + "\n" + methodParam, Toast.LENGTH_LONG).show();
            Log.i(TAG, "UnitySendMessage: CafeBazaarIABManager, " + methodName + ", " + methodParam);
        }
    }


    public void SetPublicKey(String publicKey) {
        PUBLIC_KEY = publicKey;
    }

    public void SetCallbackGameObject(String gameObjectName) {
        GameObjectName = gameObjectName;
    }

    public void StartIabService(String bindURL, String packageURL) {
        if (PUBLIC_KEY == null) {
            String msg = "error!!! PublicKey base64Encoded is NULL";
            ServiceBillingBazaar.log("error!!! PublicKey base64Encoded is NULL");
            UnitySendMessage((String)INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(1, "error!!! PublicKey base64Encoded is NULL"));
            return;
        }
        if (this.IsPackageInstalled()) {
            this.StartIabHelper(bindURL, packageURL);
        } else {
            String msg = "market is not installed on the device.";
            ServiceBillingBazaar.log("market is not installed on the device.");
            UnitySendMessage((String)INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(2, "market is not installed on the device."));
        }
    }

    private synchronized void StartIabHelper(String bindURL, String packageURL) {
        String base64EncodedPublicKey = PUBLIC_KEY;
        try {
            this.mHelper = new IabHelper(ServiceBillingBazaar.GetCurrentContext(), bindURL, packageURL, base64EncodedPublicKey);
            this.mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener(){

                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        String msg = "(StartIabHelper) error setup market billing. " + result.getMessage();
                        ServiceBillingBazaar.log(msg);
                        UnitySendMessage((String) ServiceBillingBazaar.INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(3, msg));
                        return;
                    }
                    if (ServiceBillingBazaar.this.mHelper == null) {
                        String msg = "(StartIabHelper) error setup market billing. iabHelper is null.";
                        ServiceBillingBazaar.log("(StartIabHelper) error setup market billing. iabHelper is null.");
                        UnitySendMessage((String) ServiceBillingBazaar.INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(3, "(StartIabHelper) error setup market billing. iabHelper is null."));
                        return;
                    }
                    String msg = "(StartIabHelper) setup finished.";
                    ServiceBillingBazaar.log("(StartIabHelper) setup finished.");
                    UnitySendMessage((String) ServiceBillingBazaar.INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, "(StartIabHelper) setup finished."));
                }
            });
        }
        catch (Exception e) {
            String msg = "(StartIabHelper) error setup market billing. " + e.getMessage();
            ServiceBillingBazaar.log(msg);
            UnitySendMessage((String)INITIALIZE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(3, msg));
        }
    }

    private boolean IsPackageInstalled() {
        PackageManager pm = ServiceBillingBazaar.GetCurrentContext().getPackageManager();
        try {
            pm.getPackageInfo(MARKET_APP_PACKAGE, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void StopIabHelper() {
        if (this.mHelper != null) {
            try {
                this.mHelper.dispose();
                this.mHelper = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void Purchase(String sku, boolean consumable, String payload) {
        try {
            Intent intent = new Intent(ServiceBillingBazaar.GetCurrentContext(), IabActivity.class);
            intent.putExtra(_OPERATION_TYPE, 1);
            intent.putExtra(_SKU, sku);
            intent.putExtra(_PAYLOAD, payload);
            intent.putExtra(_CONSUMABLE, consumable);
            ServiceBillingBazaar.GetCurrentActivity().startActivity(intent);
        }
        catch (Exception e) {
            String msg = "(Purchase) Error purchasing item. " + e.getMessage();
            ServiceBillingBazaar.log(msg);
            UnitySendMessage((String)PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
        }
    }

    public void ConsumePurchase(String mItemType, String mOriginalJson, String mSignature) {
        try {
            Intent intent = new Intent(ServiceBillingBazaar.GetCurrentContext(), IabActivity.class);
            intent.putExtra(_OPERATION_TYPE, 3);
            intent.putExtra(_ITEM_TYPE, mItemType);
            intent.putExtra(_ORIGINAL_JSON, mOriginalJson);
            intent.putExtra(_SIGNATURE, mSignature);
            ServiceBillingBazaar.GetCurrentActivity().startActivity(intent);
        }
        catch (Exception e) {
            String msg = "(ConsumePurchase) Error consuming purchase. " + e.getMessage();
            ServiceBillingBazaar.log(msg);
            UnitySendMessage((String)CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(6, msg));
        }
    }

    public void CheckInventory(String sku) {
        try {
            Intent intent = new Intent(ServiceBillingBazaar.GetCurrentContext(), IabActivity.class);
            intent.putExtra(_OPERATION_TYPE, 2);
            intent.putExtra(_SKU, sku);
            ServiceBillingBazaar.GetCurrentActivity().startActivity(intent);
        }
        catch (Exception e) {
            String msg = "(CheckInventory) Error checking inventory. " + e.getMessage();
            ServiceBillingBazaar.log(msg);
            UnitySendMessage((String)INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
        }
    }

    public void GetProductsDetails(String products) {
        try {
            String[] mProducts = products.split(",");
            Intent intent = new Intent(ServiceBillingBazaar.GetCurrentContext(), IabActivity.class);
            intent.putExtra(_OPERATION_TYPE, 4);
            intent.putExtra(_SKU, mProducts);
            ServiceBillingBazaar.GetCurrentActivity().startActivity(intent);
        }
        catch (Exception e) {
            String msg = "(GetProductsDetails) Error getting products details. " + e.getMessage();
            ServiceBillingBazaar.log(msg);
            UnitySendMessage((String)DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
        }
    }

    private static void log(String msg) {
        //UnitySendMessage((String)"JNI_DebugLog", (String)msg);
        Log.d((String)TAG, (String)msg);
    }

    private static String returnResult(int errorCode, String data) {
        try {
            JSONObject json = new JSONObject();
            json.put("errorCode", errorCode);
            json.put("data", (Object)data);
            return json.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    static {
        PUBLIC_KEY = null;
        GameObjectName = "market.ir IAB";
    }

    public static class IabActivity
            extends Activity {
        private String mSku;
        private String mPayload;
        private String mItemType;
        private String mOriginalJson;
        private String mSignature;
        private List<String> mProducts = new ArrayList<String>();
        private boolean consume = false;
        private IabHelper.OnIabPurchaseFinishedListener iabPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener(){

            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                ServiceBillingBazaar.log("IabPurchase finished: " + result + ", purchase: " + info);
                if (result.getResponse() == 0) {
                    ServiceBillingBazaar.log("purchase ok");
                    if (!info.getSku().equals(IabActivity.this.mSku)) {
                        String msg = "purchase wrong product!!! purchased product: " + info.getSku();
                        ServiceBillingBazaar.log(msg);
                        UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                        return;
                    }
                    if (IabActivity.this.consume) {
                        ServiceBillingBazaar.GetInstance().mHelper.consumeAsync(info, IabActivity.this.mConsumeFinishedListener);
                    } else {
                        try {
                            String msg = info.getString();
                            ServiceBillingBazaar.log(msg);
                            UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, msg));
                        }
                        catch (Exception e) {
                            String msg2 = "internal error parsing JSON";
                            ServiceBillingBazaar.log("internal error parsing JSON");
                            UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, "internal error parsing JSON"));
                        }
                    }
                } else if (result.getResponse() == 1) {
                    String msg = "user canceled the purchase";
                    ServiceBillingBazaar.log("user canceled the purchase");
                    UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(5, "user canceled the purchase"));
                } else if (result.getResponse() == 7) {
                    if (IabActivity.this.consume) {
                        ServiceBillingBazaar.GetInstance().mHelper.consumeAsync(info, IabActivity.this.mConsumeFinishedListener);
                    } else {
                        try {
                            String msg = info.getString();
                            ServiceBillingBazaar.log(msg);
                            UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, msg));
                        }
                        catch (Exception e) {
                            String msg2 = "internal error parsing JSON";
                            ServiceBillingBazaar.log("internal error parsing JSON");
                            UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, "internal error parsing JSON"));
                        }
                    }
                } else {
                    String msg = "failed to purchase";
                    ServiceBillingBazaar.log("failed to purchase");
                    UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(5, "failed to purchase"));
                }
            }
        };
        IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener(){

            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {
                ServiceBillingBazaar.log("OnConsumeFinishedListener, result: " + result.getMessage());
                if (result.isSuccess()) {
                    try {
                        String msg = purchase.getString();
                        ServiceBillingBazaar.log(msg);
                        UnitySendMessage((String) ServiceBillingBazaar.CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, msg));
                    }
                    catch (Exception e) {
                        String msg2 = "internal error parsing JSON";
                        ServiceBillingBazaar.log("internal error parsing JSON");
                        UnitySendMessage((String) ServiceBillingBazaar.CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(6, "internal error parsing JSON"));
                    }
                } else {
                    String msg = "failed to consume product. but the purchase was successful. result: " + result.getMessage();
                    ServiceBillingBazaar.log(msg);
                    UnitySendMessage((String) ServiceBillingBazaar.CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(6, msg));
                }
                IabActivity.this.finish();
            }
        };
        IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener(){

            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.getResponse() == 6) {
                    String msg = "user should login to market.";
                    ServiceBillingBazaar.log("user should login to market.");
                    UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(7, "user should login to market."));
                    IabActivity.this.finish();
                    return;
                }
                if (result.isFailure()) {
                    String msg = "Error checking inventory. " + result.getMessage();
                    ServiceBillingBazaar.log(msg);
                    UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                    IabActivity.this.finish();
                    return;
                }
                if (inventory.hasPurchase(IabActivity.this.mSku)) {
                    try {
                        String msg = inventory.getPurchase(IabActivity.this.mSku).getString();
                        ServiceBillingBazaar.log(msg);
                        UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, msg));
                    }
                    catch (Exception e) {
                        String msg2 = "internal error parsing JSON";
                        ServiceBillingBazaar.log("internal error parsing JSON");
                        UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, "internal error parsing JSON"));
                    }
                } else {
                    String msg = "user has not product: " + IabActivity.this.mSku;
                    ServiceBillingBazaar.log(msg);
                    UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(8, msg));
                }
                IabActivity.this.finish();
            }
        };
        IabHelper.QueryInventoryFinishedListener mGotProductsDetailsListener = new IabHelper.QueryInventoryFinishedListener(){

            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.isFailure()) {
                    String msg = "Error getting products details. " + result.getMessage();
                    ServiceBillingBazaar.log(msg);
                    UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                    IabActivity.this.finish();
                    return;
                }
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < IabActivity.this.mProducts.size(); ++i) {
                    if (!inventory.hasDetails((String)IabActivity.this.mProducts.get(i))) {
                        String msg2 = "Wrong Sku '" + (String)IabActivity.this.mProducts.get(i) + "' or Internal Error";
                        ServiceBillingBazaar.log(msg2);
                        UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(13, msg2));
                        IabActivity.this.finish();
                        return;
                    }
                    SkuDetails detail = inventory.getSkuDetails((String)IabActivity.this.mProducts.get(i));
                    try {
                        JSONObject json = detail.toJson();
                        jsonArray.put((Object)json);
                        continue;
                    }
                    catch (JSONException e) {
                        String msg3 = "internal error parsing JSON";
                        ServiceBillingBazaar.log("internal error parsing JSON");
                        UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, "internal error parsing JSON"));
                        IabActivity.this.finish();
                        return;
                    }
                }
                String msg4 = jsonArray.toString();
                ServiceBillingBazaar.log(msg4);
                UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(0, msg4));
                IabActivity.this.finish();
            }
        };

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent intent = this.getIntent();
            int operationType = intent.getIntExtra(ServiceBillingBazaar._OPERATION_TYPE, 0);
            switch (operationType) {
                case 1: {
                    this.mSku = intent.getStringExtra(ServiceBillingBazaar._SKU);
                    this.mPayload = intent.getStringExtra(ServiceBillingBazaar._PAYLOAD);
                    this.consume = intent.getBooleanExtra(ServiceBillingBazaar._CONSUMABLE, false);
                    this.Purchase();
                    break;
                }
                case 2: {
                    this.mSku = intent.getStringExtra(ServiceBillingBazaar._SKU);
                    this.CheckInventory();
                    break;
                }
                case 3: {
                    this.mItemType = intent.getStringExtra(ServiceBillingBazaar._ITEM_TYPE);
                    this.mOriginalJson = intent.getStringExtra(ServiceBillingBazaar._ORIGINAL_JSON);
                    this.mSignature = intent.getStringExtra(ServiceBillingBazaar._SIGNATURE);
                    this.ConsumePurchase();
                    break;
                }
                case 4: {
                    this.mProducts = Arrays.asList(intent.getStringArrayExtra(ServiceBillingBazaar._SKU));
                    this.GetProductsDetails();
                    break;
                }
                default: {
                    this.finish();
                }
            }
        }

        private void Purchase() {
            try {
                ServiceBillingBazaar.GetInstance().mHelper.launchPurchaseFlow(this, this.mSku, 10001, this.iabPurchaseFinishedListener, this.mPayload);
            }
            catch (IllegalStateException e) {
                String msg = "Error purchasing item. " + e.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
            catch (Exception e2) {
                String msg = "Error purchasing item. " + e2.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.PURCHASE_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
        }

        private void CheckInventory() {
            try {
                ServiceBillingBazaar.GetInstance().mHelper.queryInventoryAsync(false, this.mGotInventoryListener);
            }
            catch (IllegalStateException e) {
                String msg = "Error checking inventory. " + e.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
            catch (Exception e2) {
                String msg = "Error checking inventory. " + e2.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.INVENTORY_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
        }

        private void GetProductsDetails() {
            try {
                ServiceBillingBazaar.GetInstance().mHelper.queryInventoryAsync(true, this.mProducts, this.mGotProductsDetailsListener);
            }
            catch (IllegalStateException e) {
                String msg = "Error getting products details. " + e.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
            catch (Exception e2) {
                String msg = "Error getting products details. " + e2.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.DETAILS_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(4, msg));
                this.finish();
            }
        }

        private void ConsumePurchase() {
            try {
                Purchase info = new Purchase(this.mItemType, this.mOriginalJson, this.mSignature);
                ServiceBillingBazaar.GetInstance().mHelper.consumeAsync(info, this.mConsumeFinishedListener);
            }
            catch (IllegalStateException e) {
                String msg = "Error consuming purchase. " + e.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(6, msg));
                this.finish();
            }
            catch (Exception e2) {
                String msg = "Error consuming purchase. " + e2.getMessage();
                ServiceBillingBazaar.log(msg);
                UnitySendMessage((String) ServiceBillingBazaar.CONSUME_RESULT_FUNCTION, (String) ServiceBillingBazaar.returnResult(6, msg));
                this.finish();
            }
        }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (ServiceBillingBazaar.GetInstance().mHelper != null && !ServiceBillingBazaar.GetInstance().mHelper.handleActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);
            }
            this.finish();
        }
    }
}