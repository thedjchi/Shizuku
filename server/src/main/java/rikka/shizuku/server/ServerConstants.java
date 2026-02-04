package rikka.shizuku.server;

import moe.shizuku.server.BuildConfig;

public class ServerConstants {

    public static final int MANAGER_APP_NOT_FOUND = 50;

    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";
    public static final String REQUEST_PERMISSION_ACTION = BuildConfig.MANAGER_APPLICATION_ID + ".intent.action.REQUEST_PERMISSION";

    public static final int BINDER_TRANSACTION_getApplications = 10001;
}
