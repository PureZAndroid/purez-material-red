package substratum.zawzaw.purezmaterialred;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;
import com.github.javiersantos.piracychecker.PiracyChecker;
import com.github.javiersantos.piracychecker.PiracyCheckerUtils;
import com.github.javiersantos.piracychecker.enums.InstallerID;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerCallback;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError;
import java.io.File;
import java.util.ArrayList;
import projekt.substrate.LetsGetStarted;

public class SubstratumLauncher extends Activity{

    private static final String BASE_64_LICENSE_KEY = "";

    private static final String APK_SIGNATURE_PRODUCTION = "";

    private static final Boolean THEME_READY = false;

    private static final String SUBSTRATUM_PACKAGE_NAME = "projekt.substratum";

    private void startAntiPiracyCheck() {

        Log.e("SubstratumAntiPiracyLog", PiracyCheckerUtils.getAPKSignature(this));

        PiracyChecker piracyChecker = new PiracyChecker(this)

                .enableInstallerId(InstallerID.GOOGLE_PLAY)

                .callback(new PiracyCheckerCallback() {
                    @Override
                    public void allow() {
                        beginSubstratumLaunch();
                    }

                    @Override
                    public void dontAllow(PiracyCheckerError error) {
                        String parse = String.format(getString(R.string.toast_unlicensed),
                                getString(R.string.ThemeName));
                        Toast toast = Toast.makeText(SubstratumLauncher.this, parse,
                                Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    }
                });

        if (BASE_64_LICENSE_KEY.length() > 0) {
            piracyChecker.enableGooglePlayLicensing(BASE_64_LICENSE_KEY);
        }
        if (APK_SIGNATURE_PRODUCTION.length() > 0) {
            piracyChecker.enableSigningCertificate(APK_SIGNATURE_PRODUCTION);
        }
        piracyChecker.start();
    }

    private boolean isPackageInstalled(String package_name) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPackageEnabled(String package_name) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(package_name, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void beginSubstratumLaunch() {
        if (isPackageInstalled(SUBSTRATUM_PACKAGE_NAME)) {
            if (!isPackageEnabled(SUBSTRATUM_PACKAGE_NAME)) {
                Toast toast = Toast.makeText(this, getString(R.string.toast_substratum_frozen),
                        Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            launchSubstratum();
        } else {
            getSubstratumFromPlayStore();
        }
    }

    private void getSubstratumFromPlayStore() {
        String playURL =
                "https://play.google.com/store/apps/details?" +
                        "id=projekt.substratum&hl=en";
        Intent i = new Intent(Intent.ACTION_VIEW);
        Toast toast = Toast.makeText(this, getString(R.string.toast_substratum),
                Toast.LENGTH_SHORT);
        toast.show();
        i.setData(Uri.parse(playURL));
        startActivity(i);
        finish();
    }

    private void launchSubstratum() {
        Intent intent = LetsGetStarted.begin(getApplicationContext(),
                getIntent(), getString(R.string.ThemeName), getPackageName(),
                getString(R.string.unauthorized), BuildConfig.SUBSTRATE_MODULE);
        if (intent != null) {
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = SubstratumLauncher.this.getPreferences(Context.MODE_PRIVATE);
        int lastVersion = sharedPref.getInt("last_version", 0);

        if (lastVersion == BuildConfig.VERSION_CODE) {
            if (THEME_READY) {
                detectThemeReady();
            } else {
                launch();
            }
        } else {
            checkConnection();
        }
    }

    private void launch() {
        if (BuildConfig.ENABLE_ANTI_PIRACY && !BuildConfig.DEBUG) {
            startAntiPiracyCheck();
        } else {
            beginSubstratumLaunch();
        }
    }

    private void detectThemeReady() {
        File addon = new File("/system/addon.d/80-ThemeReady.sh");

        if (addon.exists()) {
            ArrayList<String> appname_arr = new ArrayList<>();
            boolean updated = false;
            String data_path = "/data/app/";
            String[] app_folder = {"com.google.android.gm",
                    "com.google.android.googlequicksearchbox",
                    "com.android.vending",
                    "com.google.android.apps.plus",
                    "com.google.android.talk",
                    "com.google.android.youtube",
                    "com.google.android.apps.photos",
                    "com.google.android.contacts",
                    "com.google.android.dialer"};
            String folder1 = "-1";
            String folder2 = "-2";
            String apk_path = "/base.apk";
            StringBuilder app_name = new StringBuilder();

            for (int i = 0; i < app_folder.length; i++) {
                File app1 = new File(data_path + app_folder[i] + folder1 + apk_path);
                File app2 = new File(data_path + app_folder[i] + folder2 + apk_path);
                if (app1.exists() || app2.exists()) {
                    try {
                        updated = true;
                        ApplicationInfo app =
                                this.getPackageManager().getApplicationInfo(app_folder[i], 0);
                        String label = getPackageManager().getApplicationLabel(app).toString();

                        appname_arr.add(label);
                    } catch (PackageManager.NameNotFoundException e) {

                    }
                }
            }

            for (int i = 0; i < appname_arr.size(); i++) {
                app_name.append(appname_arr.get(i));
                if (i <= appname_arr.size() - 3) {
                    app_name.append(", ");
                } else if (i == appname_arr.size() - 2) {
                    app_name.append(" and ");
                }
            }

            if (!updated) {
                launch();
            } else {
                String parse = String.format(getString(R.string.theme_ready_updated),
                        app_name);

                new AlertDialog.Builder(SubstratumLauncher.this, android.R.style
                        .Theme_DeviceDefault_Light_Dialog_Alert)
                        .setIcon(R.mipmap.purez_icon_launcher)
                        .setTitle(getString(R.string.ThemeName))
                        .setMessage(parse)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                launch();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        })
                        .show();
            }
        } else {
            new AlertDialog.Builder(SubstratumLauncher.this, android.R.style
                    .Theme_DeviceDefault_Light_Dialog_Alert)
                    .setIcon(R.mipmap.purez_icon_launcher)
                    .setTitle(getString(R.string.ThemeName))
                    .setMessage(getString(R.string.theme_ready_not_detected))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            launch();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager)
                        SubstratumLauncher.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast toast = Toast.makeText(this, R.string.toast_internet,
                    Toast.LENGTH_LONG);
            toast.show();
            finish();
        } else {
            SharedPreferences sharedPref =
                    SubstratumLauncher.this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("last_version", BuildConfig.VERSION_CODE);
            editor.apply();

            if (THEME_READY) {
                detectThemeReady();
            } else {
                launch();
            }
        }
    }

}








