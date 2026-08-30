package moe.shizuku.manager.shell;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;

import rikka.rish.Rish;
import rikka.rish.RishConfig;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuApiConstants;

public class Shell extends Rish {

    private final String packageName;

    public Shell(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void requestPermission(Runnable onGrantedRunnable) {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onGrantedRunnable.run();
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            printPermissionDeniedHelp();
            System.exit(1);
        } else {
            Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    Shizuku.removeRequestPermissionResultListener(this);

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        onGrantedRunnable.run();
                    } else {
                        printPermissionDeniedHelp();
                        System.exit(1);
                    }
                }
            });
            Shizuku.requestPermission(0);
        }
    }

    private void printPermissionDeniedHelp() {
        System.err.println("[!] Shizuku permission denied for " + (packageName != null ? packageName : "this app") + ".");
        System.err.println("[!] Please open the Shizuku app -> 'Authorized Apps' and enable permission for this terminal.");
        System.err.flush();
    }

    public static void main(String[] args, String packageName, IBinder binder, Handler handler) {
        RishConfig.init(binder, ShizukuApiConstants.BINDER_DESCRIPTOR, 30000);
        Shizuku.onBinderReceived(binder, packageName);
        Shizuku.addBinderReceivedListenerSticky(() -> {
            int version = Shizuku.getVersion();
            if (version < 12) {
                System.err.println("Rish requires server 12 (running " + version + ")");
                System.err.flush();
                System.exit(1);
            }
            new Shell(packageName).start(args);
        });
    }
}
